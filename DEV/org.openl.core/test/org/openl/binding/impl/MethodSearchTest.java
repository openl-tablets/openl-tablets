package org.openl.binding.impl;

import java.io.Serializable;

import org.junit.Test;

public class MethodSearchTest extends AbstractMethodSearchTest {

    @Test
    public void testMethodChoosing() {
        assertInvoke("M1", ClassWithMethods.class, "method1", int.class, double.class);
        assertInvoke("M1", ClassWithMethods.class, "method1", int.class, int.class);
        assertInvoke("M2", ClassWithMethods.class, "method1", byte.class, byte.class);
        assertInvoke("M3", ClassWithMethods.class, "method2", byte.class, byte.class);
        assertNotFound(ClassWithMethods.class, "method3", int.class, double.class);

        assertInvoke("M4", SecondClassWithMethods.class, "method1", int.class, double.class);
        assertInvoke("M4", SecondClassWithMethods.class, "method1", int.class, int.class);
        assertInvoke("M2", SecondClassWithMethods.class, "method1", byte.class, byte.class);
        assertInvoke("M3", SecondClassWithMethods.class, "method2", byte.class, byte.class);
        assertInvoke("M5", SecondClassWithMethods.class, "method3", int.class, double.class);

        assertInvoke("M4", ThirdClassWithMethods.class, "method1", int.class, double.class);
        assertInvoke("M4", ThirdClassWithMethods.class, "method1", int.class, int.class);
        assertInvoke("M2", ThirdClassWithMethods.class, "method1", byte.class, byte.class);
        assertInvoke("M3", ThirdClassWithMethods.class, "method2", byte.class, byte.class);
        assertInvoke("M5", ThirdClassWithMethods.class, "method3", int.class, double.class);
    }

    @Test
    public void testMethodChoosingWithGenerics() {
        assertInvoke("M6", ClassWithGenerics.class, "method1", String.class, String.class);
        assertInvoke("M6", ClassWithGenerics.class, "method1", int.class, short.class);
        assertInvoke("M6", ClassWithGenerics.class, "method1", Byte.class, Long.class);
        assertInvoke("M6", ClassWithGenerics.class, "method1", Double.class, short.class);
        assertNotFound(ClassWithGenerics.class, "method1", Integer.class, String.class);

        assertInvoke("String", ClassWithGenerics.class, "method2", String.class, String.class);
        assertInvoke("Integer", ClassWithGenerics.class, "method2", short.class, int.class);
        assertInvoke("Long", ClassWithGenerics.class, "method2", Byte.class, Long.class);
        assertInvoke("Double", ClassWithGenerics.class, "method2", Double.class, short.class);

        Object t = new Object();
        assertInvoke(t, ClassWithGenerics.class, "copy", new Class<?>[] { Object.class }, new Object[] { t });
        Double[] d = new Double[] {};
        assertInvoke(d, ClassWithGenerics.class, "copy", new Class<?>[] { Double[].class }, new Object[] { d });
    }

    @Test
    public void testMethodChoosingWithNulls() {
        assertAmbigiouse(ForthClassWithMethods.class, "method1", null, null);
        assertInvoke("M8", ForthClassWithMethods.class, "method1", int.class, null);
        assertInvoke("M9", ForthClassWithMethods.class, "method1", String.class, null);
        assertAmbigiouse(ForthClassWithMethods.class, "method2", null, null);
        assertInvoke("M12", ForthClassWithMethods.class, "method3", null, null, null);
    }

    public static class ClassWithMethods {
        public String method1(int arg1, double arg2) {
            return "M1";
        };

        public String method1(int arg1, byte arg2) {
            return "M2";
        };

        public String method2(int arg1, double arg2) {
            return "M3";
        };
    }

    public static class SecondClassWithMethods extends ClassWithMethods implements Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public String method1(int arg1, double arg2) {
            return "M4";
        };

        public String method3(int arg1, double arg2) {
            return "M5";
        };
    }

    public static class ClassWithGenerics {
        public <T> String method1(T arg1, T arg2) {
            return "M6";
        };

        public <T> String method2(T arg1, T arg2) {
            return arg1.getClass().getSimpleName();
        };

        public <T> T copy(T t) {
            return t;
        }

        public <T extends Serializable> T copy(T t) {
            return t;
        }
    }

    public static class ThirdClassWithMethods extends SecondClassWithMethods {
        private static final long serialVersionUID = 1L;
    }

    public static class ForthClassWithMethods {
        public <T> String method1(int arg1, int arg2) {
            return "M7";
        };

        public <T> String method1(T arg1, T arg2) {
            return "M8";
        };

        public String method1(String arg1, String arg2) {
            return "M9";
        };

        public String method2(String[] arg1) {
            return "M10";
        };

        public String method2(Integer[] arg1) {
            return "M11";
        };

        public String method3(String[] arg1) {
            return "M12";
        };

    }
}
