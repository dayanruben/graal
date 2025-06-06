/*
 * Copyright (c) 2012, 2025, Oracle and/or its affiliates. All rights reserved.
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
package jdk.graal.compiler.replacements.gc;

import static jdk.graal.compiler.nodes.extended.BranchProbabilityNode.FREQUENT_PROBABILITY;
import static jdk.graal.compiler.nodes.extended.BranchProbabilityNode.NOT_FREQUENT_PROBABILITY;
import static jdk.graal.compiler.nodes.extended.BranchProbabilityNode.probability;

import org.graalvm.word.LocationIdentity;
import org.graalvm.word.Pointer;
import org.graalvm.word.UnsignedWord;

import jdk.graal.compiler.api.directives.GraalDirectives;
import jdk.graal.compiler.api.replacements.Snippet;
import jdk.graal.compiler.api.replacements.Snippet.ConstantParameter;
import jdk.graal.compiler.core.common.GraalOptions;
import jdk.graal.compiler.core.common.memory.BarrierType;
import jdk.graal.compiler.core.common.spi.ForeignCallDescriptor;
import jdk.graal.compiler.graph.Node.ConstantNodeParameter;
import jdk.graal.compiler.graph.Node.NodeIntrinsic;
import jdk.graal.compiler.nodes.NamedLocationIdentity;
import jdk.graal.compiler.nodes.NodeView;
import jdk.graal.compiler.nodes.StructuredGraph;
import jdk.graal.compiler.nodes.ValueNode;
import jdk.graal.compiler.nodes.extended.FixedValueAnchorNode;
import jdk.graal.compiler.nodes.extended.ForeignCallNode;
import jdk.graal.compiler.nodes.extended.MembarNode;
import jdk.graal.compiler.nodes.gc.G1ArrayRangePostWriteBarrierNode;
import jdk.graal.compiler.nodes.gc.G1ArrayRangePreWriteBarrierNode;
import jdk.graal.compiler.nodes.gc.G1PostWriteBarrierNode;
import jdk.graal.compiler.nodes.gc.G1PreWriteBarrierNode;
import jdk.graal.compiler.nodes.gc.G1ReferentFieldReadBarrierNode;
import jdk.graal.compiler.nodes.memory.address.AddressNode;
import jdk.graal.compiler.nodes.memory.address.AddressNode.Address;
import jdk.graal.compiler.nodes.memory.address.OffsetAddressNode;
import jdk.graal.compiler.nodes.spi.LoweringTool;
import jdk.graal.compiler.nodes.type.NarrowOopStamp;
import jdk.graal.compiler.replacements.SnippetCounter;
import jdk.graal.compiler.replacements.SnippetTemplate;
import jdk.graal.compiler.replacements.Snippets;
import jdk.graal.compiler.replacements.nodes.AssertionNode;
import jdk.graal.compiler.replacements.nodes.CStringConstant;
import jdk.graal.compiler.word.Word;

/**
 * Implementation of the write barriers for the G1 garbage collector.
 */
public abstract class G1WriteBarrierSnippets extends WriteBarrierSnippets implements Snippets {

    public static final LocationIdentity SATB_QUEUE_MARKING_ACTIVE_LOCATION = NamedLocationIdentity.mutable("GC-SATB-Marking-Active");
    public static final LocationIdentity SATB_QUEUE_BUFFER_LOCATION = NamedLocationIdentity.mutable("GC-SATB-Queue-Buffer");
    public static final LocationIdentity SATB_QUEUE_LOG_LOCATION = NamedLocationIdentity.mutable("GC-SATB-Queue-Log");
    public static final LocationIdentity SATB_QUEUE_INDEX_LOCATION = NamedLocationIdentity.mutable("GC-SATB-Queue-Index");

    public static final LocationIdentity CARD_QUEUE_BUFFER_LOCATION = NamedLocationIdentity.mutable("GC-Card-Queue-Buffer");
    public static final LocationIdentity CARD_QUEUE_LOG_LOCATION = NamedLocationIdentity.mutable("GC-Card-Queue-Log");
    public static final LocationIdentity CARD_QUEUE_INDEX_LOCATION = NamedLocationIdentity.mutable("GC-Card-Queue-Index");

