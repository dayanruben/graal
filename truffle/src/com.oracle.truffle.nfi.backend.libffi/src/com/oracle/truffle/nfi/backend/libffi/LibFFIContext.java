/*
 * Copyright (c) 2017, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.nfi.backend.libffi;

import java.io.IOException;
import java.util.HashMap;
import java.util.function.Supplier;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.TruffleLanguage.Env;
import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.nfi.backend.libffi.LibFFIType.EnvType;
import com.oracle.truffle.nfi.backend.libffi.NativeAllocation.Destructor;
import com.oracle.truffle.nfi.backend.libffi.NativeAllocation.FreeDestructor;
import com.oracle.truffle.nfi.backend.spi.NFIState;
import com.oracle.truffle.nfi.backend.spi.types.NativeSimpleType;

class LibFFIContext {

    final LibFFILanguage language;
    @CompilationFinal Env env;

    final TruffleLogger attachThreadLogger;

    private long nativeContext;
    private final ThreadLocal<NativeEnv> nativeEnv = ThreadLocal.withInitial(new NativeEnvSupplier());

    @CompilationFinal(dimensions = 1) final LibFFIType[] simpleTypeMap = new LibFFIType[NativeSimpleType.values().length];
    @CompilationFinal(dimensions = 1) final LibFFIType[] arrayTypeMap = new LibFFIType[NativeSimpleType.values().length];
    @CompilationFinal(dimensions = 1) final LibFFIType[] varargsTypeMap = new LibFFIType[NativeSimpleType.values().length];
    @CompilationFinal LibFFIType cachedEnvType;

    private final HashMap<Long, ClosureNativePointer> nativePointerMap = new HashMap<>();

    final LoadFlags platformLoadFlags = LoadFlags.initLoadFlags(this);

    // initialized by native code
    // Checkstyle: stop field name check
    @CompilationFinal int RTLD_GLOBAL;
    @CompilationFinal int RTLD_LOCAL;
    @CompilationFinal int RTLD_LAZY;
    @CompilationFinal int RTLD_NOW;
    @CompilationFinal int ISOLATED_NAMESPACE;
    // Checkstyle: resume field name check

    // Initialized lazily by native code.
    private volatile long isolatedNamespaceId;

    private static class NativeEnv {

        private final long pointer;

        NativeEnv(long pointer) {
            this.pointer = pointer;
        }
    }

    private static class NativeEnvDestructor extends Destructor {

        private final long pointer;

        NativeEnvDestructor(long pointer) {
            this.pointer = pointer;
        }

        @Override
        public void destroy() {
            LibFFIContext.disposeNativeEnvV2(pointer);
        }
    }

    private final class NativeEnvSupplier implements Supplier<NativeEnv> {

        @Override
        public NativeEnv get() {
            long envPtr;
            Destructor destructor;

            if (NativeLibVersion.get() < 2) {
                envPtr = initializeNativeEnv(nativeContext);
                destructor = new FreeDestructor(envPtr);
            } else {
                envPtr = initializeNativeEnvV2(nativeContext, language.getNFIState());
                destructor = new NativeEnvDestructor(envPtr);
            }

            NativeEnv ret = new NativeEnv(envPtr);
            NativeAllocation.getGlobalQueue().registerNativeAllocation(ret, destructor);
            return ret;
        }
    }

    LibFFIContext(LibFFILanguage language, Env env) {
        this.language = language;
        this.env = env;
        this.attachThreadLogger = env.getLogger("attachCurrentThread");
    }

    void patchEnv(Env newEnv) {
        this.env = newEnv;
    }

    // called from native
    long getNativeEnv() {
        return nativeEnv.get().pointer;
    }

    // called from native, and only from a "new" thread that can not be entered already
    boolean attachThread() {
        try {
            Object ret = env.getContext().enter(null);
            assert ret == null : "thread already entered";
            return true;
        } catch (Throwable t) {
            // can't enter the context (e.g. because of a single-threaded language being active)
            attachThreadLogger.severe(t.getMessage());
            return false;
        }
    }

    // called from native immediately before detaching that thread from the VM
    void detachThread() {
        env.getContext().leave(null, null);
    }

    void initialize() {
        loadNFILib();
        NativeAllocation.ensureGCThreadRunning();

        nativeContext = initializeNativeContext();
        initializeVarargsPromotedType(NativeSimpleType.UINT8, NativeSimpleType.UINT32);
        initializeVarargsPromotedType(NativeSimpleType.UINT16, NativeSimpleType.UINT32);
        initializeVarargsPromotedType(NativeSimpleType.SINT8, NativeSimpleType.SINT32);
        initializeVarargsPromotedType(NativeSimpleType.SINT16, NativeSimpleType.SINT32);
        initializeVarargsPromotedType(NativeSimpleType.FLOAT, NativeSimpleType.DOUBLE);

        nativeEnv.remove();
    }

    void dispose() {
        if (nativeContext != 0) {
            disposeNativeContext(nativeContext);
            nativeContext = 0;
        }
        nativeEnv.set(null);
        synchronized (nativePointerMap) {
            nativePointerMap.clear();
        }
    }

    private ClosureNativePointer getClosureNativePointer(long codePointer) {
        synchronized (nativePointerMap) {
            return nativePointerMap.get(codePointer);
        }
    }

    void removeClosureNativePointer(long codePointer) {
        synchronized (nativePointerMap) {
            nativePointerMap.remove(codePointer);
        }
    }

    // called from native
    ClosureNativePointer createClosureNativePointer(long nativeClosure, long codePointer, CallTarget callTarget, LibFFISignature signature, Object receiver) {
        ClosureNativePointer ret = ClosureNativePointer.create(this, nativeClosure, codePointer, callTarget, signature, receiver);
        synchronized (nativePointerMap) {
            nativePointerMap.put(codePointer, ret);
        }
        return ret;
    }

    // called from native
    void newClosureRef(long codePointer) {
        getClosureNativePointer(codePointer).addRef();
    }

    // called from native
    void releaseClosureRef(long codePointer) {
        getClosureNativePointer(codePointer).releaseRef();
    }

    // called from native
    Object getClosureObject(long codePointer) {
        return LibFFIClosure.newClosureWrapper(getClosureNativePointer(codePointer));
    }

    @TruffleBoundary
    LibFFILibrary loadLibrary(String name, int flags) {
        return LibFFILibrary.create(loadLibrary(nativeContext, name, flags), name);
    }

    Object lookupSymbol(LibFFILibrary library, String name) {
        return LibFFISymbol.create(library, name, lookup(nativeContext, library.handle, name));
    }

    LibFFIType lookupSimpleType(NativeSimpleType type) {
        return simpleTypeMap[type.ordinal()];
    }

    LibFFIType lookupArrayType(NativeSimpleType type) {
        return arrayTypeMap[type.ordinal()];
    }

    LibFFIType lookupVarargsType(NativeSimpleType type) {
        return varargsTypeMap[type.ordinal()];
    }

    @TruffleBoundary
    LibFFIType lookupEnvType() {
        return cachedEnvType;
    }

    protected void initializeSimpleType(NativeSimpleType simpleType, int size, int alignment, long ffiType) {
        int idx = simpleType.ordinal();
        int pointerIdx = NativeSimpleType.POINTER.ordinal();

        assert simpleTypeMap[idx] == null : "initializeSimpleType called twice for " + simpleType;
        synchronized (language) {
            if (language.simpleTypeMap[idx] == null) {
                assert language.arrayTypeMap[idx] == null;
                language.simpleTypeMap[idx] = LibFFIType.createSimpleTypeInfo(simpleType, size, alignment);
                language.arrayTypeMap[idx] = LibFFIType.createArrayTypeInfo(language.simpleTypeMap[pointerIdx], simpleType);
                if (idx == pointerIdx) {
                    language.cachedEnvType = new EnvType(language.simpleTypeMap[pointerIdx]);
                }
            }
        }
        simpleTypeMap[idx] = new LibFFIType(language.simpleTypeMap[idx], ffiType);
        if (language.arrayTypeMap[idx] != null) {
            arrayTypeMap[idx] = new LibFFIType(language.arrayTypeMap[idx], simpleTypeMap[pointerIdx].type);
        }
        if (idx == pointerIdx) {
            cachedEnvType = new LibFFIType(language.cachedEnvType, simpleTypeMap[pointerIdx].type);
        }
    }

    private void initializeVarargsPromotedType(NativeSimpleType simpleType, NativeSimpleType promotedType) {
        int idx = simpleType.ordinal();
        LibFFIType promoted = simpleTypeMap[promotedType.ordinal()];

        assert varargsTypeMap[idx] == null : "initializeVarargsType called twice for " + simpleType;
        synchronized (language) {
            if (language.varargsTypeMap[idx] == null) {
                language.varargsTypeMap[idx] = LibFFIType.createVarargsPromotedTypeInfo(simpleType, promoted.typeInfo);
            }
        }

        varargsTypeMap[idx] = new LibFFIType(language.varargsTypeMap[idx], promoted.type);
    }

    private native long initializeNativeContext();

    private static native void disposeNativeContext(long context);

    /**
     * Native lib versions >= 2. Result must be freed with disposeNativeEnv.
     */
    private static native long initializeNativeEnvV2(long context, NFIState state);

    /**
     * Native lib versions >= 2.
     */
    private static native void disposeNativeEnvV2(long env);

    /**
     * Native lib versions <= 1. Result is freed with free().
     */
    private static native long initializeNativeEnv(long context);

    @SuppressWarnings("restricted")
    private void loadNFILib() {
        String nfiLib = System.getProperty("truffle.nfi.library");
        if (nfiLib == null) {
            try {
                TruffleFile libNFIResources = env.getInternalResource(LibNFIResource.class);
                TruffleFile libNFI = libNFIResources.resolve("bin").resolve(System.mapLibraryName("trufflenfi"));
                nfiLib = libNFI.getAbsoluteFile().getPath();
            } catch (IOException ioe) {
                throw CompilerDirectives.shouldNotReachHere(ioe);
            }
        }
        System.load(nfiLib);
    }

    ClosureNativePointer allocateClosureObjectRet(LibFFISignature signature, CallTarget callTarget, Object receiver) {
        return allocateClosureObjectRet(nativeContext, signature, callTarget, receiver);
    }

    ClosureNativePointer allocateClosureStringRet(LibFFISignature signature, CallTarget callTarget, Object receiver) {
        return allocateClosureStringRet(nativeContext, signature, callTarget, receiver);
    }

    ClosureNativePointer allocateClosureBufferRet(LibFFISignature signature, CallTarget callTarget, Object receiver) {
        return allocateClosureBufferRet(nativeContext, signature, callTarget, receiver);
    }

    ClosureNativePointer allocateClosureVoidRet(LibFFISignature signature, CallTarget callTarget, Object receiver) {
        return allocateClosureVoidRet(nativeContext, signature, callTarget, receiver);
    }

    @TruffleBoundary
    private static native ClosureNativePointer allocateClosureObjectRet(long nativeContext, LibFFISignature signature, CallTarget callTarget, Object receiver);

    @TruffleBoundary
    private static native ClosureNativePointer allocateClosureStringRet(long nativeContext, LibFFISignature signature, CallTarget callTarget, Object receiver);

    @TruffleBoundary
    private static native ClosureNativePointer allocateClosureBufferRet(long nativeContext, LibFFISignature signature, CallTarget callTarget, Object receiver);

    @TruffleBoundary
    private static native ClosureNativePointer allocateClosureVoidRet(long nativeContext, LibFFISignature signature, CallTarget callTarget, Object receiver);

    long prepareSignature(LibFFIType retType, int argCount, LibFFIType... args) {
        assert args.length >= argCount;
        return prepareSignature(nativeContext, retType, argCount, args);
    }

    long prepareSignatureVarargs(LibFFIType retType, int argCount, int nFixedArgs, LibFFIType... args) {
        assert args.length >= argCount;
        return prepareSignatureVarargs(nativeContext, retType, argCount, nFixedArgs, args);
    }

    private static native long prepareSignature(long nativeContext, LibFFIType retType, int argCount, LibFFIType... args);

    private static native long prepareSignatureVarargs(long nativeContext, LibFFIType retType, int argCount, int nFixedArgs, LibFFIType... args);

    // substituted by SVM
    void executeNative(long cif, long functionPointer, byte[] primArgs, int patchCount, int[] patchOffsets, Object[] objArgs, byte[] ret) {
        executeNative(nativeContext, language.getNFIState(), cif, functionPointer, primArgs, patchCount, patchOffsets, objArgs, ret);
    }

    // substituted by SVM
    long executePrimitive(long cif, long functionPointer, byte[] primArgs, int patchCount, int[] patchOffsets, Object[] objArgs) {
        return executePrimitive(nativeContext, language.getNFIState(), cif, functionPointer, primArgs, patchCount, patchOffsets, objArgs);
    }

    // substituted by SVM
    Object executeObject(long cif, long functionPointer, byte[] primArgs, int patchCount, int[] patchOffsets, Object[] objArgs) {
        return executeObject(nativeContext, language.getNFIState(), cif, functionPointer, primArgs, patchCount, patchOffsets, objArgs);
    }

    @TruffleBoundary
    private static native void executeNative(long nativeContext, NFIState state, long cif, long functionPointer, byte[] primArgs, int patchCount, int[] patchOffsets, Object[] objArgs, byte[] ret);

    @TruffleBoundary
    private static native long executePrimitive(long nativeContext, NFIState state, long cif, long functionPointer, byte[] primArgs, int patchCount, int[] patchOffsets, Object[] objArgs);

    @TruffleBoundary
    private static native Object executeObject(long nativeContext, NFIState state, long cif, long functionPointer, byte[] primArgs, int patchCount, int[] patchOffsets, Object[] objArgs);

    private static native long loadLibrary(long nativeContext, String name, int flags);

    @TruffleBoundary
    private static native long lookup(long nativeContext, long library, String name);

    static native void freeLibrary(long library);

    private static final ContextReference<LibFFIContext> REFERENCE = ContextReference.create(LibFFILanguage.class);

    static LibFFIContext get(Node node) {
        return REFERENCE.get(node);
    }
}
