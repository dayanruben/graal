/*
 * Copyright (c) 2015, 2022, Oracle and/or its affiliates. All rights reserved.
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
package jdk.graal.compiler.lir.phases;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import jdk.graal.compiler.core.common.LibGraalSupport;
import jdk.graal.compiler.core.common.util.PhasePlan;
import jdk.graal.compiler.debug.DebugCloseable;
import jdk.graal.compiler.debug.DebugContext;
import jdk.graal.compiler.debug.TimerKey;
import jdk.graal.compiler.lir.gen.LIRGenerationResult;
import jdk.vm.ci.code.TargetDescription;

public class LIRPhaseSuite<C> extends LIRPhase<C> implements PhasePlan<LIRPhase<C>> {
    private List<LIRPhase<C>> phases;

    public LIRPhaseSuite() {
        phases = new ArrayList<>();
    }

    /**
     * Gets an unmodifiable view on the phases in this suite.
     */
    @Override
    public List<LIRPhase<C>> getPhases() {
        return Collections.unmodifiableList(phases);
    }

    /**
     * Add a new phase at the beginning of this suite.
     */
    public final void prependPhase(LIRPhase<C> phase) {
        phases.add(0, phase);
    }

    /**
     * Add a new phase at the end of this suite.
     */
    public final void appendPhase(LIRPhase<C> phase) {
        phases.add(phase);
    }

    public final ListIterator<LIRPhase<C>> findPhase(Class<? extends LIRPhase<C>> phaseClass) {
        ListIterator<LIRPhase<C>> it = phases.listIterator();
        if (findNextPhase(it, phaseClass)) {
            return it;
        } else {
            return null;
        }
    }

    public final <T extends LIRPhase<C>> T findPhaseInstance(Class<T> phaseClass) {
        ListIterator<LIRPhase<C>> it = phases.listIterator();
        while (it.hasNext()) {
            LIRPhase<C> phase = it.next();
            if (phaseClass.isInstance(phase)) {
                return phaseClass.cast(phase);
            }
        }
        return null;
    }

    public static <C> boolean findNextPhase(ListIterator<LIRPhase<C>> it, Class<? extends LIRPhase<C>> phaseClass) {
        while (it.hasNext()) {
            LIRPhase<C> phase = it.next();
            if (phaseClass.isInstance(phase)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Time spent in hinted GC in backend.
     */
    public static final TimerKey LIRHintedGC = DebugContext.timer("LIRHintedGC").doc("Time spent in hinted GC performed before each LIR phase.");

    @SuppressWarnings({"try"})
    @Override
    protected final void run(TargetDescription target, LIRGenerationResult lirGenRes, C context) {
        for (LIRPhase<C> phase : phases) {
            LibGraalSupport libgraal = LibGraalSupport.INSTANCE;
            if (libgraal != null) {
                /*
                 * Notify libgraal runtime that most objects allocated in previous LIR phase are
                 * dead and can be reclaimed. This will lower the chance of allocation failure in
                 * the next LIR phase.
                 */
                try (DebugCloseable timer = LIRHintedGC.start(lirGenRes.getLIR().getDebug())) {
                    libgraal.notifyLowMemoryPoint(false);
                    libgraal.processReferences();
                }
            }
            phase.apply(target, lirGenRes, context);
        }
    }

    public LIRPhaseSuite<C> copy() {
        LIRPhaseSuite<C> suite = new LIRPhaseSuite<>();
        suite.phases.addAll(phases);
        return suite;
    }

    @Override
    public String toString() {
        return String.format("%s:%n%s", getClass().getSimpleName(), new PhasePlan.Printer().toString(this));
    }
}
