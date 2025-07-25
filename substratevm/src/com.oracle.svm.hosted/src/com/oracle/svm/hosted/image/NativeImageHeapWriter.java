/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.svm.hosted.image;

import static com.oracle.svm.core.util.VMError.shouldNotReachHere;
import static com.oracle.svm.core.util.VMError.shouldNotReachHereUnexpectedInput;

import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.stream.Stream;

import org.graalvm.nativeimage.ImageSingletons;
import org.graalvm.nativeimage.impl.CEntryPointLiteralCodePointer;
import org.graalvm.word.WordBase;

import com.oracle.graal.pointsto.heap.HostedValuesProvider;
import com.oracle.graal.pointsto.heap.ImageHeapConstant;
import com.oracle.graal.pointsto.heap.ImageHeapPrimitiveArray;
import com.oracle.graal.pointsto.heap.ImageHeapRelocatableConstant;
import com.oracle.graal.pointsto.util.AnalysisError;
import com.oracle.objectfile.ObjectFile;
import com.oracle.svm.core.StaticFieldsSupport;
import com.oracle.svm.core.SubstrateOptions;
import com.oracle.svm.core.config.ConfigurationValues;
import com.oracle.svm.core.config.ObjectLayout;
import com.oracle.svm.core.graal.code.CGlobalDataBasePointer;
import com.oracle.svm.core.heap.Heap;
import com.oracle.svm.core.heap.ObjectHeader;
import com.oracle.svm.core.hub.DynamicHub;
import com.oracle.svm.core.identityhashcode.IdentityHashCodeSupport;
import com.oracle.svm.core.image.ImageHeapLayoutInfo;
import com.oracle.svm.core.imagelayer.ImageLayerBuildingSupport;
import com.oracle.svm.core.meta.MethodOffset;
import com.oracle.svm.core.meta.MethodPointer;
import com.oracle.svm.core.meta.MethodRef;
import com.oracle.svm.core.util.HostedByteBufferPointer;
import com.oracle.svm.core.util.VMError;
import com.oracle.svm.hosted.DeadlockWatchdog;
import com.oracle.svm.hosted.code.CEntryPointLiteralFeature;
import com.oracle.svm.hosted.config.DynamicHubLayout;
import com.oracle.svm.hosted.config.HybridLayout;
import com.oracle.svm.hosted.image.NativeImageHeap.ObjectInfo;
import com.oracle.svm.hosted.imagelayer.CrossLayerConstantRegistryFeature;
import com.oracle.svm.hosted.imagelayer.LayeredImageHooks;
import com.oracle.svm.hosted.meta.HostedClass;
import com.oracle.svm.hosted.meta.HostedConstantReflectionProvider;
import com.oracle.svm.hosted.meta.HostedField;
import com.oracle.svm.hosted.meta.HostedInstanceClass;
import com.oracle.svm.hosted.meta.HostedMetaAccess;
import com.oracle.svm.hosted.meta.HostedMethod;
import com.oracle.svm.hosted.meta.MaterializedConstantFields;
import com.oracle.svm.hosted.meta.PatchedWordConstant;

import jdk.graal.compiler.api.replacements.SnippetReflectionProvider;
import jdk.graal.compiler.core.common.CompressEncoding;
import jdk.graal.compiler.core.common.NumUtil;
import jdk.graal.compiler.debug.Assertions;
import jdk.graal.compiler.debug.DebugContext;
import jdk.graal.compiler.debug.Indent;
import jdk.internal.misc.Unsafe;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaMethod;

/**
 * Writes the native image heap into one or multiple {@link RelocatableBuffer}s.
 */
public final class NativeImageHeapWriter {

    private final NativeImageHeap heap;
    private final ImageHeapLayoutInfo heapLayout;
    private final boolean imageLayer = ImageLayerBuildingSupport.buildingImageLayer();
    private final LayeredImageHooks layerHooks = imageLayer ? LayeredImageHooks.singleton() : null;
    private final CrossLayerConstantRegistryFeature layerConstantRegistry = imageLayer ? CrossLayerConstantRegistryFeature.singleton() : null;
    private final JavaKind wordKind = ConfigurationValues.getWordKind();
    private long sectionOffsetOfARelocatablePointer = -1;

