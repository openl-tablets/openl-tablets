package org.openl.binding.impl;

import java.io.Serializable;

import org.junit.Test;
import org.openl.binding.exception.AmbiguousMethodException;

public class MethodSearchTest extends AbstractMethodSearchTest {

    @Test
    public void testMethodChoosing() throws AmbiguousMethodException {
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
    public void testMethodChoosingWithGenerics() throws AmbiguousMethodException {
        assertInvoke("M6", ClassWithGenerics.class, "method1", String.class, String.class);
        assertInvoke("M6", ClassWithGenerics.class, "method1", int.class, short.class);
        assertInvoke("M6", ClassWithGenerics.class, "method1", Byte.class, Long.class);
        assertInvoke("M6", ClassWithGenerics.class, "method1", Double.class, short.class);
        assertInvoke("M6", ClassWithGenerics.class, "method1", Integer.class, String.class);

        assertInvoke("String", ClassWithGenerics.class, "method2", String.class, String.class);
        assertInvoke("Integer", ClassWithGenerics.class, "method2", short.class, int.class);
        assertInvoke("Long", ClassWithGenerics.class, "method2", Byte.class, Long.class);
        assertInvoke("Double", ClassWithGenerics.class, "method2", Double.class, short.class);

        // assertNotFound( ClassWithGenerics.class, "method3", byte[].class, byte[].class);

        assertInvoke("M8", ClassWithGenerics.class, "method4", byte[].class);
        assertInvoke("M8", ClassWithGenerics.class, "method4", byte[].class, byte[].class);
        // assertNotFound( ClassWithGenerics.class, "method4", byte[].class, byte[].class, byte[].class);
        assertInvoke("M8", ClassWithGenerics.class, "method4", byte[].class, byte.class, byte.class);

        assertInvoke("M9", ClassWithGenerics.class, "method5", byte[].class);
        assertInvoke("M9", ClassWithGenerics.class, "method5", byte[].class, byte[].class);
        assertInvoke("M9", ClassWithGenerics.class, "method5", byte[].class, byte[].class, byte[].class);

        Object t = new Object();
        assertInvoke(t, ClassWithGenerics.class, "copy", new Class<?>[] { Object.class }, new Object[] { t });
        Double[] d = new Double[] {};
        assertInvoke(d, ClassWithGenerics.class, "copy", new Class<?>[] { Double[].class }, new Object[] { d });

        assertInvoke("M10", ClassWithGenerics.class, "ne", byte[].class, byte.class);
        assertInvoke("M10", ClassWithGenerics.class, "ne", byte.class, byte[].class);

        assertInvoke("M11", ClassWithGenerics.class, "method6", byte[].class, byte[].class);

        assertInvoke("M14", ClassWithGenerics.class, "method7", String.class, String.class);

    }

    @Test
    public void testMethodChoosingWithNulls() throws AmbiguousMethodException {
        assertInvoke("M9", ForthClassWithMethods.class, "method1", new Class[] { null, null });
        assertInvoke("M8", ForthClassWithMethods.class, "method1", int.class, null);
        assertInvoke("M9", ForthClassWithMethods.class, "method1", String.class, null);
        assertAmbiguous(ForthClassWithMethods.class, "method2", null, null);
        assertInvoke("M12", ForthClassWithMethods.class, "method3", null, null, null);
    }

    @Test
    public void testMethodChoosingWithNullsVarArgs() throws AmbiguousMethodException {
        assertInvoke("M12", ForthClassWithMethods.class, "method3");
        assertInvoke("M13", ForthClassWithMethods.class, "method4", Integer.class);
        assertNotFound(ForthClassWithMethods.class, "method5");
        assertInvoke("M15-null", ForthClassWithMethods.class, "method6");
        assertInvoke("M15-null", ForthClassWithMethods.class, "method6", new Class<?>[] { null });
        assertInvoke("M15-2", ForthClassWithMethods.class, "method6", new Class<?>[] { null, null });
        assertInvoke("M15-1", ForthClassWithMethods.class, "method6", String.class);
        assertInvoke("M15-2", ForthClassWithMethods.class, "method6", String.class, String.class);
        assertInvoke("M16", ForthClassWithMethods.class, "method7", Integer.class);
        assertInvoke("M17-null", ForthClassWithMethods.class, "method8");
        assertInvoke("M17-null", ForthClassWithMethods.class, "method8", new Class<?>[] { null });
        assertInvoke("M17-2", ForthClassWithMethods.class, "method8", new Class<?>[] { null, null });
        assertInvoke("M17-1", ForthClassWithMethods.class, "method8", String.class);
        assertInvoke("M17-2", ForthClassWithMethods.class, "method8", String.class, String.class);

        assertInvoke("M21", ForthClassWithMethods.class, "method7");

        // assertAmbiguous(ForthClassWithMethods.class, "method7", String.class);
        assertInvoke("M19", ForthClassWithMethods.class, "method7", String.class);

        assertInvoke("M20", ForthClassWithMethods.class, "method7", String.class, String.class);
        assertAmbiguous(ForthClassWithMethods.class, "method7", null, null);

        // assertAmbiguous(ForthClassWithMethods.class, "method7", String.class, String.class, String.class);
        assertInvoke("M19", ForthClassWithMethods.class, "method7", String.class, String.class, String.class);

        assertInvoke("M16", ForthClassWithMethods.class, "method7", Integer.class);
        assertInvoke("M16", ForthClassWithMethods.class, "method7", Integer.class, String.class);
        assertInvoke("M16", ForthClassWithMethods.class, "method7", Integer.class, String.class, String.class);
        assertInvoke("M23", ForthClassWithMethods.class, "method9", String[].class, null);
        assertInvoke("M25", ForthClassWithMethods.class, "method10", Double.class);
        assertInvoke("M26", ForthClassWithMethods.class, "method11", new Class<?>[] { null, null });
        assertInvoke("M27", ForthClassWithMethods.class, "method12", Integer[].class, Integer[].class);
        assertInvoke("M28", ForthClassWithMethods.class, "method12", Integer[].class, String[].class);
        assertNotFound(ForthClassWithMethods.class, "method6", Object.class);
    }

    public static class ClassWithMethods {
        public String method1(int arg1, double arg2) {
            return "M1";
        }

        public String method1(int arg1, byte arg2) {
            return "M2";
        }

        public String method2(int arg1, double arg2) {
            return "M3";
        }
    }

    public static class SecondClassWithMethods extends ClassWithMethods implements Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public String method1(int arg1, double arg2) {
            return "M4";
        }

        public String method3(int arg1, double arg2) {
            return "M5";
        }
    }

