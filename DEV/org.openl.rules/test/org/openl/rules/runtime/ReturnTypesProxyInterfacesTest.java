package org.openl.rules.runtime;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.rules.TestUtils;

public class ReturnTypesProxyInterfacesTest {

    private static final String EXP_MSG = "Expected return type '%s' for method '%s', but found '%s'.";

    private <T> T initInstance(Class<T> tClass) {
        return TestUtils.create("test/rules/runtime/ReturnTypeValidation.xlsx", tClass);
    }

    @Test
    public void testSameReturnType() {
        assertEquals((Long) 1L, initInstance(SameReturnType.class).doSomething("1"));
    }

    @Test
    public void testUpCastReturnType() {
        assertEquals(1L, initInstance(UpCastReturnType.class).doSomething("1"));
    }

    @Test
    public void testObjectReturnType() {
        assertEquals(1L, initInstance(ObjectReturnType.class).doSomething("1"));
    }

    @Test
    public void testVoidReturnType() {
        try {
            initInstance(VoidReturnType.class);
        } catch (RuntimeException e) {
            assertReturnTypeException(String.format(EXP_MSG, "java.lang.Long", "doSomething", "void"), e);
            return;
        }
        fail("Must be failed with return type mismatch exception");
    }

    @Test
    public void testArrayReturnType() {
        try {
            initInstance(ArrayReturnType.class);
        } catch (RuntimeException e) {
            assertReturnTypeException(String.format(EXP_MSG, "java.lang.Long", "doSomething", "[Ljava.lang.Long;"), e);
            return;
        }
        fail("Must be failed with return type mismatch exception");
    }

    @Test
    public void testCannotCastReturnType() {
        try {
            initInstance(CannotCastReturnType.class);
        } catch (RuntimeException e) {
            assertReturnTypeException(String.format(EXP_MSG, "java.lang.Long", "doSomething", "java.lang.Integer"), e);
            return;
        }
        fail("Must be failed with return type mismatch exception");
    }

    @Test
    public void testLongArrayReturnType() {
        try {
            initInstance(LongArrayReturnType.class);
        } catch (RuntimeException e) {
            assertReturnTypeException(String.format(EXP_MSG, "[J", "doArray", "[Ljava.lang.Long;"), e);
            return;
        }
        fail("Must be failed with return type mismatch exception");
    }

    @Test
    public void testPrimitiveLongArrayReturnType() {
        final long[] expected = new long[] { 1, 2, 3 };
        final long[] actual = initInstance(PrimitiveLongArrayReturnType.class).doArray();
        assertArraysEquals(expected, actual);
    }

    @Test
    public void testVoidReturnTypeToVoid() {
        initInstance(VoidReturnTypeToVoid.class).voidMethod();
    }

    @Test
    public void testVoidReturnTypeToInt() {
        try {
            initInstance(VoidReturnTypeToInt.class);
        } catch (RuntimeException e) {
            assertReturnTypeException(String.format(EXP_MSG, "void", "voidMethod", "int"), e);
            return;
        }
        fail("Must be failed with return type mismatch exception");
    }

    private void assertArraysEquals(long[] expected, long[] actual) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals((Long) actual[i], (Long) expected[i]);
        }
    }

    private void assertReturnTypeException(String expectedMsg, RuntimeException actual) {
        Throwable cause = actual.getCause();
        while (cause != null && !cause.getMessage().equals(expectedMsg)) {
            cause = cause.getCause();
        }
        assertNotNull(cause);
        assertEquals(expectedMsg, cause.getMessage());
    }

    public interface SameReturnType {
        Long doSomething(String str);
    }

    public interface UpCastReturnType {
        Number doSomething(String str);
    }

    public interface ObjectReturnType {
        Object doSomething(String str);
    }

    public interface VoidReturnType {
        void doSomething(String str);
    }

    public interface ArrayReturnType {
        Long[] doSomething(String str);
    }

    public interface CannotCastReturnType {
        Integer doSomething(String str);
    }

    public interface PrimitiveLongArrayReturnType {
        long[] doArray();
    }

    public interface LongArrayReturnType {
        Long[] doArray();
    }

    public interface VoidReturnTypeToVoid {
        void voidMethod();
    }

    public interface VoidReturnTypeToInt {
        int voidMethod();
    }
}