    public NativeImageHeapWriter(NativeImageHeap heap, ImageHeapLayoutInfo heapLayout) {
        this.heap = heap;
        this.heapLayout = heapLayout;
    }

    /**
     * Write the model of the native image heap to the RelocatableBuffers that represent the native
     * image.
     */
    @SuppressWarnings("try")
    public long writeHeap(DebugContext debug, RelocatableBuffer buffer) {
        try (Indent perHeapIndent = debug.logAndIndent("NativeImageHeap.writeHeap:")) {
            DeadlockWatchdog watchdog = DeadlockWatchdog.singleton();
            for (ObjectInfo info : heap.getObjects()) {
                assert !heap.isBlacklisted(info.getObject()) : "Backlisted object: " + info.getObject();
                if (info.getConstant().isWrittenInPreviousLayer()) {
                    /*
                     * Base layer constants already written in the base layer heap are only added to
                     * the heap model to store the absolute offset in the base layer heap. We don't
                     * need to actually write them; their absolute offset is used by the objects
                     * that reference them.
                     */
                    continue;
                }
                writeObject(info, buffer);

                watchdog.recordActivity();
            }

            // Only static fields that are writable get written to the native image heap,
            // the read-only static fields have been inlined into the code.
            writeStaticFields(buffer, watchdog);

            heap.getLayouter().writeMetadata(buffer.getByteBuffer(), 0);
        }
        return sectionOffsetOfARelocatablePointer;
    }

    private void writeStaticFields(RelocatableBuffer buffer, DeadlockWatchdog watchdog) {
        /*
         * Write the values of static fields. The arrays for primitive and object fields are empty
         * and just placeholders. This ensures we get the latest version, since there can be
         * Features registered that change the value of static fields late in the native image
         * generation process.
         */
        ObjectInfo primitiveFields = heap.getObjectInfo(StaticFieldsSupport.getCurrentLayerStaticPrimitiveFields());
        ObjectInfo objectFields = heap.getObjectInfo(StaticFieldsSupport.getCurrentLayerStaticObjectFields());
        for (HostedField field : heap.hUniverse.getFields()) {
            if (field.getWrapped().installableInLayer() && Modifier.isStatic(field.getModifiers()) && field.hasLocation() && field.isRead()) {
                assert field.isWritten() || !field.isValueAvailable() || MaterializedConstantFields.singleton().contains(field.wrapped);
                ObjectInfo fields = (field.getStorageKind() == JavaKind.Object) ? objectFields : primitiveFields;
                writeField(buffer, fields, field, null, null);
            }

            watchdog.recordActivity();
        }
    }

    private int referenceSize() {
        return heap.objectLayout.getReferenceSize();
    }

    private void mustBeReferenceAligned(int index) {
        assert (index % heap.objectLayout.getReferenceSize() == 0) : "index " + index + " must be reference-aligned.";
    }

    private static void verifyTargetDidNotChange(Object target, Object reason, Object targetInfo) {
        if (targetInfo == null) {
            throw NativeImageHeap.reportIllegalType(target, reason, "Inconsistent image heap.");
        }
    }

    private void writeField(RelocatableBuffer buffer, ObjectInfo fields, HostedField field, JavaConstant receiver, ObjectInfo info) {
        int index = getIndexInBuffer(fields, field.getLocation());
        JavaConstant value;
        try {
            value = heap.hConstantReflection.readFieldValue(field, receiver, true);
        } catch (AnalysisError.TypeNotFoundError ex) {
            throw NativeImageHeap.reportIllegalType(ex.getType(), info);
        }

        Object reason = (info != null) ? info : field;
        writeConstant(buffer, index, value.getJavaKind(), value, info, reason);
    }

    private void fillReferenceWithGarbage(RelocatableBuffer buffer, int index) {
        long garbageValue = 0xefefefefefefefefL;
        if (referenceSize() == Long.BYTES) {
            buffer.getByteBuffer().putLong(index, garbageValue);
        } else if (referenceSize() == Integer.BYTES) {
            buffer.getByteBuffer().putInt(index, (int) garbageValue);
        } else {
            throw shouldNotReachHere("Unsupported reference size: " + referenceSize());
        }
    }

