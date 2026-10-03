package org.openl.rules.helpers;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.openl.OpenL;
import org.openl.engine.OpenLManager;
import org.openl.source.impl.StringSourceCodeModule;

/**
 * Checks that flatten returns an array of the closest common type of the elements, a primitive type included, and an
 * empty value when every argument is a missing array.
 *
 * @author Yury Molchan
 */
class FlattenTest {

    static Stream<Arguments> closestElementType() {
        return Stream.of(
                arguments("flatten(new int[] {1, 2})", new int[]{1, 2}),
                arguments("flatten(new int[][] {{1, 2}, {3}})", new int[]{1, 2, 3}),
                arguments("flatten(1, 2, 3)", new int[]{1, 2, 3}),
                arguments("flatten((int[]) null)", null),
                arguments("flatten((Integer[]) null)", null),
                arguments("flatten((int[]) null, new int[] {1})", new int[]{1}),
                arguments("flatten(new int[] {1}, new double[] {2.5})", new double[]{1, 2.5}),
                arguments("flatten(new boolean[] {true, false})", new boolean[]{true, false}),
                arguments("flatten(new char[] {'a', 'b'})", new char[]{'a', 'b'}),
                arguments("flatten(new int[] {1, 2}, new Integer[] {3})", new Integer[]{1, 2, 3}),
                arguments("flatten(new Integer[] {1, null}, new double[] {2.5})", new Double[]{1.0, null, 2.5}),
                arguments("flatten(new int[] {1}, null)", new Integer[]{1, null}),
                arguments("flatten(new String[][] {{\"a\"}, {\"b\"}})", new String[]{"a", "b"}));
    }

    @ParameterizedTest
    @MethodSource
    void closestElementType(String expression, Object expected) {
        var result = OpenLManager.run(OpenL.getInstance(), new StringSourceCodeModule(expression, null));
        assertArrayEquals(new Object[]{expected}, new Object[]{result}, expression);
        if (expected != null) {
            assertEquals(expected.getClass(), result.getClass(), expression);
        }
    }
}
