/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.svm.core.jdk.management;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryManagerMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.PlatformManagedObject;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServerBuilder;
import javax.management.openmbean.OpenType;

import org.graalvm.nativeimage.ImageSingletons;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeClassInitialization;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

import com.oracle.svm.core.feature.AutomaticallyRegisteredFeature;
import com.oracle.svm.core.feature.InternalFeature;
import com.oracle.svm.core.jdk.JNIRegistrationUtil;
import com.oracle.svm.core.jdk.RuntimeSupportFeature;
import com.oracle.svm.core.thread.ThreadListenerSupport;
import com.oracle.svm.core.thread.ThreadListenerSupportFeature;
import com.oracle.svm.util.ReflectionUtil;

/** See {@link ManagementSupport} for documentation. */
@AutomaticallyRegisteredFeature
public final class ManagementFeature extends JNIRegistrationUtil implements InternalFeature {
    private Map<PlatformManagedObject, PlatformManagedObject> platformManagedObjectReplacements;

    @Override
    public List<Class<? extends Feature>> getRequiredFeatures() {
        return Arrays.asList(RuntimeSupportFeature.class, ThreadListenerSupportFeature.class);
    }

    @Override
    public void afterRegistration(AfterRegistrationAccess access) {
        SubstrateRuntimeMXBean runtimeMXBean = new SubstrateRuntimeMXBean();
        ImageSingletons.add(SubstrateRuntimeMXBean.class, runtimeMXBean);

        SubstrateThreadMXBean threadMXBean = new SubstrateThreadMXBean();
        ImageSingletons.add(SubstrateThreadMXBean.class, threadMXBean);

        ManagementSupport managementSupport = new ManagementSupport(runtimeMXBean, threadMXBean);
        ImageSingletons.add(ManagementSupport.class, managementSupport);
        ThreadListenerSupport.get().register(managementSupport);
    }

    @Override
    public void duringSetup(DuringSetupAccess access) {
        platformManagedObjectReplacements = new IdentityHashMap<>();
        for (Class<? extends PlatformManagedObject> clazz : Arrays.asList(ClassLoadingMXBean.class, CompilationMXBean.class, RuntimeMXBean.class,
                        ThreadMXBean.class, OperatingSystemMXBean.class, MemoryMXBean.class)) {
            PlatformManagedObject source = ManagementFactory.getPlatformMXBean(clazz);
            PlatformManagedObject target = ManagementSupport.getSingleton().getPlatformMXBeanRaw(clazz);
            if (source != null && target != null) {
                platformManagedObjectReplacements.put(source, target);
            }
        }
        access.registerObjectReplacer(this::replaceHostedPlatformManagedObject);

        RuntimeClassInitialization.initializeAtBuildTime("com.sun.jmx.mbeanserver.DefaultMXBeanMappingFactory");
        RuntimeClassInitialization.initializeAtBuildTime("com.sun.jmx.mbeanserver.DefaultMXBeanMappingFactory$Mappings");
        RuntimeClassInitialization.initializeAtBuildTime("com.sun.jmx.mbeanserver.DefaultMXBeanMappingFactory$IdentityMapping");
        RuntimeClassInitialization.initializeAtBuildTime("com.sun.jmx.mbeanserver.DescriptorCache");
        RuntimeClassInitialization.initializeAtBuildTime("com.sun.jmx.remote.util.ClassLogger");

        RuntimeClassInitialization.initializeAtRunTime("sun.management.MemoryImpl");
        RuntimeClassInitialization.initializeAtRunTime("com.sun.management.internal.PlatformMBeanProviderImpl");
    }

    /**
     * PlatformManagedObject are often caches in static final fields of application classes.
     * Replacing the hosted objects with the proper runtime objects allows these application classes
     * to be initialized at image build time. Note that only singleton beans can be automatically
     * replaced, beans that have a list (like {@link GarbageCollectorMXBean} cannot be replaced
     * automatically.
     */
    private Object replaceHostedPlatformManagedObject(Object source) {
        if (source instanceof PlatformManagedObject) {
            Object replacement = platformManagedObjectReplacements.get(source);
            if (replacement != null) {
                return replacement;
            }
        }
        return source;
    }

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        access.registerReachabilityHandler(ManagementFeature::registerMBeanServerFactoryNewBuilder, method(access, "javax.management.MBeanServerFactory", "newBuilder", Class.class));
        access.registerReachabilityHandler(ManagementFeature::registerMXBeanMappingMakeOpenClass, method(access, "com.sun.jmx.mbeanserver.MXBeanMapping", "makeOpenClass", Type.class, OpenType.class));

        assert verifyMemoryManagerBeans();
        assert ManagementSupport.getSingleton().verifyNoOverlappingMxBeans();
    }

    private static boolean verifyMemoryManagerBeans() {
        ManagementSupport managementSupport = ManagementSupport.getSingleton();
        List<MemoryPoolMXBean> memoryPools = managementSupport.getPlatformMXBeans(MemoryPoolMXBean.class);
        List<MemoryManagerMXBean> memoryManagers = managementSupport.getPlatformMXBeans(MemoryManagerMXBean.class);

        Set<String> memoryManagerNames = new HashSet<>();
        Set<String> memoryPoolNames = new HashSet<>();
        for (MemoryPoolMXBean memoryPool : memoryPools) {
            String memoryPoolName = memoryPool.getName();
            assert verifyObjectName(memoryPoolName);
            memoryPoolNames.add(memoryPoolName);
        }
        for (MemoryManagerMXBean memoryManager : memoryManagers) {
            String memoryManagerName = memoryManager.getName();
            assert verifyObjectName(memoryManagerName);
            memoryManagerNames.add(memoryManagerName);
            assert memoryPoolNames.containsAll(List.of(memoryManager.getMemoryPoolNames())) : memoryManagerName;
        }
        for (MemoryPoolMXBean memoryPool : memoryPools) {
            assert memoryManagerNames.containsAll(List.of(memoryPool.getMemoryManagerNames())) : memoryPool.getName();
        }
        return true;
    }

    private static boolean verifyObjectName(String name) {
        assert !name.contains(":");
        assert !name.contains("=");
        assert !name.contains("\"");
        assert !name.contains("\n");
        return true;
    }

    private static void registerMBeanServerFactoryNewBuilder(@SuppressWarnings("unused") DuringAnalysisAccess a) {
        /*
         * MBeanServerBuilder is the default builder used when no class is explicitly specified via
         * a system property.
         */
        RuntimeReflection.register(ReflectionUtil.lookupConstructor(MBeanServerBuilder.class));
    }

    private static void registerMXBeanMappingMakeOpenClass(DuringAnalysisAccess access) {
        /*
         * The allowed "open types" are looked up by class name. According to the specification, all
         * array types of arbitrary depth are allowed, but we cannot register all array classes.
         * Registering the one-dimensional array classes capture the common use cases.
         */
        for (String className : OpenType.ALLOWED_CLASSNAMES_LIST) {
            RuntimeReflection.register(clazz(access, className));
            RuntimeReflection.register(clazz(access, "[L" + className + ";"));
        }
    }
}
