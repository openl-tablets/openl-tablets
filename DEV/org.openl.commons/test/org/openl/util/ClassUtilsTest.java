package org.openl.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.Test;

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

    static class InnerClass {
    }

    @Test
    public void testIsAssignable() {
        assertTrue(ClassUtils.isAssignable(null, null));

        assertTrue(ClassUtils.isAssignable(Integer.class, null));
        assertTrue(ClassUtils.isAssignable(null, Integer.class));
        assertFalse(ClassUtils.isAssignable(int.class, null));
        assertFalse(ClassUtils.isAssignable(null, int.class));

        assertTrue(ClassUtils.isAssignable(Integer.class, int.class));
        assertTrue(ClassUtils.isAssignable(int.class, Integer.class));
        assertTrue(ClassUtils.isAssignable(Integer.class, Number.class));
        assertFalse(ClassUtils.isAssignable(Number.class, Integer.class));

        assertTrue(ClassUtils.isAssignable(int.class, double.class));
        assertFalse(ClassUtils.isAssignable(int.class, Double.class));
        assertFalse(ClassUtils.isAssignable(double.class, int.class));
        assertFalse(ClassUtils.isAssignable(Double.class, int.class));

        assertTrue(ClassUtils.isAssignable(Integer.class, double.class));
        assertFalse(ClassUtils.isAssignable(Integer.class, Double.class));
        assertFalse(ClassUtils.isAssignable(double.class, Integer.class));
        assertFalse(ClassUtils.isAssignable(Double.class, Integer.class));
    }

    @Test
    public void testSet() throws Exception {
        assertEquals("value", setFieldInBean("a", "value").a);
        assertEquals("value", setFieldInBean("c", "value").c);
        assertEquals(10, setFieldInBean("x", 10).e);
        assertEquals(17.3, setFieldInBean("x", 17.3).e);
        assertNull(setFieldInBean("x", null).e);
        assertEquals(10, setFieldInBean("y", 10).e);
        assertEquals(3.1415, setFieldInBean("y", 3.1415).e);
        assertEquals(10.0, setFieldInBean("z", 10).e);
        assertEquals(3.1415, setFieldInBean("z", 3.1415).e);
        assertNull(setFieldInBean("y", null).e);
        assertEquals(10, setFieldInBean("i", 10).i);

        assertThrows(IllegalAccessException.class, () -> {
            setFieldInBean("b", "value");
        });
        assertThrows(IllegalAccessException.class, () -> {
            setFieldInBean("d", "value");
        });
        assertThrows(IllegalAccessException.class, () -> {
            setFieldInBean("e", "value");
        });
        assertThrows(IllegalAccessException.class, () -> {
            setFieldInBean("f", "value");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            setFieldInBean("a", 10);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            setFieldInBean("c", 10);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            setFieldInBean("i", "10");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            setFieldInBean("i", null);
        });
        assertThrows(UnsupportedOperationException.class, () -> {
            setFieldInBean("x", "not a number");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            setFieldInBean("y", "not a number");
        });
        assertThrows(UnsupportedOperationException.class, () -> {
            setFieldInBean("z", null);
        });
    }

    @Test
    public void testGet() throws Exception {
        assertEquals("getter", getFieldInBean("a"));
        assertThrows(IllegalAccessException.class, () -> {
            getFieldInBean("b");
        });
        assertEquals("public", getFieldInBean("c"));
        assertEquals("final", getFieldInBean("d"));
        assertThrows(IllegalAccessException.class, () -> {
            getFieldInBean("e");
        });
        assertThrows(IllegalAccessException.class, () -> {
            getFieldInBean("f");
        });
        assertEquals(10, getFieldInBean("x"));
        assertThrows(IllegalAccessException.class, () -> {
            getFieldInBean("y");
        });
        assertThrows(IllegalAccessException.class, () -> {
            getFieldInBean("z");
        });
    }

    @Test
    public void testGetType() throws Exception {
        var bean = new Bean();
        assertEquals(String.class, ClassUtils.getType(bean, "a"));
        assertEquals(String.class, ClassUtils.getType(bean, "b"));
        assertEquals(String.class, ClassUtils.getType(bean, "c"));
        assertEquals(String.class, ClassUtils.getType(bean, "d"));
        assertEquals(Number.class, ClassUtils.getType(bean, "e"));
        assertNull(ClassUtils.getType(bean, "f"));
        assertEquals(int.class, ClassUtils.getType(bean, "i"));
        assertEquals(Number.class, ClassUtils.getType(bean, "x"));
        assertNull(ClassUtils.getType(bean, "y"));
        assertNull(ClassUtils.getType(bean, "z"));
    }

    private static Bean setFieldInBean(String fieldName, Object value) throws Exception {
        var bean = new Bean();
        ClassUtils.set(bean, fieldName, value);
        return bean;
    }

    private static Object getFieldInBean(String fieldName) throws Exception {
        var bean = new Bean();
        return ClassUtils.get(bean, fieldName);
    }

    public static class Bean {
        private String a = "getter";
        protected String b = "protected";
        public String c = "public";
        public final String d = "final";
        private Number e = 10;
        public int i;

        public void setA(String a) {
            this.a = a;
        }
        public String getA() {
            return a;
        }

        public void setX(Number e) {
            this.e = e;
        }
        public Number getX() {
            return e;
        }

        public void setX(Object e) {
            throw new UnsupportedOperationException();
        }

        public void setY(Number e) {
            this.e = e;
        }

        public void setZ(double e) {
            this.e = e;
        }

        public void setZ(Object e) {
            throw new UnsupportedOperationException();
        }
    }
}