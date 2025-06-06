/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
package jdk.graal.compiler.libgraal.truffle;

import com.oracle.truffle.compiler.TruffleCompilable;
import com.oracle.truffle.compiler.TruffleCompilationTask;
import com.oracle.truffle.compiler.TruffleCompilerListener;
import com.oracle.truffle.compiler.hotspot.libgraal.TruffleFromLibGraal;
import org.graalvm.jniutils.HSObject;
import org.graalvm.jniutils.JNI.JNIEnv;
import org.graalvm.jniutils.JNI.JObject;
import org.graalvm.jniutils.JNI.JString;
import org.graalvm.jniutils.JNIMethodScope;
import org.graalvm.jniutils.JNIUtil;

import java.util.function.Supplier;

import static com.oracle.truffle.compiler.hotspot.libgraal.TruffleFromLibGraal.Id.OnCompilationRetry;
import static com.oracle.truffle.compiler.hotspot.libgraal.TruffleFromLibGraal.Id.OnFailure;
import static com.oracle.truffle.compiler.hotspot.libgraal.TruffleFromLibGraal.Id.OnGraalTierFinished;
import static com.oracle.truffle.compiler.hotspot.libgraal.TruffleFromLibGraal.Id.OnSuccess;
import static com.oracle.truffle.compiler.hotspot.libgraal.TruffleFromLibGraal.Id.OnTruffleTierFinished;

final class HSTruffleCompilerListener extends HSObject implements TruffleCompilerListener {

    private final TruffleFromLibGraalCalls calls;

    HSTruffleCompilerListener(JNIMethodScope scope, JObject handle, HSTruffleCompilerRuntime runtime) {
        super(scope, handle);
        this.calls = runtime.calls;
    }

    @TruffleFromLibGraal(OnSuccess)
    @Override
    public void onSuccess(TruffleCompilable compilable, TruffleCompilationTask task, GraphInfo graphInfo, CompilationResultInfo compilationResultInfo, int tier) {
        JObject hsCompilable = ((HSTruffleCompilable) compilable).getHandle();
        JObject hsTask = ((HSTruffleCompilationTask) task).getHandle();
        JNIEnv env = JNIMethodScope.env();
        try (LibGraalObjectHandleScope graphInfoScope = LibGraalObjectHandleScope.forObject(graphInfo);
                        LibGraalObjectHandleScope compilationResultInfoScope = LibGraalObjectHandleScope.forObject(compilationResultInfo)) {
            HSTruffleCompilerListenerGen.callOnSuccess(calls, env, getHandle(), hsCompilable, hsTask, graphInfoScope.getHandle(), compilationResultInfoScope.getHandle(), tier);
        }
    }

    @TruffleFromLibGraal(OnTruffleTierFinished)
    @Override
    public void onTruffleTierFinished(TruffleCompilable compilable, TruffleCompilationTask task, GraphInfo graph) {
        JObject hsCompilable = ((HSTruffleCompilable) compilable).getHandle();
        JObject hsTask = ((HSTruffleCompilationTask) task).getHandle();
        JNIEnv env = JNIMethodScope.env();
        try (LibGraalObjectHandleScope graphInfoScope = LibGraalObjectHandleScope.forObject(graph)) {
            HSTruffleCompilerListenerGen.callOnTruffleTierFinished(calls, env, getHandle(), hsCompilable, hsTask, graphInfoScope.getHandle());
        }
    }

    @TruffleFromLibGraal(OnGraalTierFinished)
    @Override
    public void onGraalTierFinished(TruffleCompilable compilable, GraphInfo graph) {
        JObject hsCompilable = ((HSTruffleCompilable) compilable).getHandle();
        JNIEnv env = JNIMethodScope.env();
        try (LibGraalObjectHandleScope graphInfoScope = LibGraalObjectHandleScope.forObject(graph)) {
            HSTruffleCompilerListenerGen.callOnGraalTierFinished(calls, env, getHandle(), hsCompilable, graphInfoScope.getHandle());
        }
    }

    @TruffleFromLibGraal(OnFailure)
    @Override
    public void onFailure(TruffleCompilable compilable, String reason, boolean bailout, boolean permanentBailout, int tier, Supplier<String> lazyStackTrace) {
        try (LibGraalObjectHandleScope lazyStackTraceScope = lazyStackTrace != null ? LibGraalObjectHandleScope.forObject(lazyStackTrace) : null) {
            JObject hsCompilable = ((HSTruffleCompilable) compilable).getHandle();
            JNIEnv env = JNIMethodScope.env();
            JString hsReason = JNIUtil.createHSString(env, reason);
            HSTruffleCompilerListenerGen.callOnFailure(calls, env, getHandle(), hsCompilable, hsReason, bailout, permanentBailout, tier,
                            lazyStackTraceScope != null ? lazyStackTraceScope.getHandle() : 0L);
        }
    }

    @TruffleFromLibGraal(OnCompilationRetry)
    @Override
    public void onCompilationRetry(TruffleCompilable compilable, TruffleCompilationTask task) {
        JObject hsCompilable = ((HSTruffleCompilable) compilable).getHandle();
        JObject hsTask = ((HSTruffleCompilationTask) task).getHandle();
        JNIEnv env = JNIMethodScope.env();
        HSTruffleCompilerListenerGen.callOnCompilationRetry(calls, env, getHandle(), hsCompilable, hsTask);
    }
}
