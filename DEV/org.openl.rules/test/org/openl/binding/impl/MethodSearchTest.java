package org.openl.binding.impl;

import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.openl.binding.exception.AmbiguousMethodException;

// The fixture methods exist only to be found by overload resolution, so they never read their parameters.
@SuppressWarnings("java:S1172")
class MethodSearchTest extends AbstractMethodSearchTest {

    @Test
    void testMethodChoosing() throws AmbiguousMethodException {
        assertInvoke("M1", ClassWithMethods.class, "method1", int.class, double.class);
        assertInvoke("M1", ClassWithMethods.class, "method1", int.class, int.class);
        assertInvoke("M2", ClassWithMethods.class, "method1", byte.class, byte.class);
        assertInvoke("M3", ClassWithMethods.class, "method2", byte.class, byte.class);
        assertNotFound(ClassWithMethods.class, "method3", int.class, double.class);
        assertInvoke("m1-BigInteger", ClassWithMethods.class, "m1", Long.class);

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

        assertInvoke("m2-Number", ClassWithMethods.class, "m2", short.class);
    }

    @Test
    void testMethodChoosingWithGenerics() throws AmbiguousMethodException {
        assertInvoke("M6", ClassWithGenerics.class, "method1", String.class, String.class);
        assertInvoke("M6", ClassWithGenerics.class, "method1", int.class, short.class);
        assertInvoke("M6", ClassWithGenerics.class, "method1", Byte.class, Long.class);
        assertInvoke("M6", ClassWithGenerics.class, "method1", Double.class, short.class);
        assertInvoke("M6", ClassWithGenerics.class, "method1", Integer.class, String.class);

        assertInvoke("String", ClassWithGenerics.class, "method2", String.class, String.class);
        assertInvoke("Integer", ClassWithGenerics.class, "method2", short.class, int.class);
        assertInvoke("Long", ClassWithGenerics.class, "method2", Byte.class, Long.class);
        assertInvoke("Double", ClassWithGenerics.class, "method2", Double.class, short.class);

        assertInvoke("M8", ClassWithGenerics.class, "method4", byte[].class);
        assertInvoke("M8", ClassWithGenerics.class, "method4", byte[].class, byte[].class);
        assertInvoke("M8", ClassWithGenerics.class, "method4", byte[].class, byte.class, byte.class);

        assertInvoke("M9", ClassWithGenerics.class, "method5", byte[].class);
        assertInvoke("M9", ClassWithGenerics.class, "method5", byte[].class, byte[].class);
        assertInvoke("M9", ClassWithGenerics.class, "method5", byte[].class, byte[].class, byte[].class);

        var t = new Object();
        assertInvoke(t, ClassWithGenerics.class, "copy", new Class<?>[]{Object.class}, new Object[]{t});
        Double[] d = new Double[]{};
        assertInvoke(d, ClassWithGenerics.class, "copy", new Class<?>[]{Double[].class}, new Object[]{d});

        assertInvoke("M10", ClassWithGenerics.class, "ne", byte[].class, byte.class);
        assertInvoke("M10", ClassWithGenerics.class, "ne", byte.class, byte[].class);

        assertInvoke("M11", ClassWithGenerics.class, "method6", byte[].class, byte[].class);

        assertInvoke("M14", ClassWithGenerics.class, "method7", String.class, String.class);

    }

    @Test
    void testMethodChoosingWithNulls() throws AmbiguousMethodException {
        assertInvoke("M9", ForthClassWithMethods.class, "method1", new Class[]{null, null});
        assertInvoke("M8", ForthClassWithMethods.class, "method1", int.class, null);
        assertInvoke("M9", ForthClassWithMethods.class, "method1", String.class, null);
        assertAmbiguous(ForthClassWithMethods.class, "method2", null, null);
        assertInvoke("M12", ForthClassWithMethods.class, "method3", null, null, null);
    }

    static Stream<Arguments> testMethodChoosingWithNullsVarArgs() {
        return Stream.of(
                arguments("M12", "method3", new Class<?>[]{}),
                arguments("M13", "method4", new Class<?>[]{Integer.class}),
                arguments(NF, "method5", new Class<?>[]{}),
                arguments("M15-null", "method6", new Class<?>[]{}),
                arguments("M15-null", "method6", new Class<?>[]{null}),
                arguments("M15-2", "method6", new Class<?>[]{null, null}),
                arguments("M15-1", "method6", new Class<?>[]{String.class}),
                arguments("M15-2", "method6", new Class<?>[]{String.class, String.class}),
                arguments("M16", "method7", new Class<?>[]{Integer.class}),
                arguments("M17-null", "method8", new Class<?>[]{}),
                arguments("M17-null", "method8", new Class<?>[]{null}),
                arguments("M17-2", "method8", new Class<?>[]{null, null}),
                arguments("M17-1", "method8", new Class<?>[]{String.class}),
                arguments("M17-2", "method8", new Class<?>[]{String.class, String.class}),

                arguments("M21", "method7", new Class<?>[]{}),
                arguments("M19", "method7", new Class<?>[]{String.class}),

                arguments("M20", "method7", new Class<?>[]{String.class, String.class}),
                arguments(AMB, "method7", new Class<?>[]{null, null}),
                arguments("M19", "method7", new Class<?>[]{String.class, String.class, String.class}),

                arguments("M16", "method7", new Class<?>[]{Integer.class}),
                arguments("M16", "method7", new Class<?>[]{Integer.class, String.class}),
                arguments("M16", "method7", new Class<?>[]{Integer.class, String.class, String.class}),
                arguments("M23", "method9", new Class<?>[]{String[].class, null}),
                arguments("M25", "method10", new Class<?>[]{Double.class}),
                arguments("M26", "method11", new Class<?>[]{null, null}),
                arguments("M27", "method12", new Class<?>[]{Integer[].class, Integer[].class}),
                arguments("M28", "method12", new Class<?>[]{Integer[].class, String[].class}),
                arguments(NF, "method6", new Class<?>[]{Object.class}));
    }

    @ParameterizedTest
    @MethodSource
    void testMethodChoosingWithNullsVarArgs(Object expected, String methodName, Class<?>[] argTypes)
            throws AmbiguousMethodException {
        assertMethod(expected, ForthClassWithMethods.class, methodName, argTypes);
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

        public String m1(long x) {
            return "m1-long";
        }

        public String m1(BigInteger x) {
            return "m1-BigInteger";
        }

        public <T> String m2(T o) {
            return "m2-Object";
        }

        public String m2(Number o) {
            return "m2-Number";
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
