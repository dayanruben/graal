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
package org.graalvm.wasm.constants;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Locale;

public final class Instructions {

    public static final int UNREACHABLE = 0x00;
    public static final int NOP = 0x01;

    public static final int BLOCK = 0x02;
    public static final int LOOP = 0x03;
    public static final int IF = 0x04;
    public static final int ELSE = 0x05;
    public static final int END = 0x0B;

    public static final int BR = 0x0C;
    public static final int BR_IF = 0x0D;
    public static final int BR_TABLE = 0x0E;

    public static final int RETURN = 0x0F;
    public static final int CALL = 0x10;
    public static final int CALL_INDIRECT = 0x11;

    public static final int DROP = 0x1A;
    public static final int SELECT = 0x1B;
    public static final int SELECT_T = 0x1C;

    public static final int LOCAL_GET = 0x20;
    public static final int LOCAL_SET = 0x21;
    public static final int LOCAL_TEE = 0x22;
    public static final int GLOBAL_GET = 0x23;
    public static final int GLOBAL_SET = 0x24;

    public static final int TABLE_GET = 0x25;
    public static final int TABLE_SET = 0x26;

    public static final int I32_LOAD = 0x28;
    public static final int I64_LOAD = 0x29;
    public static final int F32_LOAD = 0x2A;
    public static final int F64_LOAD = 0x2B;
    public static final int I32_LOAD8_S = 0x2C;
    public static final int I32_LOAD8_U = 0x2D;
    public static final int I32_LOAD16_S = 0x2E;
    public static final int I32_LOAD16_U = 0x2F;
    public static final int I64_LOAD8_S = 0x30;
    public static final int I64_LOAD8_U = 0x31;
    public static final int I64_LOAD16_S = 0x32;
    public static final int I64_LOAD16_U = 0x33;
    public static final int I64_LOAD32_S = 0x34;
    public static final int I64_LOAD32_U = 0x35;
    public static final int I32_STORE = 0x36;
    public static final int I64_STORE = 0x37;
    public static final int F32_STORE = 0x38;
    public static final int F64_STORE = 0x39;
    public static final int I32_STORE_8 = 0x3A;
    public static final int I32_STORE_16 = 0x3B;
    public static final int I64_STORE_8 = 0x3C;
    public static final int I64_STORE_16 = 0x3D;
    public static final int I64_STORE_32 = 0x3E;
    public static final int MEMORY_SIZE = 0x3F;
    public static final int MEMORY_GROW = 0x40;

    public static final int I32_CONST = 0x41;
    public static final int I64_CONST = 0x42;
    public static final int F32_CONST = 0x43;
    public static final int F64_CONST = 0x44;

    public static final int I32_EQZ = 0x45;
    public static final int I32_EQ = 0x46;
    public static final int I32_NE = 0x47;
    public static final int I32_LT_S = 0x48;
    public static final int I32_LT_U = 0x49;
    public static final int I32_GT_S = 0x4A;
    public static final int I32_GT_U = 0x4B;
    public static final int I32_LE_S = 0x4C;
    public static final int I32_LE_U = 0x4D;
    public static final int I32_GE_S = 0x4E;
    public static final int I32_GE_U = 0x4F;

    public static final int I64_EQZ = 0x50;
    public static final int I64_EQ = 0x51;
    public static final int I64_NE = 0x52;
    public static final int I64_LT_S = 0x53;
    public static final int I64_LT_U = 0x54;
    public static final int I64_GT_S = 0x55;
    public static final int I64_GT_U = 0x56;
    public static final int I64_LE_S = 0x57;
    public static final int I64_LE_U = 0x58;
    public static final int I64_GE_S = 0x59;
    public static final int I64_GE_U = 0x5A;

    public static final int F32_EQ = 0x5B;
    public static final int F32_NE = 0x5C;
    public static final int F32_LT = 0x5D;
    public static final int F32_GT = 0x5E;
    public static final int F32_LE = 0x5F;
    public static final int F32_GE = 0x60;

    public static final int F64_EQ = 0x61;
    public static final int F64_NE = 0x62;
    public static final int F64_LT = 0x63;
    public static final int F64_GT = 0x64;
    public static final int F64_LE = 0x65;
    public static final int F64_GE = 0x66;

