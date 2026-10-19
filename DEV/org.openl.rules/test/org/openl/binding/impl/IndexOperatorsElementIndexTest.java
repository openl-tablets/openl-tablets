package org.openl.binding.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.openl.OpenL;
import org.openl.engine.OpenLManager;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.syntax.exception.CompositeOpenlException;
import org.openl.util.text.TextInfo;

/**
 * Checks the index of an element named in an array index operator, as in {@code array[(x, i) transform to i]}.
 * <p>
 * The index is the position of the element in the array or the collection from 0. The empty elements an operator
 * skips keep their positions.
 */
class IndexOperatorsElementIndexTest {

    private static final String LETTERS = "String[] a = {\"a\", \"b\", \"c\", \"d\"}; ";
    private static final String POINTS = "java.awt.Point[] p = {new java.awt.Point(1, 2)}; ";

    static Stream<Arguments> everyOperator() {
        return Stream.of(
                arguments(LETTERS + "a[(s, i) select all having i != 1]", new String[]{"a", "c", "d"}),
                arguments(LETTERS + "a[(s, i) select all where i > 1]", new String[]{"c", "d"}),
                arguments(LETTERS + "a[(s, i) @ i == 0 or s == \"d\"]", new String[]{"a", "d"}),
                arguments(LETTERS + "a[(s, i) select first having i > 1]", "c"),
                arguments(LETTERS + "a[(s, i) select first where i > 5]", null),
                arguments(LETTERS + "a[(s, i) !@ i == 3]", "d"),
                arguments(LETTERS + "a[(s, i) order by -i]", new String[]{"d", "c", "b", "a"}),
                arguments(LETTERS + "a[(s, i) order increasing by -i]", new String[]{"d", "c", "b", "a"}),
                arguments(LETTERS + "a[(s, i) ^@ i == 1]", new String[]{"a", "c", "d", "b"}),
                arguments(LETTERS + "a[(s, i) order decreasing by i]", new String[]{"d", "c", "b", "a"}),
                arguments(LETTERS + "a[(s, i) v@ i == 2]", new String[]{"c", "a", "b", "d"}),
                arguments(LETTERS + "a[(s, i) split by i < 2]", new String[][]{{"a", "b"}, {"c", "d"}}),
                arguments(LETTERS + "a[(s, i) ~@ i == 2]", new String[][]{{"a", "b", "d"}, {"c"}}),
                arguments(LETTERS + "a[(s, i) s@ i > 0]", new String[][]{{"a"}, {"b", "c", "d"}}),
                arguments(LETTERS + "a[(s, i) transform to s + i]", new String[]{"a0", "b1", "c2", "d3"}),
                arguments(LETTERS + "a[(s, i) *@ i]", new Integer[]{0, 1, 2, 3}),
                arguments(LETTERS + "a[(s, i) transform unique to i < 2]", new Boolean[]{true, false}),
                arguments(LETTERS + "a[(s, i) *!@ i > 0 ? \"tail\" : s]", new String[]{"a", "tail"}));
    }

    static Stream<Arguments> declarations() {
        return Stream.of(
                arguments(LETTERS + "a[(String s, Integer i) transform to s + i]",
                        new String[]{"a0", "b1", "c2", "d3"}),
                arguments(LETTERS + "a[(String s, int i) transform to i * 2]", new int[]{0, 2, 4, 6}),
                arguments(LETTERS + "a[(s, Integer i) select all having i >= 2]", new String[]{"c", "d"}),
                arguments(LETTERS + "a[(String s, i) select first having i == 1 and s == \"b\"]", "b"),
                arguments(LETTERS + "a[( s ,  i ) transform to i]", new Integer[]{0, 1, 2, 3}),
                arguments(LETTERS + "a[(java.lang.String s, java.lang.Integer i) !@ i == 2]", "c"),
                arguments("String[][] a = {{\"a\"}, {\"b\", \"c\"}}; a[(String[] row, i) transform to row[0] + i]",
                        new String[]{"a0", "b1"}),
                arguments(LETTERS + "a[(s, i) transform to a[i] == s]", new boolean[]{true, true, true, true}),
                arguments("Integer[] a = {10, 20}; a[(Long x, i) transform to x + i]", new Long[]{10L, 21L}),
                arguments(LETTERS + "a[(s, Long i) transform to i]", new Long[]{0L, 1L, 2L, 3L}),
                arguments(LETTERS + "a[(s, long i) transform to i]", new long[]{0, 1, 2, 3}),
                arguments(LETTERS + "a[(s, Double i) transform to i / 2]", new Double[]{0.0, 0.5, 1.0, 1.5}),
                arguments(LETTERS + "a[(s, Object i) select all having i != 1]", new String[]{"a", "c", "d"}),
                arguments(LETTERS + "a[(s, Number i) transform to i]", new Number[]{0, 1, 2, 3}),
                arguments(LETTERS + "a[(s, BigDecimal i) transform to i]",
                        new BigDecimal[]{BigDecimal.valueOf(0), BigDecimal.ONE, BigDecimal.valueOf(2),
                                BigDecimal.valueOf(3)}),
                arguments(LETTERS + "a[(s, BigInteger i) transform to i]",
                        new BigInteger[]{BigInteger.ZERO, BigInteger.ONE, BigInteger.TWO, BigInteger.valueOf(3)}));
    }

