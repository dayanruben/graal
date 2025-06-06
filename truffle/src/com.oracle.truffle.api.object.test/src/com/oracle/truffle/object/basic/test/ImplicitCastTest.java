/*
 * Copyright (c) 2016, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.object.basic.test;

import static com.oracle.truffle.object.basic.test.DOTestAsserts.getLocationType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Location;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.test.AbstractParametrizedLibraryTest;

@SuppressWarnings("deprecation")
@RunWith(Parameterized.class)
public class ImplicitCastTest extends AbstractParametrizedLibraryTest {

    @Parameters
    public static Collection<Object[]> data() {
        List<Object[]> params = new ArrayList<>();

        for (TestRun run : TestRun.values()) {
            params.add(new Object[]{run, 1, 1L << 42, long.class});
            params.add(new Object[]{run, 1, 3.14, double.class});
        }

        return Collections.unmodifiableList(params);
    }

    @Parameter(1) public int intVal;
    @Parameter(2) public Object otherVal;
    @Parameter(3) public Class<?> otherPrimClass;

    private DynamicObject newInstanceWithImplicitCast() {
        Shape.Builder b = Shape.newBuilder();
        b.allowImplicitCastIntToLong(otherPrimClass == long.class);
        b.allowImplicitCastIntToDouble(otherPrimClass == double.class);
        Shape rootShape = b.build();
        return new TestDynamicObjectDefault(rootShape);
    }

    private static DynamicObject newInstance() {
        Shape rootShape = Shape.newBuilder().build();
        return new TestDynamicObjectDefault(rootShape);
    }

    @Test
    public void testIntOther() {
        DynamicObject object = newInstanceWithImplicitCast();

        DynamicObjectLibrary library = createLibrary(DynamicObjectLibrary.class, object);

        library.put(object, "a", intVal);
        Location location1 = object.getShape().getProperty("a").getLocation();
        Assert.assertEquals(int.class, getLocationType(location1));

        library.put(object, "a", otherVal);
        Location location2 = object.getShape().getProperty("a").getLocation();
        Assert.assertEquals(otherPrimClass, getLocationType(location2));
        Assert.assertEquals(otherVal.getClass(), library.getOrDefault(object, "a", null).getClass());
        DOTestAsserts.assertSameUnderlyingLocation(location1, location2);
    }

    @Test
    public void testOtherInt() {
        DynamicObject object = newInstanceWithImplicitCast();

        DynamicObjectLibrary library = createLibrary(DynamicObjectLibrary.class, object);

        library.put(object, "a", otherVal);
        Location location1 = object.getShape().getProperty("a").getLocation();
        Assert.assertEquals(otherPrimClass, getLocationType(location1));

        library.put(object, "a", intVal);
        Location location2 = object.getShape().getProperty("a").getLocation();
        Assert.assertEquals(otherPrimClass, getLocationType(location2));
        Assert.assertEquals(otherVal.getClass(), library.getOrDefault(object, "a", null).getClass());
        DOTestAsserts.assertSameUnderlyingLocation(location1, location2);
    }

    @Test
    public void testIntOtherDoesNotGoBack() {
        DynamicObject object = newInstanceWithImplicitCast();

        DynamicObjectLibrary library = createLibrary(DynamicObjectLibrary.class, object);

        library.put(object, "a", intVal);
        Location location1 = object.getShape().getProperty("a").getLocation();
        Assert.assertEquals(int.class, getLocationType(location1));

        library.put(object, "a", otherVal);
        Location location2 = object.getShape().getProperty("a").getLocation();
        Assert.assertEquals(otherPrimClass, getLocationType(location2));
        Assert.assertEquals(otherVal.getClass(), library.getOrDefault(object, "a", null).getClass());
        DOTestAsserts.assertSameUnderlyingLocation(location1, location2);

        library.put(object, "a", intVal);
        Location location3 = object.getShape().getProperty("a").getLocation();
        Assert.assertEquals(otherPrimClass, getLocationType(location3));
        Assert.assertEquals(otherVal.getClass(), library.getOrDefault(object, "a", null).getClass());
        DOTestAsserts.assertSameUnderlyingLocation(location2, location3);
    }

    @Test
    public void testIntObject() {
        DynamicObject object = newInstanceWithImplicitCast();

        DynamicObjectLibrary library = createLibrary(DynamicObjectLibrary.class, object);

        library.put(object, "a", intVal);
        library.put(object, "a", "");
        Location location = object.getShape().getProperty("a").getLocation();
        Assert.assertEquals(Object.class, getLocationType(location));
        Assert.assertEquals(String.class, library.getOrDefault(object, "a", null).getClass());
    }

    @Test
    public void testIntOtherObject() {
        DynamicObject object = newInstanceWithImplicitCast();

        DynamicObjectLibrary library = createLibrary(DynamicObjectLibrary.class, object);

        library.put(object, "a", intVal);
        library.put(object, "a", otherVal);
        library.put(object, "a", "");
        Location location = object.getShape().getProperty("a").getLocation();
        Assert.assertEquals(Object.class, getLocationType(location));
        Assert.assertEquals(String.class, library.getOrDefault(object, "a", null).getClass());
    }

    @Test
    public void testLocationDecoratorEquals() {
        DynamicObject object1 = newInstanceWithImplicitCast();

        DynamicObjectLibrary library = createLibrary(DynamicObjectLibrary.class, object1);

        library.put(object1, "a", otherVal);
        Location location1 = object1.getShape().getProperty("a").getLocation();

        // Location of "a" should not change if an Integer is set
        library.putIfPresent(object1, "a", intVal);
        Assert.assertEquals(location1, object1.getShape().getProperty("a").getLocation());

        DynamicObject object2 = newInstance();
        library.put(object2, "a", otherVal);
        Location location2 = object2.getShape().getProperty("a").getLocation();

        // This test relies on the assumption that both locations are of the same class
        Assert.assertEquals(location1.getClass(), location2.getClass());
        Assert.assertNotEquals(location1, location2);
    }
}