    protected static final LocationIdentity[] KILLED_PRE_WRITE_BARRIER_STUB_LOCATIONS = new LocationIdentity[]{SATB_QUEUE_INDEX_LOCATION, SATB_QUEUE_BUFFER_LOCATION, SATB_QUEUE_LOG_LOCATION};
    protected static final LocationIdentity[] KILLED_POST_WRITE_BARRIER_STUB_LOCATIONS = new LocationIdentity[]{CARD_QUEUE_INDEX_LOCATION, CARD_QUEUE_BUFFER_LOCATION, CARD_QUEUE_LOG_LOCATION,
                    GC_CARD_LOCATION};

    public static class Counters {
        Counters(SnippetCounter.Group.Factory factory) {
            SnippetCounter.Group countersWriteBarriers = factory.createSnippetCounterGroup("G1 WriteBarriers");
            g1AttemptedPreWriteBarrierCounter = new SnippetCounter(countersWriteBarriers, "g1AttemptedPreWriteBarrier", "Number of attempted G1 Pre Write Barriers");
            g1EffectivePreWriteBarrierCounter = new SnippetCounter(countersWriteBarriers, "g1EffectivePreWriteBarrier", "Number of effective G1 Pre Write Barriers");
            g1ExecutedPreWriteBarrierCounter = new SnippetCounter(countersWriteBarriers, "g1ExecutedPreWriteBarrier", "Number of executed G1 Pre Write Barriers");
            g1AttemptedPostWriteBarrierCounter = new SnippetCounter(countersWriteBarriers, "g1AttemptedPostWriteBarrier", "Number of attempted G1 Post Write Barriers");
            g1EffectiveAfterXORPostWriteBarrierCounter = new SnippetCounter(countersWriteBarriers, "g1EffectiveAfterXORPostWriteBarrier",
                            "Number of effective G1 Post Write Barriers (after passing the XOR test)");
            g1EffectiveAfterNullPostWriteBarrierCounter = new SnippetCounter(countersWriteBarriers, "g1EffectiveAfterNullPostWriteBarrier",
                            "Number of effective G1 Post Write Barriers (after passing the NULL test)");
            g1ExecutedPostWriteBarrierCounter = new SnippetCounter(countersWriteBarriers, "g1ExecutedPostWriteBarrier", "Number of executed G1 Post Write Barriers");
        }

        final SnippetCounter g1AttemptedPreWriteBarrierCounter;
        final SnippetCounter g1EffectivePreWriteBarrierCounter;
        final SnippetCounter g1ExecutedPreWriteBarrierCounter;
        final SnippetCounter g1AttemptedPostWriteBarrierCounter;
        final SnippetCounter g1EffectiveAfterXORPostWriteBarrierCounter;
        final SnippetCounter g1EffectiveAfterNullPostWriteBarrierCounter;
        final SnippetCounter g1ExecutedPostWriteBarrierCounter;
    }

    @Snippet
    public void g1PreWriteBarrier(Address address, Object object, Object expectedObject, @ConstantParameter boolean doLoad,
                    @ConstantParameter int traceStartCycle, @ConstantParameter Counters counters) {
        satbBarrier(address, object, expectedObject, doLoad, traceStartCycle, counters);
    }

    @Snippet
    public void g1ReferentReadBarrier(Address address, Object object, Object expectedObject, @ConstantParameter int traceStartCycle, @ConstantParameter Counters counters) {
        satbBarrier(address, object, expectedObject, false, traceStartCycle, counters);
    }

