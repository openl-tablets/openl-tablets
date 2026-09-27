package org.openl.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Created by tsaltsevich on 5/3/2016.
 */
class StringUtilsTest {

    //All possible combinations of hidden UTF-8 symbols
    private static final String CONTROLS_AND_SPACES = "\u0000\u0001\u0002\u0003\u0004\u0005\u0006\u0007\u0008\u0009" +
            "\u000B\u000C\u000E\u000F\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017\u0018\u0019\u001A\u001B\u001C" +
            "\u001D\u001E\u001F\u0020\u007F\u0080\u0081\u0082\u0083\u0084\u0085\u0086\u0087\u0088\u0089\u008A\u008B" +
            "\u008C\u008D\u008E\u008F\u0090\u0091\u0092\u0093\u0094\u0095\u0096\u0097\u0098\u0099\u009A\u009B\u009C" +
            "\u009D\u009E\u009F\u00A0\u2007\u202F\r\n\t\b\f";

    @Test
    void testSplitNull() {
        assertNull(StringUtils.split(null, ' '));
        assertNull(StringUtils.split(null, '*'));
    }

    static Stream<Arguments> testSplit() {
        return Stream.of(
                arguments("", '*', new String[]{}),
                arguments("a.b.c", '.', new String[]{"a", "b", "c"}),
                arguments("a..b.c", '.', new String[]{"a", "b", "c"}),
                arguments("a:b:c", '.', new String[]{"a:b:c"}),
                arguments("a b c", ' ', new String[]{"a", "b", "c"}),
                arguments("a..b.c.", '.', new String[]{"a", "b", "c"}),
                arguments("..a..b.c..", '.', new String[]{"a", "b", "c"}),
                arguments("a..", '.', new String[]{"a"}),
                arguments("a.", '.', new String[]{"a"}),
                arguments(".a", '.', new String[]{"a"}),
                arguments("..a", '.', new String[]{"a"}),
                arguments("..a.", '.', new String[]{"a"}),
                arguments("..a..", '.', new String[]{"a"}),

                arguments(" \t\r\n", '*', new String[]{}),
                arguments(" \t\r\n *  * * \t\n", '*', new String[]{}),
                arguments(" a .b .c ", '.', new String[]{"a", "b", "c"}),
                arguments(" a . . b . c ", '.', new String[]{"a", "b", "c"}),
                arguments(" a : b : c ", '.', new String[]{"a : b : c"}),
                arguments("a b \t\r\nc", ' ', new String[]{"a", "b", "c"}),
                arguments("a. .b.c .", '.', new String[]{"a", "b", "c"}),
                arguments(". . a..b.c..", '.', new String[]{"a", "b", "c"}),
                arguments("a\t..\n", '.', new String[]{"a"}),
                arguments("a\t", '.', new String[]{"a"}),
                arguments("\na", '.', new String[]{"a"}),
                arguments("  a", '.', new String[]{"a"}),
                arguments("  a ", '.', new String[]{"a"}),
                arguments(". a. ", '.', new String[]{"a"}));
    }

    @ParameterizedTest
    @MethodSource
    void testSplit(String text, char separator, String[] expected) {
        assertArrayEquals(expected, StringUtils.split(text, separator));
    }

