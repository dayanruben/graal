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

import com.oracle.truffle.api.memory.ByteArraySupport;
import com.oracle.truffle.api.nodes.Node;
import org.graalvm.wasm.memory.WasmMemory;
import org.graalvm.wasm.memory.WasmMemoryLibrary;

/** A directory entry. */
public final class Dirent {

    /** Static methods only; don't let anyone instantiate this class. */
    private Dirent() {
    }

    /** Size of this structure, in bytes. */
    public static final int BYTES = 24;

    /** Reads the offset of the next directory entry stored in this directory. */
    public static long readDNext(Node node, WasmMemoryLibrary memoryLib, WasmMemory memory, int address) {
        return memoryLib.load_i64(memory, node, address + 0);
    }

    /** Writes the offset of the next directory entry stored in this directory. */
    public static void writeDNext(Node node, WasmMemoryLibrary memoryLib, WasmMemory memory, int address, long value) {
        memoryLib.store_i64(memory, node, address + 0, value);
    }

    /** Writes the offset of the next directory entry stored in this directory. */
    public static void writeDNextToByteArray(byte[] buffer, int address, long value) {
        ByteArraySupport.littleEndian().putLong(buffer, address + 0, value);
    }

    /** Reads the serial number of the file referred to by this directory entry. */
    public static long readDIno(Node node, WasmMemoryLibrary memoryLib, WasmMemory memory, int address) {
        return memoryLib.load_i64(memory, node, address + 8);
    }

    /** Writes the serial number of the file referred to by this directory entry. */
    public static void writeDIno(Node node, WasmMemoryLibrary memoryLib, WasmMemory memory, int address, long value) {
        memoryLib.store_i64(memory, node, address + 8, value);
    }

    /** Writes the serial number of the file referred to by this directory entry. */
    public static void writeDInoToByteArray(byte[] buffer, int address, long value) {
        ByteArraySupport.littleEndian().putLong(buffer, address + 8, value);
    }

    /** Reads the length of the name of the directory entry. */
    public static int readDNamlen(Node node, WasmMemoryLibrary memoryLib, WasmMemory memory, int address) {
        return memoryLib.load_i32(memory, node, address + 16);
    }

    /** Writes the length of the name of the directory entry. */
    public static void writeDNamlen(Node node, WasmMemoryLibrary memoryLib, WasmMemory memory, int address, int value) {
        memoryLib.store_i32(memory, node, address + 16, value);
    }

    /** Writes the length of the name of the directory entry. */
    public static void writeDNamlen(byte[] buffer, int address, int value) {
        ByteArraySupport.littleEndian().putInt(buffer, address + 16, value);
    }

    /** Reads the type of the file referred to by this directory entry. */
    public static Filetype readDType(Node node, WasmMemoryLibrary memoryLib, WasmMemory memory, int address) {
        return Filetype.fromValue((byte) memoryLib.load_i32_8u(memory, node, address + 20));
    }

    /** Writes the type of the file referred to by this directory entry. */
    public static void writeDType(Node node, WasmMemoryLibrary memoryLib, WasmMemory memory, int address, Filetype value) {
        memoryLib.store_i32_8(memory, node, address + 20, value.toValue());
    }

    /** Writes the type of the file referred to by this directory entry. */
    public static void writeDType(byte[] buffer, int address, Filetype value) {
        ByteArraySupport.littleEndian().putByte(buffer, address + 20, value.toValue());
    }
}