    public static class ClassWithGenerics {
        public <T> String method1(T arg1, T arg2) {
            return "M6";
        }

        public <T> String method2(T arg1, T arg2) {
            return arg1.getClass().getSimpleName();
        }

        public <T> String method3(T[] arg1, T arg2) {
            return "M7";
        }

        public <T> String method4(T[] arg1, T[] arg2) {
            return "M8";
        }

        public <T> String method5(T[]... arg1) {
            return "M9";
        }

        public <T> T copy(T t) {
            return t;
        }

        public <T extends Serializable> T copy(T t) {
            return t;
        }

        public <T> String ne(T a, T b) {
            return "M10";
        }

        public <T> String method6(T[] arg1, T[] arg2) {
            return "M11";
        }

        public <T> String method6(T arg1, T arg2) {
            return "M12";
        }

        public <T extends String> T method7(T[] arg1, T arg2) {
            return (T) "M13";
        }

        public <T extends String> T method7(T arg1, T arg2) {
            return (T) "M14";
        }

    }

    public static class ThirdClassWithMethods extends SecondClassWithMethods {
        private static final long serialVersionUID = 1L;
    }

    public static class ForthClassWithMethods {
        public <T> String method1(int arg1, int arg2) {
            return "M7";
        }

        public <T> String method1(T arg1, T arg2) {
            return "M8";
        }

        public String method1(String arg1, String arg2) {
            return "M9";
        }

        public String method2(String[] arg1) {
            return "M10";
        }

        public String method2(Integer[] arg1) {
            return "M11";
        }

        public String method3(String[] arg1) {
            return "M12";
        }

        public String method4(Integer arg0, String[] arg1) {
            return "M13";
        }

        public String method5(String arg1) {
            return "M14";
        }

        public String method6(String... args) {
            return "M15-" + (args == null ? "null" : args.length);
        }

        public String method7(Integer arg0, String... arg1) {
            return "M16";
        }

        public String method8(String[] args) {
            return "M17-" + (args == null ? "null" : args.length);
        }

        public String method7(String arg0, String... arg1) {
            return "M19";
        }

        public String method7(String arg0, String arg1) {
            return "M20";
        }

        public String method7(String... args) {
            return "M21";
        }

        public <T> String method9(T[] arg0, T arg1) {
            return "M22";
        }

        public <T> String method9(T[] arg0, T[] arg1) {
            return "M23";
        }

        public <T> String method10(T[] arg0) {
            return "M24";
        }

        public String method10(Object arg0) {
            return "M25";
        }

        public <T> String method11(T[] arg0, T[] arg1) {
            return "M26";
        }

        public <T extends Comparable<?>> String method12(T[] arg0, T[] arg1) {
            return "M27";
        }

        public <T> String method12(T[] arg0, T[] arg1) {
            return "M28";
        }
    }
}