    public static final int I32_CLZ = 0x67;
    public static final int I32_CTZ = 0x68;
    public static final int I32_POPCNT = 0x69;
    public static final int I32_ADD = 0x6A;
    public static final int I32_SUB = 0x6B;
    public static final int I32_MUL = 0x6C;
    public static final int I32_DIV_S = 0x6D;
    public static final int I32_DIV_U = 0x6E;
    public static final int I32_REM_S = 0x6F;
    public static final int I32_REM_U = 0x70;
    public static final int I32_AND = 0x71;
    public static final int I32_OR = 0x72;
    public static final int I32_XOR = 0x73;
    public static final int I32_SHL = 0x74;
    public static final int I32_SHR_S = 0x75;
    public static final int I32_SHR_U = 0x76;
    public static final int I32_ROTL = 0x77;
    public static final int I32_ROTR = 0x78;

    public static final int I64_CLZ = 0x79;
    public static final int I64_CTZ = 0x7A;
    public static final int I64_POPCNT = 0x7B;
    public static final int I64_ADD = 0x7C;
    public static final int I64_SUB = 0x7D;
    public static final int I64_MUL = 0x7E;
    public static final int I64_DIV_S = 0x7F;
    public static final int I64_DIV_U = 0x80;
    public static final int I64_REM_S = 0x81;
    public static final int I64_REM_U = 0x82;
    public static final int I64_AND = 0x83;
    public static final int I64_OR = 0x84;
    public static final int I64_XOR = 0x85;
    public static final int I64_SHL = 0x86;
    public static final int I64_SHR_S = 0x87;
    public static final int I64_SHR_U = 0x88;
    public static final int I64_ROTL = 0x89;
    public static final int I64_ROTR = 0x8A;

    public static final int F32_ABS = 0x8B;
    public static final int F32_NEG = 0x8C;
    public static final int F32_CEIL = 0x8D;
    public static final int F32_FLOOR = 0x8E;
    public static final int F32_TRUNC = 0x8F;
    public static final int F32_NEAREST = 0x90;
    public static final int F32_SQRT = 0x91;
    public static final int F32_ADD = 0x92;
    public static final int F32_SUB = 0x93;
    public static final int F32_MUL = 0x94;
    public static final int F32_DIV = 0x95;
    public static final int F32_MIN = 0x96;
    public static final int F32_MAX = 0x97;
    public static final int F32_COPYSIGN = 0x98;

    public static final int F64_ABS = 0x99;
    public static final int F64_NEG = 0x9A;
    public static final int F64_CEIL = 0x9B;
    public static final int F64_FLOOR = 0x9C;
    public static final int F64_TRUNC = 0x9D;
    public static final int F64_NEAREST = 0x9E;
    public static final int F64_SQRT = 0x9F;
    public static final int F64_ADD = 0xA0;
    public static final int F64_SUB = 0xA1;
    public static final int F64_MUL = 0xA2;
    public static final int F64_DIV = 0xA3;
    public static final int F64_MIN = 0xA4;
    public static final int F64_MAX = 0xA5;
    public static final int F64_COPYSIGN = 0xA6;

    public static final int I32_WRAP_I64 = 0xA7;
    public static final int I32_TRUNC_F32_S = 0xA8;
    public static final int I32_TRUNC_F32_U = 0xA9;
    public static final int I32_TRUNC_F64_S = 0xAA;
    public static final int I32_TRUNC_F64_U = 0xAB;
    public static final int I64_EXTEND_I32_S = 0xAC;
    public static final int I64_EXTEND_I32_U = 0xAD;
    public static final int I64_TRUNC_F32_S = 0xAE;
    public static final int I64_TRUNC_F32_U = 0xAF;
    public static final int I64_TRUNC_F64_S = 0xB0;
    public static final int I64_TRUNC_F64_U = 0xB1;
    public static final int F32_CONVERT_I32_S = 0xB2;
    public static final int F32_CONVERT_I32_U = 0xB3;
    public static final int F32_CONVERT_I64_S = 0xB4;
    public static final int F32_CONVERT_I64_U = 0xB5;
    public static final int F32_DEMOTE_F64 = 0xB6;
    public static final int F64_CONVERT_I32_S = 0xB7;
    public static final int F64_CONVERT_I32_U = 0xB8;
    public static final int F64_CONVERT_I64_S = 0xB9;
    public static final int F64_CONVERT_I64_U = 0xBA;
    public static final int F64_PROMOTE_F32 = 0xBB;
    public static final int I32_REINTERPRET_F32 = 0xBC;
    public static final int I64_REINTERPRET_F64 = 0xBD;
    public static final int F32_REINTERPRET_I32 = 0xBE;
    public static final int F64_REINTERPRET_I64 = 0xBF;

    public static final int MISC = 0xFC;

