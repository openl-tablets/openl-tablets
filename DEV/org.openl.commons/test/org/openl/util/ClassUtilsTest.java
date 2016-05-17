package org.openl.util;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;

/**
 * Created by ymolchan on 12.10.2015.
 */
public class ClassUtilsTest {

    @Test
    public void testGetPackageName() throws Exception {
        assertEquals("", ClassUtils.getPackageName(null));
        assertEquals("", ClassUtils.getPackageName(double.class));
        assertEquals("java.lang", ClassUtils.getPackageName(Double.class));
        assertEquals("", ClassUtils.getPackageName(double[].class));
        assertEquals("java.lang", ClassUtils.getPackageName(Double[].class));
        assertEquals("java.util", ClassUtils.getPackageName(Map.Entry.class));
        assertEquals("java.util", ClassUtils.getPackageName(Map.Entry[][].class));
        assertEquals("org.openl.util", ClassUtils.getPackageName(InnerClass.class));
        assertEquals("org.openl.util", ClassUtils.getPackageName(InnerClass[][][].class));
    }

    @Test
    public void testGetShortClassName() throws Exception {
        assertEquals("", ClassUtils.getShortClassName(null));
        assertEquals("double", ClassUtils.getShortClassName(double.class));
        assertEquals("Double", ClassUtils.getShortClassName(Double.class));
        assertEquals("double[]", ClassUtils.getShortClassName(double[].class));
        assertEquals("Double[]", ClassUtils.getShortClassName(Double[].class));
        assertEquals("Map.Entry", ClassUtils.getShortClassName(Map.Entry.class));
        assertEquals("Map.Entry[][]", ClassUtils.getShortClassName(Map.Entry[][].class));
        assertEquals("ClassUtilsTest.InnerClass", ClassUtils.getShortClassName(InnerClass.class));
        assertEquals("ClassUtilsTest.InnerClass[][][]", ClassUtils.getShortClassName(InnerClass[][][].class));
    }

    class InnerClass {
    }
}