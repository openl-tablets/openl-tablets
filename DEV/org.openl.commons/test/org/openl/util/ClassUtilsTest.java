package org.openl.util;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;

/**
 * Created by ymolchan on 12.10.2015.
 */
public class ClassUtilsTest {

    @Test
    public void testGetPackageName() {
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
    public void testGetShortClassName() {
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

    @Test
    public void testCapitalize() {
        assertEquals("A", ClassUtils.capitalize("a"));
        assertEquals("A", ClassUtils.capitalize("A"));

        assertEquals("Aa", ClassUtils.capitalize("aa"));
        assertEquals("aA", ClassUtils.capitalize("aA"));
        assertEquals("Aa", ClassUtils.capitalize("Aa"));
        assertEquals("AA", ClassUtils.capitalize("AA"));

        assertEquals("Bbb", ClassUtils.capitalize("bbb"));
        assertEquals("BbB", ClassUtils.capitalize("bbB"));
        assertEquals("bBb", ClassUtils.capitalize("bBb"));
        assertEquals("bBB", ClassUtils.capitalize("bBB"));
        assertEquals("Bbb", ClassUtils.capitalize("Bbb"));
        assertEquals("BbB", ClassUtils.capitalize("BbB"));
        assertEquals("BBb", ClassUtils.capitalize("BBb"));
        assertEquals("BBB", ClassUtils.capitalize("BBB"));
    }

    @Test
    public void testDecapitalize() {
        assertEquals("a", ClassUtils.decapitalize("a"));
        assertEquals("a", ClassUtils.decapitalize("A"));

        assertEquals("aa", ClassUtils.decapitalize("aa"));
        assertEquals("aA", ClassUtils.decapitalize("aA"));
        assertEquals("aa", ClassUtils.decapitalize("Aa"));
        assertEquals("AA", ClassUtils.decapitalize("AA"));

        assertEquals("bbb", ClassUtils.decapitalize("bbb"));
        assertEquals("bbB", ClassUtils.decapitalize("bbB"));
        assertEquals("bBb", ClassUtils.decapitalize("bBb"));
        assertEquals("bBB", ClassUtils.decapitalize("bBB"));
        assertEquals("bbb", ClassUtils.decapitalize("Bbb"));
        assertEquals("bbB", ClassUtils.decapitalize("BbB"));
        assertEquals("BBb", ClassUtils.decapitalize("BBb"));
        assertEquals("BBB", ClassUtils.decapitalize("BBB"));
    }

    @Test
    public void testGetter() {
        assertEquals("getA", ClassUtils.getter("a"));
        assertEquals("getA", ClassUtils.getter("A"));

        assertEquals("getAa", ClassUtils.getter("aa"));
        assertEquals("getaA", ClassUtils.getter("aA"));
        assertEquals("getAa", ClassUtils.getter("Aa"));
        assertEquals("getAA", ClassUtils.getter("AA"));

        assertEquals("getBbb", ClassUtils.getter("bbb"));
        assertEquals("getBbB", ClassUtils.getter("bbB"));
        assertEquals("getbBb", ClassUtils.getter("bBb"));
        assertEquals("getbBB", ClassUtils.getter("bBB"));
        assertEquals("getBbb", ClassUtils.getter("Bbb"));
        assertEquals("getBbB", ClassUtils.getter("BbB"));
        assertEquals("getBBb", ClassUtils.getter("BBb"));
        assertEquals("getBBB", ClassUtils.getter("BBB"));
    }

    @Test
    public void testSetter() {
        assertEquals("setA", ClassUtils.setter("a"));
        assertEquals("setA", ClassUtils.setter("A"));

        assertEquals("setAa", ClassUtils.setter("aa"));
        assertEquals("setaA", ClassUtils.setter("aA"));
        assertEquals("setAa", ClassUtils.setter("Aa"));
        assertEquals("setAA", ClassUtils.setter("AA"));

        assertEquals("setBbb", ClassUtils.setter("bbb"));
        assertEquals("setBbB", ClassUtils.setter("bbB"));
        assertEquals("setbBb", ClassUtils.setter("bBb"));
        assertEquals("setbBB", ClassUtils.setter("bBB"));
        assertEquals("setBbb", ClassUtils.setter("Bbb"));
        assertEquals("setBbB", ClassUtils.setter("BbB"));
        assertEquals("setBBb", ClassUtils.setter("BBb"));
        assertEquals("setBBB", ClassUtils.setter("BBB"));
    }

    class InnerClass {
    }
}