    private void write(RelocatableBuffer buffer, int index, JavaConstant con, Object reason) {
        if (con.getJavaKind() == JavaKind.Object) {
            writeReference(buffer, index, con, reason);
        } else {
            writePrimitive(buffer, index, con);
        }
    }

    private final boolean useHeapBase = NativeImageHeap.useHeapBase();
    private final CompressEncoding compressEncoding = ImageSingletons.lookup(CompressEncoding.class);

    void writeReference(RelocatableBuffer buffer, int index, JavaConstant target, Object reason) {
        assert !(heap.hMetaAccess.isInstanceOf(target, WordBase.class)) : "word values are not references";
        mustBeReferenceAligned(index);
        if (target.isNonNull()) {
            ObjectInfo targetInfo = heap.getConstantInfo(target);
            verifyTargetDidNotChange(target, reason, targetInfo);
            if (useHeapBase) {
                int shift = compressEncoding.getShift();
                writeReferenceValue(buffer, index, targetInfo.getOffset() >>> shift);
            } else {
                addDirectRelocationWithoutAddend(buffer, index, referenceSize(), target);
            }
        }
    }

    /**
     * Ensure a {@link CEntryPointLiteralCodePointer} has been processed by
     * {@link CEntryPointLiteralFeature}. The replacement done when the value is added to the shadow
     * heap can miss the transformation to {@link MethodPointer} because this transformation can
     * only happen late, during compilation.
     */
    private WordBase prepareRelocatable(ObjectInfo info, WordBase word) {
        try {
            return (WordBase) heap.aUniverse.replaceObject(word);
        } catch (AnalysisError.TypeNotFoundError ex) {
            throw NativeImageHeap.reportIllegalType(ex.getType(), info);
        }
    }

    /**
     * @see NativeImageHeap#isRelocatableValue
     * @see NativeImage#markSiteOfRelocationToCode
     */
    private void writeConstant(RelocatableBuffer buffer, int index, JavaKind kind, JavaConstant constant, ObjectInfo info, Object reason) {
        int offsetInHeap = NumUtil.safeToInt(index + heapLayout.getStartOffset());

        if (constant instanceof ImageHeapRelocatableConstant ihrc) {
            layerConstantRegistry.markFutureHeapConstantPatchSite(ihrc, offsetInHeap);
            fillReferenceWithGarbage(buffer, index);
            return;
        }

        HostedMetaAccess metaAccess = heap.hMetaAccess;
        if (constant instanceof PatchedWordConstant pwc) {
            if (pwc.getWord() instanceof MethodOffset methodOffset) {
                ResolvedJavaMethod method = methodOffset.getMethod();
                HostedMethod hMethod = (method instanceof HostedMethod hm) ? hm : metaAccess.getUniverse().lookup(method);
                if (imageLayer && NativeImage.isInjectedNotCompiled(hMethod)) {
                    // Will be patched in a future layer (even if it ends up not compiled at all)
                    addWordConstantRelocation(buffer, index, methodOffset);
                } else {
                    HostedMethod target = NativeImage.getMethodRefTargetMethod(metaAccess, hMethod);
                    JavaConstant con = JavaConstant.forIntegerKind(wordKind, target.getCodeAddressOffset());
                    write(buffer, index, con, reason);
                }
            } else {
                addWordConstantRelocation(buffer, index, prepareRelocatable(info, pwc.getWord()));
            }
            if (imageLayer) {
                layerHooks.processPatchedWordWritten(pwc.getWord(), offsetInHeap, heapLayout);
            }
            return;
        }

        final JavaConstant con;
        if (metaAccess.isInstanceOf(constant, WordBase.class)) {
            Object value = snippetReflection().asObject(Object.class, constant);
            con = JavaConstant.forIntegerKind(wordKind, ((WordBase) value).rawValue());
        } else if (constant.isNull() && kind == wordKind) {
            con = JavaConstant.forIntegerKind(wordKind, 0);
        } else {
            con = constant;
        }
        write(buffer, index, con, reason);
    }

