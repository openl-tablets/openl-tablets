package org.openl.binding.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.openl.OpenL;
import org.openl.engine.OpenLManager;
import org.openl.source.impl.StringSourceCodeModule;

/**
 * Checks that every array index operator skips the null elements of an array or a list, and returns null for a
 * missing one.
 * <p>
 * A null result of the expression for an element is not a null element: TRANSFORM TO keeps it.
 *
 * @author Yury Molchan
 */
class IndexOperatorsNullTest {

    static Stream<Arguments> skipsNullElements() {
        return Stream.of(
                arguments("Integer[] a = {3, null, 1}; a[(x) transform to x]", new Integer[]{3, 1}),
                arguments("String[] a = {\"bb\", null, \"a\"}; a[transform to length()]", new int[]{2, 1}),
                arguments("String[] a = {\"bb\", null, \"a\"}; a[(s) transform to isEmpty(s) ? \"-\" : s]",
                        new String[]{"bb", "a"}),
                arguments("List l = new ArrayList(); l.add(3); l.add(null); l.add(1); l[(Integer x) transform to x]",
                        new Integer[]{3, 1}),

                arguments("Integer[] a = {3, null, 1, null, 3}; a[(x) transform unique to x]", new Integer[]{3, 1}),
                arguments("String[] a = {\"bb\", null, \"a\"}; a[(s) transform unique to s.length()]", new int[]{2, 1}),
                arguments("String[] a = {\"bb\", null, \"a\", \"cc\"}; a[*!@ length()]", new int[]{2, 1}),
                arguments("String[] a = {\"bb\", null, \"a\"}; a[(s) transform unique to isEmpty(s) ? \"-\" : s]",
                        new String[]{"bb", "a"}),
                arguments("List l = new ArrayList(); l.add(3); l.add(null); l.add(3); l[(Integer x) *!@ x]",
                        new Integer[]{3}),

                arguments("Integer[] a = {3, null, 1}; a[(x) select all having x == null]", new Integer[]{}),
                arguments("Integer[] a = {3, null, 1}; a[(x) select all having x != 3]", new Integer[]{1}),
                arguments("Integer[] a = {null, 3, 1}; a[(x) select first having x != 3]", 1),
                arguments("List l = new ArrayList(); l.add(null); l.add(1); l[(Integer x) @ x != 3]", new Integer[]{1}),

                arguments("Integer[] a = {3, null, 1}; a[(x) order by x]", new Integer[]{1, 3}),
                arguments("Integer[] a = {3, null, 1}; a[(x) order decreasing by x]", new Integer[]{3, 1}),
                arguments("String[] a = {\"bb\", null, \"a\"}; a[order by length()]", new String[]{"a", "bb"}),
                arguments("List l = new ArrayList(); l.add(3); l.add(null); l.add(1); l[(Integer x) ^@ x]",
                        new Integer[]{1, 3}),

                arguments("Integer[] a = {3, null, 1, 3}; a[(x) split by x]", new Integer[][]{{3, 3}, {1}}),
                arguments("String[] a = {\"5000\", null, \"2002\"}; a[split by substring(0, 1)]",
                        new String[][]{{"5000"}, {"2002"}}),
                arguments("List l = new ArrayList(); l.add(3); l.add(null); l.add(1); l[(Integer x) ~@ x]",
                        new Integer[][]{{3}, {1}}));
    }

    @ParameterizedTest
    @MethodSource
    void skipsNullElements(String expression, Object expected) {
        assertResult(expression, expected);
    }

    static Stream<Arguments> nullResultOfElement() {
        return Stream.of(
                arguments("String[] a = {\"bb\", \"a\"}; a[(s) transform to s == \"a\" ? null : s]",
                        new String[]{"bb", null}),
                arguments("String[] a = {\"bb\", \"a\"}; a[(s) transform unique to s == \"a\" ? null : s]",
                        new String[]{"bb"}));
    }

    @ParameterizedTest
    @MethodSource
    void nullResultOfElement(String expression, Object expected) {
        assertResult(expression, expected);
    }

    static Stream<String> missingArray() {
        return Stream.of("Integer[] a = null; a[(x) transform unique to x]",
                "List l = null; l[(Integer x) transform unique to x]",
                "Map m = null; m.values()[(v) transform unique to v]",
                "Integer[] a = null; a[(x) transform to x]",
                "List l = null; l[(Integer x) select all having x > 0]",
                "Integer[] a = null; a[(x) select first having x > 0]",
                "List l = null; l[(Integer x) order by x]",
                "Integer[] a = null; a[(x) split by x]");
    }

    @ParameterizedTest
    @MethodSource
    void missingArray(String expression) {
        assertResult(expression, null);
    }

    private static void assertResult(String expression, Object expected) {
        var result = OpenLManager.run(OpenL.getInstance(), new StringSourceCodeModule(expression, null));
        assertArrayEquals(new Object[]{expected}, new Object[]{result}, expression);
    }
}