    public static final int I32_TRUNC_SAT_F32_S = 0x00;
    public static final int I32_TRUNC_SAT_F32_U = 0x01;
    public static final int I32_TRUNC_SAT_F64_S = 0x02;
    public static final int I32_TRUNC_SAT_F64_U = 0x03;
    public static final int I64_TRUNC_SAT_F32_S = 0x04;
    public static final int I64_TRUNC_SAT_F32_U = 0x05;
    public static final int I64_TRUNC_SAT_F64_S = 0x06;
    public static final int I64_TRUNC_SAT_F64_U = 0x07;

    public static final int I32_EXTEND8_S = 0xC0;
    public static final int I32_EXTEND16_S = 0xC1;
    public static final int I64_EXTEND8_S = 0xC2;
    public static final int I64_EXTEND16_S = 0xC3;
    public static final int I64_EXTEND32_S = 0xC4;

    public static final int REF_NULL = 0xD0;
    public static final int REF_IS_NULL = 0xD1;
    public static final int REF_FUNC = 0xD2;

    public static final int MEMORY_INIT = 8;
    public static final int DATA_DROP = 9;
    public static final int MEMORY_COPY = 10;
    public static final int MEMORY_FILL = 11;
    public static final int TABLE_INIT = 12;
    public static final int ELEM_DROP = 13;
    public static final int TABLE_COPY = 14;
    public static final int TABLE_GROW = 15;
    public static final int TABLE_SIZE = 16;
    public static final int TABLE_FILL = 17;

    public static final int ATOMIC = 0xFE;

    public static final int ATOMIC_NOTIFY = 0x00;
    public static final int ATOMIC_WAIT32 = 0x01;
    public static final int ATOMIC_WAIT64 = 0x02;

    public static final int ATOMIC_FENCE = 0x03;

    public static final int ATOMIC_I32_LOAD = 0x10;
    public static final int ATOMIC_I64_LOAD = 0x11;
    public static final int ATOMIC_I32_LOAD8_U = 0x12;
    public static final int ATOMIC_I32_LOAD16_U = 0x13;
    public static final int ATOMIC_I64_LOAD8_U = 0x14;
    public static final int ATOMIC_I64_LOAD16_U = 0x15;
    public static final int ATOMIC_I64_LOAD32_U = 0x16;
    public static final int ATOMIC_I32_STORE = 0x17;
    public static final int ATOMIC_I64_STORE = 0x18;
    public static final int ATOMIC_I32_STORE8 = 0x19;
    public static final int ATOMIC_I32_STORE16 = 0x1A;
    public static final int ATOMIC_I64_STORE8 = 0x1B;
    public static final int ATOMIC_I64_STORE16 = 0x1C;
    public static final int ATOMIC_I64_STORE32 = 0x1D;

    public static final int ATOMIC_I32_RMW_ADD = 0x1E;
    public static final int ATOMIC_I64_RMW_ADD = 0x1F;
    public static final int ATOMIC_I32_RMW8_U_ADD = 0x20;
    public static final int ATOMIC_I32_RMW16_U_ADD = 0x21;
    public static final int ATOMIC_I64_RMW8_U_ADD = 0x22;
    public static final int ATOMIC_I64_RMW16_U_ADD = 0x23;
    public static final int ATOMIC_I64_RMW32_U_ADD = 0x24;
    public static final int ATOMIC_I32_RMW_SUB = 0x25;
    public static final int ATOMIC_I64_RMW_SUB = 0x26;
    public static final int ATOMIC_I32_RMW8_U_SUB = 0x27;
    public static final int ATOMIC_I32_RMW16_U_SUB = 0x28;
    public static final int ATOMIC_I64_RMW8_U_SUB = 0x29;
    public static final int ATOMIC_I64_RMW16_U_SUB = 0x2A;
    public static final int ATOMIC_I64_RMW32_U_SUB = 0x2B;
    public static final int ATOMIC_I32_RMW_AND = 0x2C;
    public static final int ATOMIC_I64_RMW_AND = 0x2D;
    public static final int ATOMIC_I32_RMW8_U_AND = 0x2E;
    public static final int ATOMIC_I32_RMW16_U_AND = 0x2F;
    public static final int ATOMIC_I64_RMW8_U_AND = 0x30;
    public static final int ATOMIC_I64_RMW16_U_AND = 0x31;
    public static final int ATOMIC_I64_RMW32_U_AND = 0x32;
    public static final int ATOMIC_I32_RMW_OR = 0x33;
    public static final int ATOMIC_I64_RMW_OR = 0x34;
    public static final int ATOMIC_I32_RMW8_U_OR = 0x35;
    public static final int ATOMIC_I32_RMW16_U_OR = 0x36;
    public static final int ATOMIC_I64_RMW8_U_OR = 0x37;
    public static final int ATOMIC_I64_RMW16_U_OR = 0x38;
    public static final int ATOMIC_I64_RMW32_U_OR = 0x39;
    public static final int ATOMIC_I32_RMW_XOR = 0x3A;
    public static final int ATOMIC_I64_RMW_XOR = 0x3B;
    public static final int ATOMIC_I32_RMW8_U_XOR = 0x3C;
    public static final int ATOMIC_I32_RMW16_U_XOR = 0x3D;
    public static final int ATOMIC_I64_RMW8_U_XOR = 0x3E;
    public static final int ATOMIC_I64_RMW16_U_XOR = 0x3F;
    public static final int ATOMIC_I64_RMW32_U_XOR = 0x40;
    public static final int ATOMIC_I32_RMW_XCHG = 0x41;
    public static final int ATOMIC_I64_RMW_XCHG = 0x42;
    public static final int ATOMIC_I32_RMW8_U_XCHG = 0x43;
    public static final int ATOMIC_I32_RMW16_U_XCHG = 0x44;
    public static final int ATOMIC_I64_RMW8_U_XCHG = 0x45;
    public static final int ATOMIC_I64_RMW16_U_XCHG = 0x46;
    public static final int ATOMIC_I64_RMW32_U_XCHG = 0x47;