    private void writeHubPointer(RelocatableBuffer buffer, int index, ObjectInfo obj) {
        mustBeReferenceAligned(index);

        DynamicHub hub = obj.getClazz().getHub();
        assert hub != null : "Null DynamicHub found during native image generation.";
        ObjectInfo hubInfo = heap.getObjectInfo(hub);
        assert hubInfo != null : "Unknown object " + hub + " found. Static field or an object referenced from a static field changed during native image generation?";

        ObjectHeader objectHeader = Heap.getHeap().getObjectHeader();
        int hubSize = heap.objectLayout.getHubSize();
        if (NativeImageHeap.useHeapBase()) {
            long targetOffset = hubInfo.getOffset();
            long encoding = objectHeader.encodeHubPointerForImageHeap(obj, targetOffset);
            writeValue(buffer, index, encoding, hubSize);
        } else {
            assert hubSize == referenceSize();
            // The address of the DynamicHub target will be added by the link editor.
            long encoding = objectHeader.encodeHubPointerForImageHeap(obj, 0L);
            addDirectRelocationWithAddend(buffer, index, hub, encoding);
        }
    }

    private void addDirectRelocationWithoutAddend(RelocatableBuffer buffer, int index, int size, Object target) {
        assert size == 4 || size == 8;
        assert !NativeImageHeap.spawnIsolates() || isReadOnlyRelocatable(index);
        buffer.addRelocationWithoutAddend(index, size == 8 ? ObjectFile.RelocationKind.DIRECT_8 : ObjectFile.RelocationKind.DIRECT_4, target);
        if (sectionOffsetOfARelocatablePointer == -1) {
            sectionOffsetOfARelocatablePointer = index;
        }
    }

    private void addDirectRelocationWithAddend(RelocatableBuffer buffer, int index, DynamicHub target, long objectHeaderBits) {
        assert !NativeImageHeap.spawnIsolates() || isReadOnlyRelocatable(index);
        buffer.addRelocationWithAddend(index, referenceSize() == 8 ? ObjectFile.RelocationKind.DIRECT_8 : ObjectFile.RelocationKind.DIRECT_4, objectHeaderBits, snippetReflection().forObject(target));
        if (sectionOffsetOfARelocatablePointer == -1) {
            sectionOffsetOfARelocatablePointer = index;
        }
    }

    private void addWordConstantRelocation(RelocatableBuffer buffer, int index, WordBase word) {
        mustBeReferenceAligned(index);
        assert word instanceof MethodRef || word instanceof CGlobalDataBasePointer : "unknown relocatable " + word;
        int pointerSize = ConfigurationValues.getTarget().wordSize;
        addDirectRelocationWithoutAddend(buffer, index, pointerSize, word);
    }

    private static void writePrimitive(RelocatableBuffer buffer, int index, JavaConstant con) {
        ByteBuffer bb = buffer.getByteBuffer();
        switch (con.getJavaKind()) {
            case Boolean:
                bb.put(index, (byte) con.asInt());
                break;
            case Byte:
                bb.put(index, (byte) con.asInt());
                break;
            case Char:
                bb.putChar(index, (char) con.asInt());
                break;
            case Short:
                bb.putShort(index, (short) con.asInt());
                break;
            case Int:
                bb.putInt(index, con.asInt());
                break;
            case Long:
                bb.putLong(index, con.asLong());
                break;
            case Float:
                bb.putFloat(index, con.asFloat());
                break;
            case Double:
                bb.putDouble(index, con.asDouble());
                break;
            default:
                throw shouldNotReachHere(con.getJavaKind().toString());
        }
    }

    private void writeReferenceValue(RelocatableBuffer buffer, int index, long value) {
        writeValue(buffer, index, value, referenceSize());
    }

    private static void writeValue(RelocatableBuffer buffer, int index, long value, int size) {
        if (size == Long.BYTES) {
            buffer.getByteBuffer().putLong(index, value);
        } else if (size == Integer.BYTES) {
            buffer.getByteBuffer().putInt(index, NumUtil.safeToUInt(value));
        } else {
            throw shouldNotReachHere("Unsupported value size: " + size);
        }
    }

