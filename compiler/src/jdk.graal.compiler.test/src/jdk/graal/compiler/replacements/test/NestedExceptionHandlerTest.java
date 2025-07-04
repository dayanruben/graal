/*
 * Copyright (c) 2017, 2025, Oracle and/or its affiliates. All rights reserved.
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
package jdk.graal.compiler.replacements.test;

import static jdk.graal.compiler.core.common.GraalOptions.RemoveNeverExecutedCode;
import static jdk.graal.compiler.core.common.GraalOptions.UseExceptionProbability;
import static jdk.graal.compiler.core.common.GraalOptions.UseTypeCheckHints;

import jdk.graal.compiler.api.directives.GraalDirectives;
import jdk.graal.compiler.core.phases.HighTier;
import jdk.graal.compiler.core.test.GraalCompilerTest;
import jdk.graal.compiler.nodes.StructuredGraph.AllowAssumptions;
import jdk.graal.compiler.options.OptionValues;
import org.junit.Ignore;
import org.junit.Test;

import jdk.vm.ci.meta.ResolvedJavaMethod;

public class NestedExceptionHandlerTest extends GraalCompilerTest {

    @BytecodeParserNeverInline(invokeWithException = true)
    public static void foo() {
    }

    @BytecodeParserNeverInline(invokeWithException = true)
    public static void bar() {
        throw new NegativeArraySizeException();
    }

    public static int nestedExceptionHandler() {
        int flag = 0;
        try {
            try {
                try {
                    foo();
                } catch (NegativeArraySizeException e) {
                    flag = -1;
                }
                bar();
            } catch (NullPointerException e) {
                flag = -2;
            }
        } catch (Throwable e) {
            GraalDirectives.deoptimize();
        }
        return flag;
    }

    @Test
    public void testNestedExceptionHandler() {
        test(new OptionValues(getInitialOptions(), HighTier.Options.Inline, false), "nestedExceptionHandler");
    }

    public static String snippet1() {
        try {
            synchronized (String.class) {
                try (AutoCloseable _ = null) {
                    return "RETURN";
                } catch (Throwable t) {
                    return t.toString();
                }
            }
        } finally {
            raise();
        }
    }

    public static void raise() {
        throw new RuntimeException();
    }

    public static String snippet2() {
        try {
            synchronized (String.class) {
                try (AutoCloseable _ = null) {
                    return performCompilation();
                } catch (Throwable t) {
                    return t.toString();
                }
            }
        } finally {
            synchronized (String.class) {
                String.class.toString();
            }
        }
    }

    private static String performCompilation() {
        return "passed";
    }

    @Ignore("https://bugs.eclipse.org/bugs/show_bug.cgi?id=533187")
    @Test
    public void testSnippet1() {
        OptionValues options = parseAllCodeWithoutInlining();
        ResolvedJavaMethod method = getResolvedJavaMethod("snippet1");
        parseEager(method, AllowAssumptions.YES, options);
    }

    @Ignore("https://bugs.eclipse.org/bugs/show_bug.cgi?id=533187")
    @Test
    public void testSnippet2() {
        OptionValues options = parseAllCodeWithoutInlining();
        ResolvedJavaMethod method = getResolvedJavaMethod("snippet2");
        parseEager(method, AllowAssumptions.YES, options);
    }

    private static OptionValues parseAllCodeWithoutInlining() {
        OptionValues options = new OptionValues(getInitialOptions(),
                        UseTypeCheckHints, false,
                        UseExceptionProbability, false,
                        RemoveNeverExecutedCode, false);
        return options;
    }
}