    public static final int ATOMIC_I32_RMW_CMPXCHG = 0x48;
    public static final int ATOMIC_I64_RMW_CMPXCHG = 0x49;
    public static final int ATOMIC_I32_RMW8_U_CMPXCHG = 0x4A;
    public static final int ATOMIC_I32_RMW16_U_CMPXCHG = 0x4B;
    public static final int ATOMIC_I64_RMW8_U_CMPXCHG = 0x4C;
    public static final int ATOMIC_I64_RMW16_U_CMPXCHG = 0x4D;
    public static final int ATOMIC_I64_RMW32_U_CMPXCHG = 0x4E;

    public static final int VECTOR = 0xFD;

    public static final int VECTOR_V128_LOAD = 0x00;
    public static final int VECTOR_V128_LOAD8X8_S = 0x01;
    public static final int VECTOR_V128_LOAD8X8_U = 0x02;
    public static final int VECTOR_V128_LOAD16X4_S = 0x03;
    public static final int VECTOR_V128_LOAD16X4_U = 0x04;
    public static final int VECTOR_V128_LOAD32X2_S = 0x05;
    public static final int VECTOR_V128_LOAD32X2_U = 0x06;
    public static final int VECTOR_V128_LOAD8_SPLAT = 0x07;
    public static final int VECTOR_V128_LOAD16_SPLAT = 0x08;
    public static final int VECTOR_V128_LOAD32_SPLAT = 0x09;
    public static final int VECTOR_V128_LOAD64_SPLAT = 0x0A;
    public static final int VECTOR_V128_LOAD32_ZERO = 0x5C;
    public static final int VECTOR_V128_LOAD64_ZERO = 0x5D;
    public static final int VECTOR_V128_STORE = 0x0B;
    public static final int VECTOR_V128_LOAD8_LANE = 0x54;
    public static final int VECTOR_V128_LOAD16_LANE = 0x55;
    public static final int VECTOR_V128_LOAD32_LANE = 0x56;
    public static final int VECTOR_V128_LOAD64_LANE = 0x57;
    public static final int VECTOR_V128_STORE8_LANE = 0x58;
    public static final int VECTOR_V128_STORE16_LANE = 0x59;
    public static final int VECTOR_V128_STORE32_LANE = 0x5A;
    public static final int VECTOR_V128_STORE64_LANE = 0x5B;

    public static final int VECTOR_V128_CONST = 0x0C;

    public static final int VECTOR_I8X16_SHUFFLE = 0x0D;

    public static final int VECTOR_I8X16_EXTRACT_LANE_S = 0x15;
    public static final int VECTOR_I8X16_EXTRACT_LANE_U = 0x16;
    public static final int VECTOR_I8X16_REPLACE_LANE = 0x17;
    public static final int VECTOR_I16X8_EXTRACT_LANE_S = 0x18;
    public static final int VECTOR_I16X8_EXTRACT_LANE_U = 0x19;
    public static final int VECTOR_I16X8_REPLACE_LANE = 0x1A;
    public static final int VECTOR_I32X4_EXTRACT_LANE = 0x1B;
    public static final int VECTOR_I32X4_REPLACE_LANE = 0x1C;
    public static final int VECTOR_I64X2_EXTRACT_LANE = 0x1D;
    public static final int VECTOR_I64X2_REPLACE_LANE = 0x1E;
    public static final int VECTOR_F32X4_EXTRACT_LANE = 0x1F;
    public static final int VECTOR_F32X4_REPLACE_LANE = 0x20;
    public static final int VECTOR_F64X2_EXTRACT_LANE = 0x21;
    public static final int VECTOR_F64X2_REPLACE_LANE = 0x22;