    @Test
    void testSplitWS() {
        assertNull(StringUtils.split(null));
        assertArrayEquals(new String[]{}, StringUtils.split(""), "Returned array is not empty");
        assertArrayEquals(new String[]{}, StringUtils.split("  \n\r  \t \r\n  \t\t"), "Returned array is not empty");
        assertArrayEquals(new String[]{"a", "b", "c"}, StringUtils.split("a b c"), "Returned array is not valid");
        assertArrayEquals(new String[]{"a", "b", "c"}, StringUtils.split("a \tb\nc"), "Returned array is not valid");
        assertArrayEquals(new String[]{"a:b:c"}, StringUtils.split("a:b:c"), "Returned array is not valid");
        assertArrayEquals(new String[]{"a", "b", "c"}, StringUtils.split("a\tb\rc"), "Returned array is not valid");
        assertArrayEquals(new String[]{"a", "b", "c"},
                StringUtils.split("a\n\nb c\n"),
                "Returned array is not valid");
        assertArrayEquals(new String[]{"a", "b", "c"},
                StringUtils.split("\t\ta  b c  "),
                "Returned array is not valid");
        assertArrayEquals(new String[]{"a"}, StringUtils.split("a  "), "Returned array is not valid");
        assertArrayEquals(new String[]{"a"}, StringUtils.split("a "), "Returned array is not valid");
        assertArrayEquals(new String[]{"a"}, StringUtils.split(" a"), "Returned array is not valid");
        assertArrayEquals(new String[]{"a"}, StringUtils.split("  a"), "Returned array is not valid");
        assertArrayEquals(new String[]{"a"}, StringUtils.split("  a "), "Returned array is not valid");
        assertArrayEquals(new String[]{"a"}, StringUtils.split("\t a\n\r"), "Returned array is not valid");
    }

    @Test
    void testToLines() {
        assertNull(StringUtils.toLines(null));
        assertNull(StringUtils.toLines(""));
        assertNull(StringUtils.toLines("\r\n\t "));
        assertArrayEquals(new String[]{"a"}, StringUtils.toLines("a"), "Returned array is not valid");
        assertArrayEquals(new String[]{"a"}, StringUtils.toLines("\r\n\t  a\r\n\t "), "Returned array is not valid");
        assertArrayEquals(new String[]{"a", "b", "c"}, StringUtils.toLines("a\r\nb\r\nc"), "Returned array is not valid");
        assertArrayEquals(new String[]{"a", "b", "c"}, StringUtils.toLines("\na\rb\nc\r"), "Returned array is not valid");
        assertArrayEquals(new String[]{"a", "b", "c"}, StringUtils.toLines("\n\ra\r\tb \nc\n"), "Returned array is not valid");
        assertArrayEquals(new String[]{"a", "c"}, StringUtils.toLines("\r a\n\t\r \n c \n"), "Returned array is not valid");
    }

    @Test
    void testJoinObject() {
        assertNull(StringUtils.join(null, "*"), "Returned string is not valid");
        assertEquals("", StringUtils.join(new Object[]{}, "*"), "Returned string is not valid");
        assertEquals("null", StringUtils.join(new Object[]{null}, "*"), "Returned string is not valid");
        assertEquals("null,null", StringUtils.join(new Object[]{null, null}, ","), "Returned string is not valid");
        assertEquals("a--b--c", StringUtils.join(new Object[]{"a", "b", "c"}, "--"), "Returned string is not valid");
        assertEquals("null,,a", StringUtils.join(new Object[]{null, "", "a"}, ","), "Returned string is not valid");
    }

    @Test
    void testIsEmpty() {
        assertTrue(StringUtils.isEmpty(null), "Returned value is false");
        assertTrue(StringUtils.isEmpty(""), "Returned value is false");
        assertFalse(StringUtils.isEmpty(" "), "Returned value is true");
        assertFalse(StringUtils.isEmpty("boo"), "Returned value is true");
        assertFalse(StringUtils.isEmpty("  boo  "), "Returned value is true");
    }

    @Test
    void testIsNotEmpty() {
        assertFalse(StringUtils.isNotEmpty(null), "Returned value is true");
        assertFalse(StringUtils.isNotEmpty(""), "Returned value is true");
        assertTrue(StringUtils.isNotEmpty(" "), "Returned value is false");
        assertTrue(StringUtils.isNotEmpty("boo"), "Returned value is false");
        assertTrue(StringUtils.isNotEmpty("  boo  "), "Returned value is false");
    }

    @Test
    void testIsBlank() {
        assertTrue(StringUtils.isBlank(null), "Returned value is false");
        assertTrue(StringUtils.isBlank(""), "Returned value is false");
        assertTrue(StringUtils.isBlank(" "), "Returned value is false");
        assertFalse(StringUtils.isBlank("boo"), "Returned value is true");
        assertFalse(StringUtils.isBlank("  boo  "), "Returned value is true");
        assertTrue(StringUtils.isBlank(CONTROLS_AND_SPACES), "Returned value is true");
    }

