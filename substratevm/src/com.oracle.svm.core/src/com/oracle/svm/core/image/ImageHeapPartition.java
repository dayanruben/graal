/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.svm.core.image;

/**
 * A native image heap consists of multiple non-overlapping {@link ImageHeapPartition}s. Every
 * object in the native image heap is assigned to a position within a {@link ImageHeapPartition}.
 */
public interface ImageHeapPartition {
    /** Returns the name of the partition. */
    String getName();

    /** Returns the offset at which this partition is allocated. */
    long getStartOffset();

    /**
     * Returns the size of the partition (i.e., the sum of all allocated objects + some overhead).
     */
    long getSize();

    /* Returns true if this partition is only used as a filler. */
    boolean isFiller();
}