    public static final int VECTOR_I8X16_SWIZZLE = 0x0E;
    public static final int VECTOR_I8X16_SPLAT = 0x0F;
    public static final int VECTOR_I16X8_SPLAT = 0x10;
    public static final int VECTOR_I32X4_SPLAT = 0x11;
    public static final int VECTOR_I64X2_SPLAT = 0x12;
    public static final int VECTOR_F32X4_SPLAT = 0x13;
    public static final int VECTOR_F64X2_SPLAT = 0x14;

    public static final int VECTOR_I8X16_EQ = 0x23;
    public static final int VECTOR_I8X16_NE = 0x24;
    public static final int VECTOR_I8X16_LT_S = 0x25;
    public static final int VECTOR_I8X16_LT_U = 0x26;
    public static final int VECTOR_I8X16_GT_S = 0x27;
    public static final int VECTOR_I8X16_GT_U = 0x28;
    public static final int VECTOR_I8X16_LE_S = 0x29;
    public static final int VECTOR_I8X16_LE_U = 0x2A;
    public static final int VECTOR_I8X16_GE_S = 0x2B;
    public static final int VECTOR_I8X16_GE_U = 0x2C;

    public static final int VECTOR_I16X8_EQ = 0x2D;
    public static final int VECTOR_I16X8_NE = 0x2E;
    public static final int VECTOR_I16X8_LT_S = 0x2F;
    public static final int VECTOR_I16X8_LT_U = 0x30;
    public static final int VECTOR_I16X8_GT_S = 0x31;
    public static final int VECTOR_I16X8_GT_U = 0x32;
    public static final int VECTOR_I16X8_LE_S = 0x33;
    public static final int VECTOR_I16X8_LE_U = 0x34;
    public static final int VECTOR_I16X8_GE_S = 0x35;
    public static final int VECTOR_I16X8_GE_U = 0x36;

    public static final int VECTOR_I32X4_EQ = 0x37;
    public static final int VECTOR_I32X4_NE = 0x38;
    public static final int VECTOR_I32X4_LT_S = 0x39;
    public static final int VECTOR_I32X4_LT_U = 0x3A;
    public static final int VECTOR_I32X4_GT_S = 0x3B;
    public static final int VECTOR_I32X4_GT_U = 0x3C;
    public static final int VECTOR_I32X4_LE_S = 0x3D;
    public static final int VECTOR_I32X4_LE_U = 0x3E;
    public static final int VECTOR_I32X4_GE_S = 0x3F;
    public static final int VECTOR_I32X4_GE_U = 0x40;

    public static final int VECTOR_I64X2_EQ = 0xD6;
    public static final int VECTOR_I64X2_NE = 0xD7;
    public static final int VECTOR_I64X2_LT_S = 0xD8;
    public static final int VECTOR_I64X2_GT_S = 0xD9;
    public static final int VECTOR_I64X2_LE_S = 0xDA;
    public static final int VECTOR_I64X2_GE_S = 0xDB;

    public static final int VECTOR_F32X4_EQ = 0x41;
    public static final int VECTOR_F32X4_NE = 0x42;
    public static final int VECTOR_F32X4_LT = 0x43;
    public static final int VECTOR_F32X4_GT = 0x44;
    public static final int VECTOR_F32X4_LE = 0x45;
    public static final int VECTOR_F32X4_GE = 0x46;

    public static final int VECTOR_F64X2_EQ = 0x47;
    public static final int VECTOR_F64X2_NE = 0x48;
    public static final int VECTOR_F64X2_LT = 0x49;
    public static final int VECTOR_F64X2_GT = 0x4A;
    public static final int VECTOR_F64X2_LE = 0x4B;
    public static final int VECTOR_F64X2_GE = 0x4C;

    public static final int VECTOR_V128_NOT = 0x4D;
    public static final int VECTOR_V128_AND = 0x4E;
    public static final int VECTOR_V128_ANDNOT = 0x4F;
    public static final int VECTOR_V128_OR = 0x50;
    public static final int VECTOR_V128_XOR = 0x51;
    public static final int VECTOR_V128_BITSELECT = 0x52;
    public static final int VECTOR_V128_ANY_TRUE = 0x53;