    @Test
    void testIsNotBlank() {
        assertFalse(StringUtils.isNotBlank(null), "Returned value is true");
        assertFalse(StringUtils.isNotBlank(""), "Returned value is true");
        assertFalse(StringUtils.isNotBlank(" "), "Returned value is true");
        assertTrue(StringUtils.isNotBlank("boo"), "Returned value is false");
        assertTrue(StringUtils.isNotBlank("  boo  "), "Returned value is false");
        assertFalse(StringUtils.isNotBlank(CONTROLS_AND_SPACES), "Returned value is false");
    }

    @Test
    void testContainsIgnoreCase() {
        assertFalse(StringUtils.containsIgnoreCase(null, ""), "Returned value is true");
        assertFalse(StringUtils.containsIgnoreCase("", null), "Returned value is true");
        assertFalse(StringUtils.containsIgnoreCase("abc", "z"), "Returned value is true");
        assertFalse(StringUtils.containsIgnoreCase("абя", "в"), "Returned value is true");
        assertFalse(StringUtils.containsIgnoreCase("abc", "Z"), "Returned value is true");

        assertTrue(StringUtils.containsIgnoreCase("", ""), "Returned value is false");
        assertTrue(StringUtils.containsIgnoreCase("abc", ""), "Returned value is false");
        assertTrue(StringUtils.containsIgnoreCase("abc", "a"), "Returned value is false");
        assertTrue(StringUtils.containsIgnoreCase("абв", "б"), "Returned value is false");
        assertTrue(StringUtils.containsIgnoreCase("abc", "B"), "Returned value is false");
    }

    @Test
    void testMatches() {
        assertFalse(StringUtils.matches(Pattern.compile("\\d"), ""), "Returned value is true");
        assertTrue(StringUtils.matches(Pattern.compile("\\d"), "1"), "Returned value is true");
        assertTrue(StringUtils.matches(Pattern.compile("\\d"), "2"), "Returned value is true");
        assertFalse(StringUtils.matches(Pattern.compile("\\d"), "12"), "Returned value is true");
    }

    @Test
    void testTrim() {
        assertNull(StringUtils.trim(null), "Returned string is not valid");
        assertEquals("", StringUtils.trim(""), "Returned string is not valid");
        assertEquals("", StringUtils.trim("     "), "Returned string is not valid");
        assertEquals("boo", StringUtils.trim("boo"), "Returned string is not valid");
        assertEquals("boo", StringUtils.trim("    boo    "), "Returned string is not valid");
        assertEquals("bar", StringUtils.trim("     bar     "), "Returned string is not valid");
        assertEquals("foo", StringUtils.trim(CONTROLS_AND_SPACES + "  foo" + CONTROLS_AND_SPACES + "   "), "Returned string is not valid");
        assertEquals("", StringUtils.trim(CONTROLS_AND_SPACES), "Returned string is not valid");
    }

    @Test
    void testTrimToNull() {
        assertNull(StringUtils.trimToNull(null), "Returned string is not valid");
        assertNull(StringUtils.trimToNull(""), "Returned string is not valid");
        assertNull(StringUtils.trimToNull("     "), "Returned string is not valid");
        assertEquals("boo", StringUtils.trimToNull("boo"), "Returned string is not valid");
        assertEquals("boo", StringUtils.trimToNull("    boo    "), "Returned string is not valid");
        assertEquals("bar", StringUtils.trimToNull("     bar     "), "Returned string is not valid");
        assertEquals("foo", StringUtils.trimToNull(CONTROLS_AND_SPACES + "  foo" + CONTROLS_AND_SPACES + "   "), "Returned string is not valid");
        assertNull(StringUtils.trimToNull(CONTROLS_AND_SPACES), "Returned string is not valid");
    }

