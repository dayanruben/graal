/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.truffle.api.strings.test.ops;

import static com.oracle.truffle.api.strings.TruffleString.Encoding.UTF_8;
import static org.junit.runners.Parameterized.Parameter;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.strings.TruffleStringIterator;
import com.oracle.truffle.api.strings.test.TStringTestBase;

@RunWith(Parameterized.class)
public class TStringForwardIteratorTest extends TStringTestBase {

    @Parameter public TruffleString.CreateCodePointIteratorNode createIteratorNode;
    @Parameter(1) public TruffleStringIterator.NextNode nextNode;
    @Parameter(2) public TruffleString.ErrorHandling errorHandling;

    @Parameters(name = "{0}, {1}, {2}")
    public static Iterable<Object[]> data() {
        return withErrorHandling(Arrays.asList(
                        new Object[]{TruffleString.CreateCodePointIteratorNode.create(), TruffleStringIterator.NextNode.create()},
                        new Object[]{TruffleString.CreateCodePointIteratorNode.getUncached(), TruffleStringIterator.NextNode.getUncached()}));
    }

    @Test
    public void testAll() throws Exception {
        forAllStrings(true, (a, array, codeRange, isValid, encoding, codepoints, byteIndices) -> {
            TruffleStringIterator iterator = createIteratorNode.execute(a, encoding, errorHandling);
            for (int i = 0; i < codepoints.length; i++) {
                Assert.assertEquals(byteIndices[i], iterator.getByteIndex());
                checkCodepoint(isValid, encoding, codepoints, i, nextNode.execute(iterator, encoding), errorHandling);
            }
        });
    }

    @Test
    public void testNull() throws Exception {
        checkNullSE((s1, e) -> createIteratorNode.execute(s1, e, errorHandling));
        expectNullPointerException(() -> nextNode.execute(null, UTF_8));
        expectNullPointerException(() -> nextNode.execute(createIteratorNode.execute(S_UTF8, UTF_8, errorHandling), null));
    }
}
