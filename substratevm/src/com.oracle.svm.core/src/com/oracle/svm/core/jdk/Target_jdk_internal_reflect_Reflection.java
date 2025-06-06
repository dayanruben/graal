/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.svm.core.jdk;

import org.graalvm.nativeimage.Platforms;
import org.graalvm.nativeimage.impl.InternalPlatform;

import com.oracle.svm.core.NeverInline;
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.hub.DynamicHub;
import com.oracle.svm.core.snippets.KnownIntrinsics;

@TargetClass(value = jdk.internal.reflect.Reflection.class)
final class Target_jdk_internal_reflect_Reflection {

    @Substitute
    @NeverInline("Starting a stack walk in the caller frame")
    @Platforms(InternalPlatform.NATIVE_ONLY.class)
    private static Class<?> getCallerClass() {
        return StackTraceUtils.getCallerClass(KnownIntrinsics.readCallerStackPointer(), true);
    }

    @Substitute
    private static int getClassAccessFlags(DynamicHub cls) {
        return cls.getClassAccessFlags();
    }

    @Substitute
    private static boolean areNestMates(Class<?> currentClass, Class<?> memberClass) {
        return DynamicHub.fromClass(currentClass).isNestmateOf(memberClass);
    }

    @Alias
    public static native void ensureNativeAccess(Class<?> currentClass, Class<?> owner, String methodName, boolean jni);
}