    @Test
    void testTrimToEmpty() {
        assertEquals("", StringUtils.trimToEmpty(null), "Returned string is not valid");
        assertEquals("", StringUtils.trimToEmpty(""), "Returned string is not valid");
        assertEquals("", StringUtils.trimToEmpty("     "), "Returned string is not valid");
        assertEquals("boo", StringUtils.trimToEmpty("boo"), "Returned string is not valid");
        assertEquals("boo", StringUtils.trimToEmpty("    boo    "), "Returned string is not valid");
        assertEquals("bar", StringUtils.trimToEmpty("     bar     "), "Returned string is not valid");
        assertEquals("foo", StringUtils.trimToEmpty(CONTROLS_AND_SPACES + "  foo" + CONTROLS_AND_SPACES + "   "), "Returned string is not valid");
        assertEquals("", StringUtils.trimToEmpty(CONTROLS_AND_SPACES), "Returned string is not valid");
    }

    @Test
    void testCapitalize() {
        assertNull(StringUtils.capitalize(null), "Returned string is not valid");
        assertEquals("", StringUtils.capitalize(""), "Returned string is not valid");
        assertEquals("Foo", StringUtils.capitalize("foo"), "Returned string is not valid");
        assertEquals("FOo", StringUtils.capitalize("fOo"), "Returned string is not valid");
        assertEquals("МУу", StringUtils.capitalize("мУу"), "Returned string is not valid");
    }

    @Test
    void testUnCapitalize() {
        assertNull(StringUtils.uncapitalize(null), "Returned string is not valid");
        assertEquals("", StringUtils.uncapitalize(""), "Returned string is not valid");
        assertEquals("foo", StringUtils.uncapitalize("Foo"), "Returned string is not valid");
        assertEquals("fOO", StringUtils.uncapitalize("FOO"), "Returned string is not valid");
        assertEquals("муУ", StringUtils.uncapitalize("МуУ"), "Returned string is not valid");
    }

    @Test
    void testCamelToKebab() {
        assertNull(StringUtils.camelToKebab(null), "Returned string is not valid");
        assertEquals("", StringUtils.camelToKebab(""), "Returned string is not valid");
        assertEquals("foo", StringUtils.camelToKebab("FOO"), "Returned string is not valid");
        assertEquals("foo", StringUtils.camelToKebab("Foo"), "Returned string is not valid");
        assertEquals("foo", StringUtils.camelToKebab("foo"), "Returned string is not valid");
        assertEquals("foo-bar", StringUtils.camelToKebab("FooBar"), "Returned string is not valid");
        assertEquals("foo-bar", StringUtils.camelToKebab("fooBar"), "Returned string is not valid");
        assertEquals("foo-bar", StringUtils.camelToKebab("FOOBar"), "Returned string is not valid");
        assertEquals("a-bar", StringUtils.camelToKebab("ABar"), "Returned string is not valid");
        assertEquals("a-bar", StringUtils.camelToKebab("aBar"), "Returned string is not valid");
        assertEquals("a-bar", StringUtils.camelToKebab("aBAR"), "Returned string is not valid");
    }

    static Stream<Arguments> testFirst() {
        return Stream.of(
                arguments(CONTROLS_AND_SPACES, 0, CONTROLS_AND_SPACES.length(), -1),
                arguments("", 0, 0, -1),
                arguments("", 1, 0, -1),
                arguments("", -1, 1, -1),
                arguments("", 0, -1, -1),

                arguments("X", -1, 1, -1),
                arguments("X", 0, 1, -1),

                arguments("XY", 0, 2, -1),
                arguments("XY", 1, 2, -1),
                arguments("XY", 2, 2, -1),

                arguments("!", 0, 0, -1),
                arguments("!", 0, 1, 0),
                arguments("!", 1, 0, -1),
                arguments("!", 0, -1, -1),
                arguments("!", -1, 2, 0),

                arguments("X!", 0, 0, -1),
                arguments("X!", 0, 1, -1),
                arguments("X!", 0, 2, 1),
                arguments("X!", 1, 0, -1),
                arguments("X!", 1, 1, -1),
                arguments("X!", 1, 2, 1),
                arguments("X!", 2, 0, -1),
                arguments("X!", 2, 1, -1),
                arguments("X!", 2, 2, -1),
                arguments("X!", 1, 3, 1),
                arguments("X!", 2, 3, -1),

                arguments("!!!", 0, 3, 0),
                arguments("X!!", 0, 3, 1),
                arguments("XY!", 0, 3, 2),
                arguments("XYZ", 0, 3, -1),
                arguments("!YZ", 0, 3, 0),
                arguments("!!Z", 0, 3, 0),
                arguments("X!Z", 0, 3, 1),
                arguments("!Y!", 0, 3, 0));
    }