    public static final int VECTOR_I8X16_ABS = 0x60;
    public static final int VECTOR_I8X16_NEG = 0x61;
    public static final int VECTOR_I8X16_POPCNT = 0x62;
    public static final int VECTOR_I8X16_ALL_TRUE = 0x63;
    public static final int VECTOR_I8X16_BITMASK = 0x64;
    public static final int VECTOR_I8X16_NARROW_I16X8_S = 0x65;
    public static final int VECTOR_I8X16_NARROW_I16X8_U = 0x66;
    public static final int VECTOR_I8X16_SHL = 0x6B;
    public static final int VECTOR_I8X16_SHR_S = 0x6C;
    public static final int VECTOR_I8X16_SHR_U = 0x6D;
    public static final int VECTOR_I8X16_ADD = 0x6E;
    public static final int VECTOR_I8X16_ADD_SAT_S = 0x6F;
    public static final int VECTOR_I8X16_ADD_SAT_U = 0x70;
    public static final int VECTOR_I8X16_SUB = 0x71;
    public static final int VECTOR_I8X16_SUB_SAT_S = 0x72;
    public static final int VECTOR_I8X16_SUB_SAT_U = 0x73;
    public static final int VECTOR_I8X16_MIN_S = 0x76;
    public static final int VECTOR_I8X16_MIN_U = 0x77;
    public static final int VECTOR_I8X16_MAX_S = 0x78;
    public static final int VECTOR_I8X16_MAX_U = 0x79;
    public static final int VECTOR_I8X16_AVGR_U = 0x7B;

    public static final int VECTOR_I16X8_EXTADD_PAIRWISE_I8X16_S = 0x7C;
    public static final int VECTOR_I16X8_EXTADD_PAIRWISE_I8X16_U = 0x7D;
    public static final int VECTOR_I16X8_ABS = 0x80;
    public static final int VECTOR_I16X8_NEG = 0x81;
    public static final int VECTOR_I16X8_Q15MULR_SAT_S = 0x82;
    public static final int VECTOR_I16X8_ALL_TRUE = 0x83;
    public static final int VECTOR_I16X8_BITMASK = 0x84;
    public static final int VECTOR_I16X8_NARROW_I32X4_S = 0x85;
    public static final int VECTOR_I16X8_NARROW_I32X4_U = 0x86;
    public static final int VECTOR_I16X8_EXTEND_LOW_I8X16_S = 0x87;
    public static final int VECTOR_I16X8_EXTEND_HIGH_I8X16_S = 0x88;
    public static final int VECTOR_I16X8_EXTEND_LOW_I8X16_U = 0x89;
    public static final int VECTOR_I16X8_EXTEND_HIGH_I8X16_U = 0x8A;
    public static final int VECTOR_I16X8_SHL = 0x8B;
    public static final int VECTOR_I16X8_SHR_S = 0x8C;
    public static final int VECTOR_I16X8_SHR_U = 0x8D;
    public static final int VECTOR_I16X8_ADD = 0x8E;
    public static final int VECTOR_I16X8_ADD_SAT_S = 0x8F;
    public static final int VECTOR_I16X8_ADD_SAT_U = 0x90;
    public static final int VECTOR_I16X8_SUB = 0x91;
    public static final int VECTOR_I16X8_SUB_SAT_S = 0x92;
    public static final int VECTOR_I16X8_SUB_SAT_U = 0x93;
    public static final int VECTOR_I16X8_MUL = 0x95;
    public static final int VECTOR_I16X8_MIN_S = 0x96;
    public static final int VECTOR_I16X8_MIN_U = 0x97;
    public static final int VECTOR_I16X8_MAX_S = 0x98;
    public static final int VECTOR_I16X8_MAX_U = 0x99;
    public static final int VECTOR_I16X8_AVGR_U = 0x9B;
    public static final int VECTOR_I16X8_EXTMUL_LOW_I8X16_S = 0x9C;
    public static final int VECTOR_I16X8_EXTMUL_HIGH_I8X16_S = 0x9D;
    public static final int VECTOR_I16X8_EXTMUL_LOW_I8X16_U = 0x9E;
    public static final int VECTOR_I16X8_EXTMUL_HIGH_I8X16_U = 0x9F;