    private void writeObject(ObjectInfo info, RelocatableBuffer buffer) {
        VMError.guarantee(!(info.getConstant() instanceof ImageHeapRelocatableConstant), "ImageHeapRelocationConstants cannot be written to the heap %s", info.getConstant());

        ObjectLayout objectLayout = heap.objectLayout;
        DynamicHubLayout dynamicHubLayout = heap.dynamicHubLayout;
        HostedConstantReflectionProvider constantReflection = heap.hConstantReflection;
        HostedValuesProvider hostedValuesProvider = heap.aUniverse.getHostedValuesProvider();

        /*
         * Write a reference from the object to its hub. This lives at layout.getHubOffset() from
         * the object base.
         */
        assert objectLayout.isAligned(getIndexInBuffer(info, 0));
        writeHubPointer(buffer, getIndexInBuffer(info, objectLayout.getHubOffset()), info);

        ByteBuffer bufferBytes = buffer.getByteBuffer();
        HostedClass clazz = info.getClazz();
        if (clazz.isInstanceClass()) {
            /*
             * We always write the instance fields and identity hashcode
             *
             * If the object is a hybrid object, then also the array length and elements will be
             * written.
             *
             * If the object is a dynamic hub, then the typeID and vTable (& length) slots will be
             * written.
             */
            ImageHeapConstant con = info.getConstant();

            HostedInstanceClass instanceClazz = (HostedInstanceClass) clazz;
            long idHashOffset;
            Stream<HostedField> instanceFields = Arrays.stream(clazz.getInstanceFields(true)).filter(HostedField::isRead);

            if (dynamicHubLayout.isDynamicHub(clazz)) {
                /*
                 * Write typeID slots for closed world. In the open world configuration information
                 * is stored in a separate array since it has a variable length.
                 */
                if (SubstrateOptions.useClosedTypeWorldHubLayout()) {
                    short[] typeIDSlots = (short[]) heap.readInlinedField(dynamicHubLayout.closedTypeWorldTypeCheckSlotsField, con);
                    int typeIDSlotsLength = typeIDSlots.length;
                    for (int i = 0; i < typeIDSlotsLength; i++) {
                        int index = getIndexInBuffer(info, dynamicHubLayout.getClosedTypeWorldTypeCheckSlotsOffset(i));
                        short value = typeIDSlots[i];
                        bufferBytes.putShort(index, value);
                    }
                }

                /* Write vtable slots and length. */
                JavaConstant vTable = heap.readInlinedFieldAsConstant(dynamicHubLayout.vTableField, con);
                int vtableLength = hostedValuesProvider.readArrayLength(vTable);
                bufferBytes.putInt(getIndexInBuffer(info, dynamicHubLayout.getVTableLengthOffset()), vtableLength);
                final JavaKind elementStorageKind = dynamicHubLayout.getVTableSlotStorageKind();
                for (int i = 0; i < vtableLength; i++) {
                    JavaConstant vtableSlot = hostedValuesProvider.readArrayElement(vTable, i);
                    int elementIndex = getIndexInBuffer(info, dynamicHubLayout.getVTableSlotOffset(i));
                    writeConstant(buffer, elementIndex, elementStorageKind, vtableSlot, info, info);
                }

                idHashOffset = dynamicHubLayout.getIdentityHashOffset(vtableLength);
                instanceFields = instanceFields.filter(field -> !dynamicHubLayout.isIgnoredField(field));

                if (layerHooks != null) {
                    MethodRef[] vtableObject = hostedValuesProvider.asObject(MethodRef[].class, vTable);
                    layerHooks.processDynamicHubWritten((DynamicHub) info.getObject(), vtableObject);
                }

            } else if (heap.getHybridLayout(clazz) != null) {
                HybridLayout hybridLayout = heap.getHybridLayout(clazz);
                /* Write array and its length. */
                HostedField hybridArrayField = hybridLayout.getArrayField();
                JavaConstant hybridArray = heap.readInlinedFieldAsConstant(hybridArrayField, con);
                int length = hostedValuesProvider.readArrayLength(hybridArray);
                bufferBytes.putInt(getIndexInBuffer(info, objectLayout.getArrayLengthOffset()), length);
                for (int i = 0; i < length; i++) {
                    final int elementIndex = getIndexInBuffer(info, hybridLayout.getArrayElementOffset(i));
                    final JavaKind elementStorageKind = hybridLayout.getArrayElementStorageKind();
                    JavaConstant element = hostedValuesProvider.readArrayElement(hybridArray, i);
                    writeConstant(buffer, elementIndex, elementStorageKind, element, info, info);
                }

                idHashOffset = hybridLayout.getIdentityHashOffset(length);
                instanceFields = instanceFields.filter(field -> !field.equals(hybridArrayField));
            } else {
                idHashOffset = instanceClazz.getIdentityHashOffset();
            }

            /*
             * Write the "regular" instance fields.
             */
            instanceFields.forEach(field -> {
                assert (field.getLocation() >= 0) &&
                                (field.getLocation() >= instanceClazz.getFirstInstanceFieldOffset()) &&
                                (field.getLocation() < instanceClazz.getAfterFieldsOffset()) : Assertions.errorMessage(field,
                                                instanceClazz.getFirstInstanceFieldOffset(), instanceClazz.getAfterFieldsOffset());
                writeField(buffer, info, field, con, info);
            });

            /* Write the identity hashcode */
            assert idHashOffset >= 0;
            HostedByteBufferPointer identityHashPtr = new HostedByteBufferPointer(bufferBytes, getIndexInBuffer(info, idHashOffset));
            IdentityHashCodeSupport.writeIdentityHashCodeToImageHeap(identityHashPtr, info.getIdentityHashCode());
        } else if (clazz.isArray()) {

            JavaKind kind = clazz.getComponentType().getStorageKind();
            ImageHeapConstant constant = info.getConstant();

            int length = constantReflection.readArrayLength(constant);
            bufferBytes.putInt(getIndexInBuffer(info, objectLayout.getArrayLengthOffset()), length);
            HostedByteBufferPointer identityHashPtr = getHashCodePtr(info, bufferBytes, objectLayout, kind, length);
            IdentityHashCodeSupport.writeIdentityHashCodeToImageHeap(identityHashPtr, info.getIdentityHashCode());

            if (clazz.getComponentType().isPrimitive()) {
                ImageHeapPrimitiveArray imageHeapArray = (ImageHeapPrimitiveArray) constant;
                writePrimitiveArray(info, buffer, objectLayout, kind, imageHeapArray.getArray(), length);
            } else {
                constantReflection.forEachArrayElement(constant, (element, index) -> {
                    long elementOffset = objectLayout.getArrayElementOffset(kind, index);
                    final int elementIndex = getIndexInBuffer(info, elementOffset);
                    writeConstant(buffer, elementIndex, kind, element, info, info);
                });
            }
        } else {
            throw shouldNotReachHereUnexpectedInput(clazz); // ExcludeFromJacocoGeneratedReport
        }
    }