    private void satbBarrier(Address address, Object object, Object expectedObject, boolean doLoad,
                    int traceStartCycle, Counters counters) {
        Word thread = getThread();
        verifyOop(object);
        Word field = Word.fromAddress(address);
        byte markingValue = thread.readByte(satbQueueMarkingActiveOffset(), SATB_QUEUE_MARKING_ACTIVE_LOCATION);

        boolean trace = isTracingActive(traceStartCycle);
        int gcCycle = 0;
        if (trace) {
            Pointer gcTotalCollectionsAddress = Word.pointer(gcTotalCollectionsAddress());
            gcCycle = gcTotalCollectionsAddress.readInt(0, LocationIdentity.any());
            log(trace, "[%d] G1-Pre Thread %p Object %p\n", gcCycle, thread.rawValue(), Word.objectToTrackedPointer(object).rawValue());
            log(trace, "[%d] G1-Pre Thread %p Expected Object %p\n", gcCycle, thread.rawValue(), Word.objectToTrackedPointer(expectedObject).rawValue());
            log(trace, "[%d] G1-Pre Thread %p Field %p\n", gcCycle, thread.rawValue(), field.rawValue());
            log(trace, "[%d] G1-Pre Thread %p Marking %d\n", gcCycle, thread.rawValue(), markingValue);
            log(trace, "[%d] G1-Pre Thread %p DoLoad %d\n", gcCycle, thread.rawValue(), doLoad ? 1L : 0L);
        }

        counters.g1AttemptedPreWriteBarrierCounter.inc();
        // If the concurrent marker is enabled, the barrier is issued.
        if (probability(NOT_FREQUENT_PROBABILITY, markingValue != (byte) 0)) {
            // If the previous value has to be loaded (before the write), the load is issued.
            // The load is always issued except the cases of CAS and referent field.
            Object previousObject;
            if (doLoad) {
                previousObject = field.readObject(0, BarrierType.NONE, LocationIdentity.any());
                if (trace) {
                    log(trace, "[%d] G1-Pre Thread %p Previous Object %p\n ", gcCycle, thread.rawValue(), Word.objectToTrackedPointer(previousObject).rawValue());
                    verifyOop(previousObject);
                }
            } else {
                previousObject = FixedValueAnchorNode.getObject(expectedObject);
            }

            counters.g1EffectivePreWriteBarrierCounter.inc();
            // If the previous value is null the barrier should not be issued.
            if (probability(FREQUENT_PROBABILITY, previousObject != null)) {
                counters.g1ExecutedPreWriteBarrierCounter.inc();
                // If the thread-local SATB buffer is full issue a native call which will
                // initialize a new one and add the entry.
                Word indexValue = thread.readWord(satbQueueIndexOffset(), SATB_QUEUE_INDEX_LOCATION);
                if (probability(FREQUENT_PROBABILITY, indexValue.notEqual(0))) {
                    Word bufferAddress = thread.readWord(satbQueueBufferOffset(), SATB_QUEUE_BUFFER_LOCATION);
                    Word nextIndex = indexValue.subtract(wordSize());

                    // Log the object to be marked as well as update the SATB's buffer next index.
                    bufferAddress.writeWord(nextIndex, Word.objectToTrackedPointer(previousObject), SATB_QUEUE_LOG_LOCATION);
                    thread.writeWord(satbQueueIndexOffset(), nextIndex, SATB_QUEUE_INDEX_LOCATION);
                } else {
                    g1PreBarrierStub(previousObject);
                }
            }
        }
    }