    static Stream<Arguments> positions() {
        return Stream.of(
                arguments("double[] in = {1.5, 2.5, 3.5, 4.5, 5.5}; in[(x, i) transform to i < 3 ? x : 0.0]",
                        new double[]{1.5, 2.5, 3.5, 0.0, 0.0}),
                arguments("Double[] in = {1.5, 2.5, 3.5, 4.5}; in[(x, i) transform to i < 3 ? x : 0.0]",
                        new Double[]{1.5, 2.5, 3.5, 0.0}),
                arguments("Integer[] a = {3, null, 1}; a[(x, i) transform to i]", new Integer[]{0, 2}),
                arguments("Integer[] a = {null, 3, null, 1}; a[(x, i) select all having i > 1]", new Integer[]{1}),
                arguments("List l = new ArrayList(); l.add(\"a\"); l.add(null); l.add(\"b\"); "
                        + "l[(String s, i) transform to s + i]", new String[]{"a0", "b2"}),
                arguments("Set s = new LinkedHashSet(); s.add(\"x\"); s.add(\"y\"); s[(String e, i) *@ e + i]",
                        new String[]{"x0", "y1"}),
                arguments("String[][] a = {{\"a\", \"b\"}, {\"c\"}}; "
                        + "a[(row, i) transform to row[(s, j) transform to s + i + j]]",
                        new String[][]{{"a00", "b01"}, {"c10"}}),
                arguments(LETTERS + "a[(s, i) select all having i > 0][(s, i) transform to s + i]",
                        new String[]{"b0", "c1", "d2"}));
    }

    static Stream<Arguments> unchangedSyntax() {
        return Stream.of(
                arguments(LETTERS + "int i = 1; a[(i)]", "b"),
                arguments(LETTERS + "int i = 1; a[(i) * 2]", "c"),
                arguments(LETTERS + "int i = 1; a[(i + 2)]", "d"),
                arguments(LETTERS + "a[(Integer) 2]", "c"),
                arguments(LETTERS + "a[(s) transform to s + s]", new String[]{"aa", "bb", "cc", "dd"}),
                arguments(LETTERS + "a[(String s) @ s > \"b\"]", new String[]{"c", "d"}),
                arguments(LETTERS + "a[!@ length() == 1]", "a"));
    }

    @ParameterizedTest
    @MethodSource({"everyOperator", "declarations", "positions", "unchangedSyntax"})
    void evaluates(String expression, Object expected) {
        assertArrayEquals(new Object[]{expected}, new Object[]{run(expression)}, expression);
    }

    static Stream<Arguments> errors() {
        return Stream.of(
                arguments(LETTERS + "a[(s, String i) transform to i]",
                        "Cannot convert an index of type 'Integer' to type 'java.lang.String'.",
                        "String i"),
                arguments(LETTERS + "a[(s, Byte i) transform to i]",
                        "Cannot convert an index of type 'Integer' to type 'java.lang.Byte'.",
                        "Byte i"),
                arguments(LETTERS + "a[(s, short i) transform to i]",
                        "Cannot convert an index of type 'Integer' to type 'short'.",
                        "short i"),
                arguments(LETTERS + "a[(s, float i) transform to i]",
                        "Cannot convert an index of type 'Integer' to type 'float'.",
                        "float i"),
                arguments(LETTERS + "a[(s, Integer[] i) transform to s]",
                        "Cannot convert an index of type 'Integer' to type 'java.lang.Integer[]'.",
                        "Integer[] i"),
                arguments(POINTS + "p[(q, x) split by x]",
                        "Field 'x' is ambiguous:\nMatching fields:\nx, Point.x",
                        "x"),
                arguments(POINTS + "p[(x) split by x]",
                        "Field 'x' is ambiguous:\nMatching fields:\nx, Point.x",
                        "x"),
                arguments(LETTERS + "a[(s, Foo i) transform to i]", "Type 'Foo' is not found.", "Foo"),
                arguments(LETTERS + "a[(Foo s, i) transform to i]", "Type 'Foo' is not found.", "Foo"),
                arguments(LETTERS + "a[(s, s) transform to s]", "Variable 's' is already defined.", "s"),
                arguments(LETTERS + "int i = 0; a[(s, i) transform to s]", "Variable 'i' is already defined.", "i"),
                arguments(LETTERS + "String s = \"\"; a[(s, i) transform to s]", "Variable 's' is already defined.",
                        "s"),
                arguments(LETTERS + "a[(Date d, i) transform to i]",
                        "Cannot cast 'java.lang.String' to 'java.util.Date'.",
                        "Date d"));
    }

    @ParameterizedTest
    @MethodSource
    void errors(String expression, String message, String fragment) {
        var e = assertThrows(CompositeOpenlException.class, () -> run(expression));
        assertEquals(1, e.getErrors().length, expression);
        var error = e.getErrors()[0];
        assertEquals(message, error.getMessage(), expression);
        var text = new TextInfo(expression);
        var location = error.getLocation();
        assertEquals(fragment,
                expression.substring(location.getStart().getAbsolutePosition(text),
                        location.getEnd().getAbsolutePosition(text) + 1),
                expression);
    }

    static Stream<String> syntaxErrors() {
        return Stream.of(LETTERS + "a[(s, i, j) transform to s]", LETTERS + "a[(s, ) transform to s]",
                LETTERS + "a[(, i) transform to s]", LETTERS + "a[(s, Integer i[]) transform to s]",
                LETTERS + "a[(String s[], i) transform to s]");
    }

    @ParameterizedTest
    @MethodSource
    void syntaxErrors(String expression) {
        assertThrows(CompositeOpenlException.class, () -> run(expression));
    }

    private static Object run(String expression) {
        return OpenLManager.run(OpenL.getInstance(), new StringSourceCodeModule(expression, null));
    }
}