    private HostedByteBufferPointer getHashCodePtr(ObjectInfo info, ByteBuffer bufferBytes, ObjectLayout objectLayout, JavaKind kind, int length) {
        return new HostedByteBufferPointer(bufferBytes, getIndexInBuffer(info, objectLayout.getArrayIdentityHashOffset(kind, length)));
    }

    private int getIndexInBuffer(ObjectInfo objInfo, long offset) {
        long index = objInfo.getOffset() + offset - heapLayout.getStartOffset();
        return NumUtil.safeToInt(index);
    }

    private boolean isReadOnlyRelocatable(int indexInBuffer) {
        long offset = indexInBuffer + heapLayout.getStartOffset();
        return heapLayout.isReadOnlyRelocatable(offset);
    }

    private void writePrimitiveArray(ObjectInfo info, RelocatableBuffer buffer, ObjectLayout objectLayout, JavaKind kind, Object array, int length) {
        int elementIndex = getIndexInBuffer(info, objectLayout.getArrayElementOffset(kind, 0));
        int elementTypeSize = Unsafe.getUnsafe().arrayIndexScale(array.getClass());
        assert elementTypeSize == kind.getByteCount();
        Unsafe.getUnsafe().copyMemory(array, Unsafe.getUnsafe().arrayBaseOffset(array.getClass()), buffer.getBackingArray(),
                        Unsafe.ARRAY_BYTE_BASE_OFFSET + elementIndex, length * elementTypeSize);
    }

    private SnippetReflectionProvider snippetReflection() {
        return heap.hUniverse.getSnippetReflection();
    }

}