    public static final int VECTOR_I32X4_EXTADD_PAIRWISE_I16X8_S = 0x7E;
    public static final int VECTOR_I32X4_EXTADD_PAIRWISE_I16X8_U = 0x7F;
    public static final int VECTOR_I32X4_ABS = 0xA0;
    public static final int VECTOR_I32X4_NEG = 0xA1;
    public static final int VECTOR_I32X4_ALL_TRUE = 0xA3;
    public static final int VECTOR_I32X4_BITMASK = 0xA4;
    public static final int VECTOR_I32X4_EXTEND_LOW_I16X8_S = 0xA7;
    public static final int VECTOR_I32X4_EXTEND_HIGH_I16X8_S = 0xA8;
    public static final int VECTOR_I32X4_EXTEND_LOW_I16X8_U = 0xA9;
    public static final int VECTOR_I32X4_EXTEND_HIGH_I16X8_U = 0xAA;
    public static final int VECTOR_I32X4_SHL = 0xAB;
    public static final int VECTOR_I32X4_SHR_S = 0xAC;
    public static final int VECTOR_I32X4_SHR_U = 0xAD;
    public static final int VECTOR_I32X4_ADD = 0xAE;
    public static final int VECTOR_I32X4_SUB = 0xB1;
    public static final int VECTOR_I32X4_MUL = 0xB5;
    public static final int VECTOR_I32X4_MIN_S = 0xB6;
    public static final int VECTOR_I32X4_MIN_U = 0xB7;
    public static final int VECTOR_I32X4_MAX_S = 0xB8;
    public static final int VECTOR_I32X4_MAX_U = 0xB9;
    public static final int VECTOR_I32X4_DOT_I16X8_S = 0xBA;
    public static final int VECTOR_I32X4_EXTMUL_LOW_I16X8_S = 0xBC;
    public static final int VECTOR_I32X4_EXTMUL_HIGH_I16X8_S = 0xBD;
    public static final int VECTOR_I32X4_EXTMUL_LOW_I16X8_U = 0xBE;
    public static final int VECTOR_I32X4_EXTMUL_HIGH_I16X8_U = 0xBF;

    public static final int VECTOR_I64X2_ABS = 0xC0;
    public static final int VECTOR_I64X2_NEG = 0xC1;
    public static final int VECTOR_I64X2_ALL_TRUE = 0xC3;
    public static final int VECTOR_I64X2_BITMASK = 0xC4;
    public static final int VECTOR_I64X2_EXTEND_LOW_I32X4_S = 0xC7;
    public static final int VECTOR_I64X2_EXTEND_HIGH_I32X4_S = 0xC8;
    public static final int VECTOR_I64X2_EXTEND_LOW_I32X4_U = 0xC9;
    public static final int VECTOR_I64X2_EXTEND_HIGH_I32X4_U = 0xCA;
    public static final int VECTOR_I64X2_SHL = 0xCB;
    public static final int VECTOR_I64X2_SHR_S = 0xCC;
    public static final int VECTOR_I64X2_SHR_U = 0xCD;
    public static final int VECTOR_I64X2_ADD = 0xCE;
    public static final int VECTOR_I64X2_SUB = 0xD1;
    public static final int VECTOR_I64X2_MUL = 0xD5;
    public static final int VECTOR_I64X2_EXTMUL_LOW_I32X4_S = 0xDC;
    public static final int VECTOR_I64X2_EXTMUL_HIGH_I32X4_S = 0xDD;
    public static final int VECTOR_I64X2_EXTMUL_LOW_I32X4_U = 0xDE;
    public static final int VECTOR_I64X2_EXTMUL_HIGH_I32X4_U = 0xDF;

    public static final int VECTOR_F32X4_CEIL = 0x67;
    public static final int VECTOR_F32X4_FLOOR = 0x68;
    public static final int VECTOR_F32X4_TRUNC = 0x69;
    public static final int VECTOR_F32X4_NEAREST = 0x6A;
    public static final int VECTOR_F32X4_ABS = 0xE0;
    public static final int VECTOR_F32X4_NEG = 0xE1;
    public static final int VECTOR_F32X4_SQRT = 0xE3;
    public static final int VECTOR_F32X4_ADD = 0xE4;
    public static final int VECTOR_F32X4_SUB = 0xE5;
    public static final int VECTOR_F32X4_MUL = 0xE6;
    public static final int VECTOR_F32X4_DIV = 0xE7;
    public static final int VECTOR_F32X4_MIN = 0xE8;
    public static final int VECTOR_F32X4_MAX = 0xE9;
    public static final int VECTOR_F32X4_PMIN = 0xEA;
    public static final int VECTOR_F32X4_PMAX = 0xEB;

    public static final int VECTOR_F64X2_CEIL = 0x74;
    public static final int VECTOR_F64X2_FLOOR = 0x75;
    public static final int VECTOR_F64X2_TRUNC = 0x7A;
    public static final int VECTOR_F64X2_NEAREST = 0x94;
    public static final int VECTOR_F64X2_ABS = 0xEC;
    public static final int VECTOR_F64X2_NEG = 0xED;
    public static final int VECTOR_F64X2_SQRT = 0xEF;
    public static final int VECTOR_F64X2_ADD = 0xF0;
    public static final int VECTOR_F64X2_SUB = 0xF1;
    public static final int VECTOR_F64X2_MUL = 0xF2;
    public static final int VECTOR_F64X2_DIV = 0xF3;
    public static final int VECTOR_F64X2_MIN = 0xF4;
    public static final int VECTOR_F64X2_MAX = 0xF5;
    public static final int VECTOR_F64X2_PMIN = 0xF6;
    public static final int VECTOR_F64X2_PMAX = 0xF7;