    @Snippet
    public void g1PostWriteBarrier(Address address, Object object, Object value, @ConstantParameter boolean usePrecise, @ConstantParameter int traceStartCycle,
                    @ConstantParameter Counters counters) {
        Word thread = getThread();
        Object fixedValue = FixedValueAnchorNode.getObject(value);
        verifyOop(object);
        verifyOop(fixedValue);
        validateObject(object, fixedValue);

        Pointer oop;
        if (usePrecise) {
            oop = Word.fromAddress(address);
        } else {
            if (verifyBarrier()) {
                verifyNotArray(object);
            }
            oop = Word.objectToTrackedPointer(object);
        }

        boolean trace = isTracingActive(traceStartCycle);
        int gcCycle = 0;
        if (trace) {
            Pointer gcTotalCollectionsAddress = Word.pointer(gcTotalCollectionsAddress());
            gcCycle = gcTotalCollectionsAddress.readInt(0, LocationIdentity.any());
            log(trace, "[%d] G1-Post Thread: %p Object: %p\n", gcCycle, thread.rawValue(), Word.objectToTrackedPointer(object).rawValue());
            log(trace, "[%d] G1-Post Thread: %p Field: %p\n", gcCycle, thread.rawValue(), oop.rawValue());
        }
        Pointer writtenValue = Word.objectToTrackedPointer(fixedValue);
        // The result of the xor reveals whether the installed pointer crosses heap regions.
        // In case it does the write barrier has to be issued.
        final int logOfHeapRegionGrainBytes = logOfHeapRegionGrainBytes();
        UnsignedWord xorResult = (oop.xor(writtenValue)).unsignedShiftRight(logOfHeapRegionGrainBytes);

        counters.g1AttemptedPostWriteBarrierCounter.inc();
        if (probability(FREQUENT_PROBABILITY, xorResult.notEqual(0))) {
            counters.g1EffectiveAfterXORPostWriteBarrierCounter.inc();
            // If the written value is not null continue with the barrier addition.
            if (probability(FREQUENT_PROBABILITY, writtenValue.notEqual(0))) {
                // Calculate the address of the card to be enqueued to the
                // thread local card queue.
                Word cardAddress = cardTableBase().add(cardTableOffset(oop));

                byte cardByte = cardAddress.readByte(0, GC_CARD_LOCATION);
                counters.g1EffectiveAfterNullPostWriteBarrierCounter.inc();

                if (supportsLowLatencyBarriers()) {
                    if (probability(NOT_FREQUENT_PROBABILITY, cardByte == cleanCardValue())) {
                        cardAddress.writeByte(0, dirtyCardValue(), GC_CARD_LOCATION);
                    }
                    return;
                }

                // If the card is already dirty, (hence already enqueued) skip the insertion.
                if (probability(NOT_FREQUENT_PROBABILITY, cardByte != youngCardValue())) {
                    MembarNode.memoryBarrier(MembarNode.FenceKind.STORE_LOAD, GC_CARD_LOCATION);
                    byte cardByteReload = cardAddress.readByte(0, GC_CARD_LOCATION);
                    if (probability(NOT_FREQUENT_PROBABILITY, cardByteReload != dirtyCardValue())) {
                        log(trace, "[%d] G1-Post Thread: %p Card: %p \n", gcCycle, thread.rawValue(), Word.unsigned((int) cardByte).rawValue());
                        cardAddress.writeByte(0, dirtyCardValue(), GC_CARD_LOCATION);
                        counters.g1ExecutedPostWriteBarrierCounter.inc();

                        // If the thread local card queue is full, issue a native call which will
                        // initialize a new one and add the card entry.
                        Word indexValue = thread.readWord(cardQueueIndexOffset(), CARD_QUEUE_INDEX_LOCATION);
                        if (probability(FREQUENT_PROBABILITY, indexValue.notEqual(0))) {
                            Word bufferAddress = thread.readWord(cardQueueBufferOffset(), CARD_QUEUE_BUFFER_LOCATION);
                            Word nextIndex = indexValue.subtract(wordSize());

                            // Log the object to be scanned as well as update the card queue's next
                            // index.
                            bufferAddress.writeWord(nextIndex, cardAddress, CARD_QUEUE_LOG_LOCATION);
                            thread.writeWord(cardQueueIndexOffset(), nextIndex, CARD_QUEUE_INDEX_LOCATION);
                        } else {
                            g1PostBarrierStub(cardAddress);
                        }
                    }
                }
            }
        }
    }

    @Snippet
    public void g1ArrayRangePreWriteBarrier(Address address, long length, @ConstantParameter int elementStride) {
        Word thread = getThread();
        byte markingValue = thread.readByte(satbQueueMarkingActiveOffset(), SATB_QUEUE_MARKING_ACTIVE_LOCATION);
        // If the concurrent marker is not enabled or the vector length is zero, return.
        if (probability(FREQUENT_PROBABILITY, markingValue == (byte) 0) || probability(NOT_FREQUENT_PROBABILITY, length == 0)) {
            return;
        }

        Word bufferAddress = thread.readWord(satbQueueBufferOffset(), SATB_QUEUE_BUFFER_LOCATION);
        Word indexAddress = thread.add(satbQueueIndexOffset());
        long indexValue = indexAddress.readWord(0, SATB_QUEUE_INDEX_LOCATION).rawValue();
        long scale = objectArrayIndexScale();
        Word start = getPointerToFirstArrayElement(Word.fromAddress(address), length, elementStride);

        for (int i = 0; GraalDirectives.injectIterationCount(10, i < length); i++) {
            Word arrElemPtr = start.add(Word.unsigned(i * scale));
            Object previousObject = arrElemPtr.readObject(0, BarrierType.NONE, LocationIdentity.any());
            verifyOop(previousObject);
            if (probability(FREQUENT_PROBABILITY, previousObject != null)) {
                if (probability(FREQUENT_PROBABILITY, indexValue != 0)) {
                    indexValue = indexValue - wordSize();
                    Word logAddress = bufferAddress.add(Word.unsigned(indexValue));
                    // Log the object to be marked and update the SATB's buffer next index.
                    logAddress.writeWord(0, Word.objectToTrackedPointer(previousObject), SATB_QUEUE_LOG_LOCATION);
                    indexAddress.writeWord(0, Word.unsigned(indexValue), SATB_QUEUE_INDEX_LOCATION);
                } else {
                    g1PreBarrierStub(previousObject);
                }
            }
        }
    }

