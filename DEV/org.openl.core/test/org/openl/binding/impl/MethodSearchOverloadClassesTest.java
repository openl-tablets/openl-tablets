package org.openl.binding.impl;

import org.junit.Test;
import org.openl.binding.exception.AmbiguousMethodException;

public class MethodSearchOverloadClassesTest extends AbstractMethodSearchTest {
    private Class<?> target = OverloadedMethods.class;

    @Test
    public void testSearch() throws AmbiguousMethodException {
        assertNotFound(target, "m0", A0.class);
        assertInvoke("A1", target, "m0", A1.class);
        assertInvoke("A1", target, "m0", A2.class);
        assertInvoke("A3", target, "m0", A3.class);
        assertInvoke("A3", target, "m0", A4.class);
    }

    @Test
    public void testOneArgument() throws AmbiguousMethodException {

        assertInvoke("A0", target, "m1", A0.class);
        assertInvoke("A1", target, "m1", A1.class);
        assertInvoke("A2", target, "m1", A2.class);
        assertInvoke("A3", target, "m1", A3.class);
        assertInvoke("A4", target, "m1", A4.class);
    }

    @Test
    public void testTwoArgument() throws AmbiguousMethodException {
        assertNotFound(target, "m2", A0.class, A0.class);
        assertNotFound(target, "m2", A1.class, A0.class);
        assertNotFound(target, "m2", A2.class, A0.class);
        assertNotFound(target, "m2", A3.class, A0.class);
        assertInvoke("A4", target, "m2", A4.class, A0.class);

        assertNotFound(target, "m2", A0.class, A1.class);
        assertNotFound(target, "m2", A1.class, A1.class);
        assertNotFound(target, "m2", A2.class, A1.class);
        assertInvoke("A3", target, "m2", A3.class, A1.class);
        assertAmbiguous(target, "m2", A4.class, A1.class);

        assertNotFound(target, "m2", A0.class, A2.class);
        assertNotFound(target, "m2", A1.class, A2.class);
        assertInvoke("A2", target, "m2", A2.class, A2.class);
        assertAmbiguous(target, "m2", A3.class, A2.class);
        assertAmbiguous(target, "m2", A4.class, A2.class);

        assertNotFound(target, "m2", A0.class, A3.class);
        assertInvoke("A1", target, "m2", A1.class, A3.class);
        assertAmbiguous(target, "m2", A2.class, A3.class);
        assertAmbiguous(target, "m2", A3.class, A3.class);
        assertAmbiguous(target, "m2", A4.class, A3.class);

        assertInvoke("A0", target, "m2", A0.class, A4.class);
        assertAmbiguous(target, "m2", A1.class, A4.class);
        assertAmbiguous(target, "m2", A2.class, A4.class);
        assertAmbiguous(target, "m2", A3.class, A4.class);
        assertAmbiguous(target, "m2", A4.class, A4.class);
    }

    static class A0 {
    }

    static class A1 extends A0 {
    }

    static class A2 extends A1 {
    }

    static class A3 extends A2 {
    }

    static class A4 extends A3 {
    }

    public static class OverloadedMethods {
        public static String m0(A1 arg) {
            return "A1";
        }

        public static String m0(A3 arg) {
            return "A3";
        }

        public static String m1(A0 arg) {
            return "A0";
        }

        public static String m1(A1 arg) {
            return "A1";
        }

        public static String m1(A2 arg) {
            return "A2";
        }

        public static String m1(A3 arg) {
            return "A3";
        }

        public static String m1(A4 arg) {
            return "A4";
        }

        public static String m2(A0 arg1, A4 arg2) {
            return "A0";
        }

        public static String m2(A1 arg1, A3 arg2) {
            return "A1";
        }

        public static String m2(A2 arg1, A2 arg2) {
            return "A2";
        }

        public static String m2(A3 arg1, A1 arg2) {
            return "A3";
        }

        public static String m2(A4 arg1, A0 arg2) {
            return "A4";
        }
    }
}
