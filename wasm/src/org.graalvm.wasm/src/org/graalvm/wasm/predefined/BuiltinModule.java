/*
 * Copyright (c) 2019, 2025, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.wasm.predefined;

import java.util.Map;

import org.graalvm.wasm.WasmContext;
import org.graalvm.wasm.WasmFunction;
import org.graalvm.wasm.WasmInstance;
import org.graalvm.wasm.WasmLanguage;
import org.graalvm.wasm.WasmModule;
import org.graalvm.wasm.WasmStore;
import org.graalvm.wasm.WasmType;
import org.graalvm.wasm.exception.Failure;
import org.graalvm.wasm.exception.WasmException;
import org.graalvm.wasm.nodes.WasmRootNode;
import org.graalvm.wasm.predefined.emscripten.EmscriptenModule;
import org.graalvm.wasm.predefined.go.GoModule;
import org.graalvm.wasm.predefined.spectest.SpectestModule;
import org.graalvm.wasm.predefined.testutil.TestutilModule;
import org.graalvm.wasm.predefined.wasi.WasiModule;

public abstract class BuiltinModule {
    private static final Map<String, BuiltinModule> predefinedModules = Map.of(
                    "emscripten", new EmscriptenModule(),
                    "testutil", new TestutilModule(),
                    "wasi_snapshot_preview1", new WasiModule(),
                    "spectest", new SpectestModule(),
                    "go", new GoModule());

    public static BuiltinModule requireBuiltinModule(String predefinedModuleName) {
        final BuiltinModule builtinModule = predefinedModules.get(predefinedModuleName);
        if (builtinModule == null) {
            throw WasmException.create(Failure.UNSPECIFIED_INVALID, "Unknown predefined module: " + predefinedModuleName);
        }
        return builtinModule;
    }

    protected BuiltinModule() {
    }

    protected abstract WasmModule createModule(WasmLanguage language, WasmContext context, String name);

    public WasmInstance createInstance(WasmLanguage language, WasmStore store, String name) {
        final WasmModule module = language.getOrCreateBuiltinModule(this, bm -> {
            WasmModule newModule = createModule(language, store.context(), name);
            newModule.finishSymbolTable();
            return newModule;
        });

        final WasmInstance instance = new WasmInstance(store, module);
        instance.createLinkActions();
        for (int i = 0; i < module.numFunctions(); i++) {
            var target = module.function(i).target();
            if (target != null && instance.target(i) == null) {
                instance.setTarget(i, target);
            }
        }
        return instance;
    }

    protected WasmFunction defineFunction(WasmContext context, WasmModule module, String name, byte[] paramTypes, byte[] retTypes, WasmRootNode rootNode) {
        // Must instantiate RootNode in the right language / sharing layer.
        assert context.language() == rootNode.getLanguage(WasmLanguage.class);
        // We could check if the same function type had already been allocated,
        // but this is just an optimization, and probably not very important,
        // since predefined modules have a relatively small size.
        final int typeIdx = module.symbolTable().allocateFunctionType(paramTypes, retTypes, context.getContextOptions().supportMultiValue());
        final WasmFunction function = module.symbolTable().declareExportedFunction(typeIdx, name);
        function.setTarget(rootNode.getCallTarget());
        return function;
    }

    protected int defineGlobal(WasmModule module, String name, byte valueType, byte mutability, Object value) {
        int index = module.symbolTable().numGlobals();
        module.symbolTable().declareExportedGlobalWithValue(name, index, valueType, mutability, value);
        return index;
    }

    protected int defineTable(WasmContext context, WasmModule module, String tableName, int initSize, int maxSize, byte type) {
        final boolean referenceTypes = context.getContextOptions().supportBulkMemoryAndRefTypes();
        switch (type) {
            case WasmType.FUNCREF_TYPE:
                break;
            case WasmType.EXTERNREF_TYPE:
                if (!referenceTypes) {
                    throw WasmException.create(Failure.UNSPECIFIED_MALFORMED, "Only function types are currently supported in tables.");
                }
                break;
            default:
                throw WasmException.create(Failure.MALFORMED_REFERENCE_TYPE, "Only reference types supported in tables.");
        }
        int index = module.symbolTable().tableCount();
        module.symbolTable().allocateTable(index, initSize, maxSize, type, referenceTypes);
        module.symbolTable().exportTable(index, tableName);
        return index;
    }

    protected void defineMemory(WasmContext context, WasmModule module, String memoryName, int initSize, int maxSize, boolean is64Bit, boolean isShared) {
        final boolean useUnsafeMemory = context.getContextOptions().useUnsafeMemory();
        final boolean directByteBufferMemoryAccess = context.getContextOptions().directByteBufferMemoryAccess();
        int index = module.symbolTable().memoryCount();
        // set multiMemory flag to true, since spectest module has multiple memories
        module.symbolTable().allocateMemory(index, initSize, maxSize, is64Bit, isShared, true, useUnsafeMemory, directByteBufferMemoryAccess);
        module.symbolTable().exportMemory(index, memoryName);
    }

    protected void importFunction(WasmContext context, WasmModule module, String importModuleName, String importFunctionName, byte[] paramTypes, byte[] retTypes, String exportName) {
        final int typeIdx = module.symbolTable().allocateFunctionType(paramTypes, retTypes, context.getContextOptions().supportMultiValue());
        final WasmFunction function = module.symbolTable().importFunction(importModuleName, importFunctionName, typeIdx);
        module.symbolTable().exportFunction(function.index(), exportName);
    }

    protected void importMemory(WasmContext context, WasmModule module, String importModuleName, String memoryName, int initSize, long maxSize, boolean is64Bit, boolean isShared) {
        final boolean multiMemory = context.getContextOptions().supportMultiMemory();
        int index = module.symbolTable().memoryCount();
        module.symbolTable().importMemory(importModuleName, memoryName, index, initSize, maxSize, is64Bit, isShared, multiMemory);
    }

    protected static byte[] types(byte... args) {
        return args;
    }
}