    @Snippet
    public void g1ArrayRangePostWriteBarrier(Address address, long length, @ConstantParameter int elementStride) {
        if (probability(NOT_FREQUENT_PROBABILITY, length == 0)) {
            return;
        }

        Word base = cardTableBase();
        Word addr = Word.fromAddress(address);
        Word start = base.add(cardTableOffset(getPointerToFirstArrayElement(addr, length, elementStride)));
        Word end = base.add(cardTableOffset(getPointerToLastArrayElement(addr, length, elementStride)));

        Word cur = start;
        if (supportsLowLatencyBarriers()) {
            do {
                byte cardByte = cur.readByte(0, GC_CARD_LOCATION);
                if (probability(NOT_FREQUENT_PROBABILITY, cardByte == cleanCardValue())) {
                    cur.writeByte(0, dirtyCardValue(), GC_CARD_LOCATION);
                }
                cur = cur.add(1);
            } while (GraalDirectives.injectIterationCount(10, cur.belowOrEqual(end)));
            return;
        }

        Word thread = getThread();
        Word bufferAddress = thread.readWord(cardQueueBufferOffset(), CARD_QUEUE_BUFFER_LOCATION);
        Word indexAddress = thread.add(cardQueueIndexOffset());
        long indexValue = thread.readWord(cardQueueIndexOffset(), CARD_QUEUE_INDEX_LOCATION).rawValue();

        do {
            byte cardByte = cur.readByte(0, GC_CARD_LOCATION);
            // If the card is already dirty, (hence already enqueued) skip the insertion.
            if (probability(NOT_FREQUENT_PROBABILITY, cardByte != youngCardValue())) {
                MembarNode.memoryBarrier(MembarNode.FenceKind.STORE_LOAD, GC_CARD_LOCATION);
                byte cardByteReload = cur.readByte(0, GC_CARD_LOCATION);
                if (probability(NOT_FREQUENT_PROBABILITY, cardByteReload != dirtyCardValue())) {
                    cur.writeByte(0, dirtyCardValue(), GC_CARD_LOCATION);
                    // If the thread local card queue is full, issue a native call which will
                    // initialize a new one and add the card entry.
                    if (probability(FREQUENT_PROBABILITY, indexValue != 0)) {
                        indexValue = indexValue - wordSize();
                        Word logAddress = bufferAddress.add(Word.unsigned(indexValue));
                        // Log the object to be scanned as well as update
                        // the card queue's next index.
                        logAddress.writeWord(0, cur, CARD_QUEUE_LOG_LOCATION);
                        indexAddress.writeWord(0, Word.unsigned(indexValue), CARD_QUEUE_INDEX_LOCATION);
                    } else {
                        g1PostBarrierStub(cur);
                    }
                }
            }
            cur = cur.add(1);
        } while (GraalDirectives.injectIterationCount(10, cur.belowOrEqual(end)));
    }

    protected abstract Word getThread();

    protected abstract int wordSize();

    /**
     * The scale as a long to force promotion to long when it's used in computations.
     */
    protected abstract long objectArrayIndexScale();

    protected abstract int satbQueueMarkingActiveOffset();

    protected abstract int satbQueueBufferOffset();

    protected abstract int satbQueueIndexOffset();

    protected abstract int cardQueueBufferOffset();

    protected abstract int cardQueueIndexOffset();

    protected abstract byte dirtyCardValue();

    protected abstract byte youngCardValue();

    public abstract byte cleanCardValue();

    protected abstract boolean supportsLowLatencyBarriers();

    protected abstract Word cardTableBase();