    @ParameterizedTest
    @MethodSource
    void testFirst(String text, int from, int to, int expected) {
        assertEquals(expected, StringUtils.first(text, from, to, (int x) -> x == '!'));
    }

    static Stream<Arguments> testLast() {
        return Stream.of(
                arguments(CONTROLS_AND_SPACES, 0, CONTROLS_AND_SPACES.length(), -1),
                arguments("", 0, 0, -1),
                arguments("", 1, 0, -1),
                arguments("", -1, 1, -1),
                arguments("", 0, -1, -1),

                arguments("X", -1, 1, -1),
                arguments("X", 0, 1, -1),

                arguments("XY", 0, 2, -1),
                arguments("XY", 1, 2, -1),
                arguments("XY", 2, 2, -1),

                arguments("!", 0, 0, -1),
                arguments("!", 0, 1, 0),
                arguments("!", 1, 0, -1),
                arguments("!", 0, -1, -1),
                arguments("!", -1, 2, 0),

                arguments("X!", 0, 0, -1),
                arguments("X!", 0, 1, -1),
                arguments("X!", 0, 2, 1),
                arguments("X!", 1, 0, -1),
                arguments("X!", 1, 1, -1),
                arguments("X!", 1, 2, 1),
                arguments("X!", 2, 0, -1),
                arguments("X!", 2, 1, -1),
                arguments("X!", 2, 2, -1),
                arguments("X!", 1, 3, 1),
                arguments("X!", 2, 3, -1),

                arguments("!!!", 0, 3, 2),
                arguments("X!!", 0, 3, 2),
                arguments("XY!", 0, 3, 2),
                arguments("XYZ", 0, 3, -1),
                arguments("!YZ", 0, 3, 0),
                arguments("!!Z", 0, 3, 1),
                arguments("X!Z", 0, 3, 1),
                arguments("!Y!", 0, 3, 2));
    }

    @ParameterizedTest
    @MethodSource
    void testLast(String text, int from, int to, int expected) {
        assertEquals(expected, StringUtils.last(text, from, to, (int x) -> x == '!'));
    }

    static Stream<Arguments> testFirstNonSpace() {
        return Stream.of(
                arguments(CONTROLS_AND_SPACES, 0, CONTROLS_AND_SPACES.length(), -1),
                arguments("", 0, 0, -1),
                arguments("", 1, 0, -1),
                arguments("", -1, 1, -1),
                arguments("", 0, -1, -1),
                arguments("X", -1, 1, 0),
                arguments("X", 0, 1, 0),
                arguments("XY", 0, 2, 0),
                arguments("XY", 1, 2, 1),
                arguments("XY", 2, 2, -1),
                arguments(" \b\t\r\nXY", 0, 0, -1),
                arguments(" \b\t\r\nXY", 0, 1, -1),
                arguments(" \b\t\r\nXY", 0, 2, -1),
                arguments(" \b\t\r\nXY", 0, 3, -1),
                arguments(" \b\t\r\nXY", 0, 4, -1),
                arguments(" \b\t\r\nXY", 0, 5, -1),
                arguments(" \b\t\r\nXY", 0, 6, 5),
                arguments(" \b\t\r\nXY", 0, 7, 5),
                arguments(" \b\t\r\nXY", 1, 7, 5),
                arguments(" \b\t\r\nXY", 2, 7, 5),
                arguments(" \b\t\r\nXY", 3, 7, 5),
                arguments(" \b\t\r\nXY", 4, 7, 5),
                arguments(" \b\t\r\nXY", 5, 7, 5),
                arguments(" \b\t\r\nXY", 6, 7, 6),
                arguments(" \b\t\r\nXY", 7, 7, -1),
                arguments(" \b\t\r\nXY", 7, 6, -1),
                arguments(" \b\t\r\nXY", 6, 6, -1),
                arguments(" \b\t\r\nXY", 6, 5, -1),
                arguments(" \b\t\r\nXY", 5, 4, -1),
                arguments(" \b\t\r\nXY", 4, 3, -1),
                arguments("   \b\t\r\n   ", 0, 10, -1),
                arguments("   \b\t\r\n   ", 1, 9, -1),
                arguments("   \b\t\r\n   ", -1, 11, -1),
                arguments("   \b\t\r\n   ", 1, 11, -1),
                arguments("   \b\t\r\n   ", -1, 9, -1),
                arguments("X  \b\t\r\n    Y", 1, 11, -1),
                arguments("X  \b\t\r\n    Y", 1, 12, 11),
                arguments("X  \b\t\r\n    Y", 0, 11, 0),
                arguments("X  \b\t\r\n    Y", 0, 0, -1),
                arguments("X  \b\t\r\n    Y", 12, 12, -1));
    }