    public static final int VECTOR_I32X4_TRUNC_SAT_F32X4_S = 0xF8;
    public static final int VECTOR_I32X4_TRUNC_SAT_F32X4_U = 0xF9;
    public static final int VECTOR_F32X4_CONVERT_I32X4_S = 0xFA;
    public static final int VECTOR_F32X4_CONVERT_I32X4_U = 0xFB;
    public static final int VECTOR_I32X4_TRUNC_SAT_F64X2_S_ZERO = 0xFC;
    public static final int VECTOR_I32X4_TRUNC_SAT_F64X2_U_ZERO = 0xFD;
    public static final int VECTOR_F64X2_CONVERT_LOW_I32X4_S = 0xFE;
    public static final int VECTOR_F64X2_CONVERT_LOW_I32X4_U = 0xFF;
    public static final int VECTOR_F32X4_DEMOTE_F64X2_ZERO = 0x5E;
    public static final int VECTOR_F64X2_PROMOTE_LOW_F32X4 = 0x5F;

    // Relaxed SIMD
    public static final int VECTOR_I8X16_RELAXED_SWIZZLE = 0x100;
    public static final int VECTOR_I32X4_RELAXED_TRUNC_F32X4_S = 0x101;
    public static final int VECTOR_I32X4_RELAXED_TRUNC_F32X4_U = 0x102;
    public static final int VECTOR_I32X4_RELAXED_TRUNC_F64X2_S_ZERO = 0x103;
    public static final int VECTOR_I32X4_RELAXED_TRUNC_F64X2_U_ZERO = 0x104;
    public static final int VECTOR_F32X4_RELAXED_MADD = 0x105;
    public static final int VECTOR_F32X4_RELAXED_NMADD = 0x106;
    public static final int VECTOR_F64X2_RELAXED_MADD = 0x107;
    public static final int VECTOR_F64X2_RELAXED_NMADD = 0x108;
    public static final int VECTOR_I8X16_RELAXED_LANESELECT = 0x109;
    public static final int VECTOR_I16X8_RELAXED_LANESELECT = 0x10A;
    public static final int VECTOR_I32X4_RELAXED_LANESELECT = 0x10B;
    public static final int VECTOR_I64X2_RELAXED_LANESELECT = 0x10C;
    public static final int VECTOR_F32X4_RELAXED_MIN = 0x10D;
    public static final int VECTOR_F32X4_RELAXED_MAX = 0x10E;
    public static final int VECTOR_F64X2_RELAXED_MIN = 0x10F;
    public static final int VECTOR_F64X2_RELAXED_MAX = 0x110;
    public static final int VECTOR_I16X8_RELAXED_Q15MULR_S = 0x111;
    public static final int VECTOR_I16X8_RELAXED_DOT_I8X16_I7X16_S = 0x112;
    public static final int VECTOR_I32X4_RELAXED_DOT_I8X16_I7X16_ADD_S = 0x113;

    private static String[] decodingTable = new String[256];

    private Instructions() {
    }

    static {
        try {
            for (Field f : Instructions.class.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) && f.getType().isPrimitive()) {
                    int code = f.getInt(null);
                    String representation = f.getName().toLowerCase(Locale.ENGLISH);
                    if (representation.startsWith("i32") || representation.startsWith("i64") ||
                                    representation.startsWith("f32") || representation.startsWith("f64") ||
                                    representation.startsWith("local") || representation.startsWith("global")) {
                        representation = representation.replaceFirst("_", ".");
                    }
                    if (representation.startsWith("atomic") || representation.startsWith("vector")) {
                        continue;
                    }
                    decodingTable[code] = representation;
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unused")
    static String rawDecode(byte[] instructions, int offset, int before, int after) {
        StringBuilder result = new StringBuilder();
        for (int i = offset - before; i <= offset + after; i++) {
            if (i == offset) {
                result.append("-> ");
            } else {
                result.append("   ");
            }
            final int opcode = Byte.toUnsignedInt(instructions[i]);
            String representation = decodingTable[opcode];
            result.append(String.format("%03d", opcode)).append(" ").append(representation).append("\n");
        }
        return result.toString();
    }
}