    protected abstract UnsignedWord cardTableOffset(Pointer oop);

    protected abstract int logOfHeapRegionGrainBytes();

    protected abstract ForeignCallDescriptor preWriteBarrierCallDescriptor();

    protected abstract ForeignCallDescriptor postWriteBarrierCallDescriptor();

    // the data below is only needed for the verification logic
    protected abstract boolean verifyOops();

    protected abstract boolean verifyBarrier();

    protected abstract long gcTotalCollectionsAddress();

    protected abstract ForeignCallDescriptor verifyOopCallDescriptor();

    protected abstract ForeignCallDescriptor validateObjectCallDescriptor();

    protected abstract ForeignCallDescriptor printfCallDescriptor();

    protected boolean isTracingActive(int traceStartCycle) {
        return traceStartCycle > 0 && ((Pointer) Word.pointer(gcTotalCollectionsAddress())).readInt(0) > traceStartCycle;
    }

    private void log(boolean enabled, String format, long value1, long value2, long value3) {
        if (enabled) {
            printf(printfCallDescriptor(), CStringConstant.cstring(format), value1, value2, value3);
        }
    }

    /**
     * Validation helper method which performs sanity checks on write operations. The addresses of
     * both the object and the value being written are checked in order to determine if they reside
     * in a valid heap region. If an object is stale, an invalid access is performed in order to
     * prematurely crash the VM and debug the stack trace of the faulty method.
     */
    private void validateObject(Object parent, Object child) {
        if (verifyOops() && child != null) {
            Word parentWord = Word.objectToTrackedPointer(parent);
            Word childWord = Word.objectToTrackedPointer(child);
            boolean success = validateOop(validateObjectCallDescriptor(), parentWord, childWord);
            AssertionNode.dynamicAssert(success, "Verification ERROR, Parent: %p Child: %p\n", parentWord.rawValue(), childWord.rawValue());
        }
    }

    private void verifyOop(Object object) {
        if (verifyOops()) {
            verifyOopStub(verifyOopCallDescriptor(), object);
        }
    }

    private void g1PreBarrierStub(Object previousObject) {
        g1PreBarrierStub(preWriteBarrierCallDescriptor(), previousObject);
    }

    private void g1PostBarrierStub(Word cardAddress) {
        g1PostBarrierStub(postWriteBarrierCallDescriptor(), cardAddress);
    }

    @NodeIntrinsic(ForeignCallNode.class)
    private static native Object verifyOopStub(@ConstantNodeParameter ForeignCallDescriptor descriptor, Object object);

    @NodeIntrinsic(ForeignCallNode.class)
    private static native boolean validateOop(@ConstantNodeParameter ForeignCallDescriptor descriptor, Word parent, Word object);

    @NodeIntrinsic(ForeignCallNode.class)
    private static native void g1PreBarrierStub(@ConstantNodeParameter ForeignCallDescriptor descriptor, Object object);

    @NodeIntrinsic(ForeignCallNode.class)
    private static native void g1PostBarrierStub(@ConstantNodeParameter ForeignCallDescriptor descriptor, Word card);

    @NodeIntrinsic(ForeignCallNode.class)
    private static native void printf(@ConstantNodeParameter ForeignCallDescriptor logPrintf, Word format, long v1, long v2, long v3);

    public abstract static class G1WriteBarrierLowerer {
        private final Counters counters;

        public G1WriteBarrierLowerer(SnippetCounter.Group.Factory factory) {
            this.counters = new Counters(factory);
        }

        public void lower(SnippetTemplate.AbstractTemplates templates, SnippetTemplate.SnippetInfo snippet, G1PreWriteBarrierNode barrier, LoweringTool tool) {
            SnippetTemplate.Arguments args = new SnippetTemplate.Arguments(snippet, barrier.graph().getGuardsStage(), tool.getLoweringStage());
            AddressNode address = barrier.getAddress();
            args.add("address", address);
            if (address instanceof OffsetAddressNode) {
                args.add("object", ((OffsetAddressNode) address).getBase());
            } else {
                args.add("object", null);
            }

            ValueNode expected = barrier.getExpectedObject();
            if (expected != null && expected.stamp(NodeView.DEFAULT) instanceof NarrowOopStamp) {
                expected = uncompress(expected);
            }
            args.add("expectedObject", expected);

            args.add("doLoad", barrier.doLoad());
            args.add("traceStartCycle", traceStartCycle(barrier.graph()));
            args.add("counters", counters);

            templates.template(tool, barrier, args).instantiate(tool.getMetaAccess(), barrier, SnippetTemplate.DEFAULT_REPLACER, args);
        }

