package org.openl.binding.impl.method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.openl.OpenL;
import org.openl.engine.OpenLManager;
import org.openl.source.impl.StringSourceCodeModule;

/**
 * Checks the type of the array that the array functions return: the type of the array they get, or the closest common
 * type of the array and the added elements.
 *
 * @author Yury Molchan
 */
class ArrayResultTypesTest {

    static Stream<Arguments> keepsArrayType() {
        return Stream.of(
                arguments("sort(new int[] {3, 1, 2})", new int[]{1, 2, 3}),
                arguments("sort(3, 1, 2)", new int[]{1, 2, 3}),
                arguments("sort(new Integer[] {3, 1})", new Integer[]{1, 3}),
                arguments("slice(new int[] {1, 2, 3}, 1)", new int[]{2, 3}),
                arguments("slice(new String[] {\"a\", \"b\"}, 1)", new String[]{"b"}),
                arguments("removeElement(new int[] {1, 2, 2}, 2)", new int[]{1, 2}),
                arguments("removeElement(new int[] {1, 2, 3}, 2, 3)", new int[]{1}),
                arguments("removeNulls(new int[] {1, 2})", new int[]{1, 2}),
                arguments("removeNulls(1, null, 3)", new Integer[]{1, 3}),
                arguments("remove(new int[] {1, 2}, 0)", new int[]{2}),
                arguments("remove(new double[] {1.5, 2.5}, 1)", new double[]{1.5}));
    }

    @ParameterizedTest
    @MethodSource
    void keepsArrayType(String expression, Object expected) {
        assertResult(expression, expected);
    }

    static Stream<Arguments> widensArrayType() {
        return Stream.of(
                arguments("addElement(new int[] {1, 2}, 1, 5)", new int[]{1, 5, 2}),
                arguments("addElement(new int[] {1, 2}, 1, 5, 6)", new int[]{1, 5, 6, 2}),
                arguments("addElement(new int[] {1, 2}, 1, new int[] {7, 8})", new int[]{1, 7, 8, 2}),
                arguments("addElement(new int[] {1, 2}, 1, 2.5)", new double[]{1, 2.5, 2}),
                arguments("addElement(new int[] {1, 2}, 1, (Integer) null)", new Integer[]{1, null, 2}),
                arguments("addElement(new Integer[] {1, null}, 1, 2.5)", new Double[]{1.0, 2.5, null}),
                arguments("add(new int[] {1, 2}, 3)", new int[]{1, 2, 3}),
                arguments("add(new int[] {1}, 2.5)", new double[]{1, 2.5}),
                arguments("add(new int[] {1, 2}, (Integer) null)", new Integer[]{1, 2, null}),
                arguments("add(new Integer[] {1, null}, 2.5)", new Double[]{1.0, null, 2.5}),
                arguments("add(1, 2, 3)", new int[]{1, 2, 3}),
                arguments("add(\"a\", 1)", new Object[]{"a", 1}),
                arguments("add(null, null)", new Object[]{null, null}),
                arguments("addAll(new int[] {1}, new double[] {2.5})", new double[]{1, 2.5}));
    }

    @ParameterizedTest
    @MethodSource
    void widensArrayType(String expression, Object expected) {
        assertResult(expression, expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"remove((int[]) null, 0)", "remove(null, 0)", "slice((int[]) null, 1)", "sort()",
            "removeNulls()"})
    void missingArray(String expression) {
        assertNull(run(expression), expression);
    }

    private static void assertResult(String expression, Object expected) {
        var result = run(expression);
        assertEquals(expected.getClass(), result.getClass(), expression);
        assertArrayEquals(new Object[]{expected}, new Object[]{result}, expression);
    }

    private static Object run(String expression) {
        return OpenLManager.run(OpenL.getInstance(), new StringSourceCodeModule(expression, null));
    }
}
