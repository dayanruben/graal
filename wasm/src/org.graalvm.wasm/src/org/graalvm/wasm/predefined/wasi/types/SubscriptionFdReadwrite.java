/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
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

/*
 * This file has been automatically generated from wasi_snapshot_preview1.witx.
 */

package org.graalvm.wasm.predefined.wasi.types;

import com.oracle.truffle.api.nodes.Node;
import org.graalvm.wasm.memory.WasmMemory;
import org.graalvm.wasm.memory.WasmMemoryLibrary;

/**
 * The contents of a {@code subscription} when type is type is {@code eventtype::fd_read} or
 * {@code eventtype::fd_write}.
 */
public final class SubscriptionFdReadwrite {

    /** Static methods only; don't let anyone instantiate this class. */
    private SubscriptionFdReadwrite() {
    }

    /** Size of this structure, in bytes. */
    public static final int BYTES = 4;

    /** Reads the file descriptor on which to wait for it to become ready for reading or writing. */
    public static int readFileDescriptor(Node node, WasmMemoryLibrary memoryLib, WasmMemory memory, int address) {
        return memoryLib.load_i32(memory, node, address + 0);
    }

    /**
     * Writes the file descriptor on which to wait for it to become ready for reading or writing.
     */
    public static void writeFileDescriptor(Node node, WasmMemoryLibrary memoryLib, WasmMemory memory, int address, int value) {
        memoryLib.store_i32(memory, node, address + 0, value);
    }

}