        public void lower(SnippetTemplate.AbstractTemplates templates, SnippetTemplate.SnippetInfo snippet, G1ReferentFieldReadBarrierNode barrier, LoweringTool tool) {
            SnippetTemplate.Arguments args = new SnippetTemplate.Arguments(snippet, barrier.graph().getGuardsStage(), tool.getLoweringStage());
            // This is expected to be lowered before address lowering
            OffsetAddressNode address = (OffsetAddressNode) barrier.getAddress();
            args.add("address", address);
            args.add("object", address.getBase());

            ValueNode expected = barrier.getExpectedObject();
            if (expected != null && expected.stamp(NodeView.DEFAULT) instanceof NarrowOopStamp) {
                expected = uncompress(expected);
            }

            args.add("expectedObject", expected);
            args.add("traceStartCycle", traceStartCycle(barrier.graph()));
            args.add("counters", counters);

            templates.template(tool, barrier, args).instantiate(tool.getMetaAccess(), barrier, SnippetTemplate.DEFAULT_REPLACER, args);
        }

        public void lower(SnippetTemplate.AbstractTemplates templates, SnippetTemplate.SnippetInfo snippet, G1PostWriteBarrierNode barrier, LoweringTool tool) {
            if (barrier.alwaysNull()) {
                barrier.graph().removeFixed(barrier);
                return;
            }

            SnippetTemplate.Arguments args = new SnippetTemplate.Arguments(snippet, barrier.graph().getGuardsStage(), tool.getLoweringStage());
            AddressNode address = barrier.getAddress();
            args.add("address", address);
            if (address instanceof OffsetAddressNode) {
                args.add("object", ((OffsetAddressNode) address).getBase());
            } else {
                assert barrier.usePrecise() : "found imprecise barrier that's not an object access " + barrier + " at address " + address;
                args.add("object", null);
            }

            ValueNode value = barrier.getValue();
            if (value.stamp(NodeView.DEFAULT) instanceof NarrowOopStamp) {
                value = uncompress(value);
            }
            args.add("value", value);

            args.add("usePrecise", barrier.usePrecise());
            args.add("traceStartCycle", traceStartCycle(barrier.graph()));
            args.add("counters", counters);

            templates.template(tool, barrier, args).instantiate(tool.getMetaAccess(), barrier, SnippetTemplate.DEFAULT_REPLACER, args);
        }

        public void lower(SnippetTemplate.AbstractTemplates templates, SnippetTemplate.SnippetInfo snippet, G1ArrayRangePreWriteBarrierNode barrier, LoweringTool tool) {
            SnippetTemplate.Arguments args = new SnippetTemplate.Arguments(snippet, barrier.graph().getGuardsStage(), tool.getLoweringStage());
            args.add("address", barrier.getAddress());
            args.add("length", barrier.getLengthAsLong());
            args.add("elementStride", barrier.getElementStride());

            templates.template(tool, barrier, args).instantiate(tool.getMetaAccess(), barrier, SnippetTemplate.DEFAULT_REPLACER, args);
        }

        public void lower(SnippetTemplate.AbstractTemplates templates, SnippetTemplate.SnippetInfo snippet, G1ArrayRangePostWriteBarrierNode barrier, LoweringTool tool) {
            SnippetTemplate.Arguments args = new SnippetTemplate.Arguments(snippet, barrier.graph().getGuardsStage(), tool.getLoweringStage());
            args.add("address", barrier.getAddress());
            args.add("length", barrier.getLengthAsLong());
            args.add("elementStride", barrier.getElementStride());

            templates.template(tool, barrier, args).instantiate(tool.getMetaAccess(), barrier, SnippetTemplate.DEFAULT_REPLACER, args);
        }

        private static int traceStartCycle(StructuredGraph graph) {
            return GraalOptions.GCDebugStartCycle.getValue(graph.getOptions());
        }

        protected abstract ValueNode uncompress(ValueNode value);
    }
}