    @ParameterizedTest
    @MethodSource
    void testFirstNonSpace(String text, int from, int to, int expected) {
        assertEquals(expected, StringUtils.firstNonSpace(text, from, to));
    }

    static Stream<Arguments> testLastNonSpace() {
        return Stream.of(
                arguments(CONTROLS_AND_SPACES, 0, CONTROLS_AND_SPACES.length(), -1),
                arguments("", 0, 0, -1),
                arguments("", 1, 0, -1),
                arguments("", -1, 1, -1),
                arguments("", 0, -1, -1),
                arguments("X", -1, 1, 0),
                arguments("X", 0, 1, 0),
                arguments("XY", 0, 2, 1),
                arguments("XY", 1, 2, 1),
                arguments("XY", 2, 2, -1),
                arguments("XY \b\t\r\n", 0, 0, -1),
                arguments("XY \b\t\r\n", 0, 1, 0),
                arguments("XY \b\t\r\n", 0, 2, 1),
                arguments("XY \b\t\r\n", 0, 3, 1),
                arguments("XY \b\t\r\n", 0, 4, 1),
                arguments("XY \b\t\r\n", 0, 5, 1),
                arguments("XY \b\t\r\n", 0, 6, 1),
                arguments("XY \b\t\r\n", 0, 7, 1),
                arguments("XY \b\t\r\n", 1, 7, 1),
                arguments("XY \b\t\r\n", 2, 7, -1),
                arguments("XY \b\t\r\n", 3, 7, -1),
                arguments("XY \b\t\r\n", 4, 7, -1),
                arguments("XY \b\t\r\n", 5, 7, -1),
                arguments("XY \b\t\r\n", 6, 7, -1),
                arguments("XY \b\t\r\n", 7, 7, -1),
                arguments("XY \b\t\r\n", 1, 5, 1),
                arguments("XY \b\t\r\n", 1, 2, 1),
                arguments("XY \b\t\r\n", 1, 1, -1),
                arguments("XY \b\t\r\n", 1, 0, -1),
                arguments("   \b\t\r\n   ", 0, 10, -1),
                arguments("   \b\t\r\n   ", 1, 9, -1),
                arguments("   \b\t\r\n   ", -1, 11, -1),
                arguments("   \b\t\r\n   ", 1, 11, -1),
                arguments("   \b\t\r\n   ", -1, 9, -1),
                arguments("X  \b\t\r\n    Y", 1, 11, -1),
                arguments("X  \b\t\r\n    Y", 1, 12, 11),
                arguments("X  \b\t\r\n    Y", 0, 11, 0),
                arguments("X  \b\t\r\n    Y", 0, 0, -1),
                arguments("X  \b\t\r\n    Y", 12, 12, -1));
    }

    @ParameterizedTest
    @MethodSource
    void testLastNonSpace(String text, int from, int to, int expected) {
        assertEquals(expected, StringUtils.lastNonSpace(text, from, to));
    }
}
