package org.openl.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.IntPredicate;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * Created by tsaltsevich on 5/3/2016.
 */
public class StringUtilsTest {

    //All possible combinations of hidden UTF-8 symbols
    private static final String CONTROLS_AND_SPACES = "\u0000\u0001\u0002\u0003\u0004\u0005\u0006\u0007\u0008\u0009" +
            "\u000B\u000C\u000E\u000F\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017\u0018\u0019\u001A\u001B\u001C" +
            "\u001D\u001E\u001F\u0020\u007F\u0080\u0081\u0082\u0083\u0084\u0085\u0086\u0087\u0088\u0089\u008A\u008B" +
            "\u008C\u008D\u008E\u008F\u0090\u0091\u0092\u0093\u0094\u0095\u0096\u0097\u0098\u0099\u009A\u009B\u009C" +
            "\u009D\u009E\u009F\u00A0\u2007\u202F\r\n\t\b\f";

    @Test
    public void testSplit() {
        assertNull(StringUtils.split(null, ' '));
        assertNull(StringUtils.split(null, '*'));
        assertArrayEquals(new String[]{}, StringUtils.split("", '*'), "Returned array is not empty");
        assertArrayEquals(new String[]{"a", "b", "c"},
                StringUtils.split("a.b.c", '.'),
                "Returned array is not valid");
        assertArrayEquals(new String[]{"a", "b", "c"},
                StringUtils.split("a..b.c", '.'),
                "Returned array is not valid");
        assertArrayEquals(new String[]{"a:b:c"}, StringUtils.split("a:b:c", '.'), "Returned array is not valid");
        assertArrayEquals(new String[]{"a", "b", "c"},
                StringUtils.split("a b c", ' '),
                "Returned array is not valid");
        assertArrayEquals(new String[]{"a", "b", "c"},
                StringUtils.split("a..b.c.", '.'),
                "Returned array is not valid");
        assertArrayEquals(new String[]{"a", "b", "c"},
                StringUtils.split("..a..b.c..", '.'),
                "Returned array is not valid");
        assertArrayEquals(new String[]{"a"}, StringUtils.split("a..", '.'), "Returned array is not valid");
        assertArrayEquals(new String[]{"a"}, StringUtils.split("a.", '.'), "Returned array is not valid");
        assertArrayEquals(new String[]{"a"}, StringUtils.split(".a", '.'), "Returned array is not valid");
        assertArrayEquals(new String[]{"a"}, StringUtils.split("..a", '.'), "Returned array is not valid");
        assertArrayEquals(new String[]{"a"}, StringUtils.split("..a.", '.'), "Returned array is not valid");
        assertArrayEquals(new String[]{"a"}, StringUtils.split("..a..", '.'), "Returned array is not valid");

        assertArrayEquals(new String[]{}, StringUtils.split(" \t\r\n", '*'), "Returned array is not empty");
        assertArrayEquals(new String[]{},
                StringUtils.split(" \t\r\n *  * * \t\n", '*'),
                "Returned array is not empty");
        assertArrayEquals(new String[]{"a", "b", "c"},
                StringUtils.split(" a .b .c ", '.'),
                "Returned array is not valid");
        assertArrayEquals(new String[]{"a", "b", "c"},
                StringUtils.split(" a . . b . c ", '.'),
                "Returned array is not valid");
        assertArrayEquals(new String[]{"a : b : c"},
                StringUtils.split(" a : b : c ", '.'),
                "Returned array is not valid");
        assertArrayEquals(new String[]{"a", "b", "c"},
                StringUtils.split("a b \t\r\nc", ' '),
                "Returned array is not valid");
        assertArrayEquals(new String[]{"a", "b", "c"},
                StringUtils.split("a. .b.c .", '.'),
                "Returned array is not valid");
        assertArrayEquals(new String[]{"a", "b", "c"},
                StringUtils.split(". . a..b.c..", '.'),
                "Returned array is not valid");
        assertArrayEquals(new String[]{"a"}, StringUtils.split("a\t..\n", '.'), "Returned array is not valid");
        assertArrayEquals(new String[]{"a"}, StringUtils.split("a\t", '.'), "Returned array is not valid");
        assertArrayEquals(new String[]{"a"}, StringUtils.split("\na", '.'), "Returned array is not valid");
        assertArrayEquals(new String[]{"a"}, StringUtils.split("  a", '.'), "Returned array is not valid");
        assertArrayEquals(new String[]{"a"}, StringUtils.split("  a ", '.'), "Returned array is not valid");
        assertArrayEquals(new String[]{"a"}, StringUtils.split(". a. ", '.'), "Returned array is not valid");
    }

    @Test
    public void testSplitWS() {
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
    public void testToLines() {
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
    public void testJoinObject() {
        assertNull(StringUtils.join(null, "*"), "Returned string is not valid");
        assertEquals("", StringUtils.join(new Object[]{}, "*"), "Returned string is not valid");
        assertEquals("null", StringUtils.join(new Object[]{null}, "*"), "Returned string is not valid");
        assertEquals("null,null", StringUtils.join(new Object[]{null, null}, ","), "Returned string is not valid");
        assertEquals("a--b--c", StringUtils.join(new Object[]{"a", "b", "c"}, "--"), "Returned string is not valid");
        assertEquals("null,,a", StringUtils.join(new Object[]{null, "", "a"}, ","), "Returned string is not valid");
    }

    @Test
    public void testIsEmpty() {
        assertTrue(StringUtils.isEmpty(null), "Returned value is false");
        assertTrue(StringUtils.isEmpty(""), "Returned value is false");
        assertFalse(StringUtils.isEmpty(" "), "Returned value is true");
        assertFalse(StringUtils.isEmpty("boo"), "Returned value is true");
        assertFalse(StringUtils.isEmpty("  boo  "), "Returned value is true");
    }

    @Test
    public void testIsNotEmpty() {
        assertFalse(StringUtils.isNotEmpty(null), "Returned value is true");
        assertFalse(StringUtils.isNotEmpty(""), "Returned value is true");
        assertTrue(StringUtils.isNotEmpty(" "), "Returned value is false");
        assertTrue(StringUtils.isNotEmpty("boo"), "Returned value is false");
        assertTrue(StringUtils.isNotEmpty("  boo  "), "Returned value is false");
    }

    @Test
    public void testIsBlank() {
        assertTrue(StringUtils.isBlank(null), "Returned value is false");
        assertTrue(StringUtils.isBlank(""), "Returned value is false");
        assertTrue(StringUtils.isBlank(" "), "Returned value is false");
        assertFalse(StringUtils.isBlank("boo"), "Returned value is true");
        assertFalse(StringUtils.isBlank("  boo  "), "Returned value is true");
        assertTrue(StringUtils.isBlank(CONTROLS_AND_SPACES), "Returned value is true");
    }

    @Test
    public void testIsNotBlank() {
        assertFalse(StringUtils.isNotBlank(null), "Returned value is true");
        assertFalse(StringUtils.isNotBlank(""), "Returned value is true");
        assertFalse(StringUtils.isNotBlank(" "), "Returned value is true");
        assertTrue(StringUtils.isNotBlank("boo"), "Returned value is false");
        assertTrue(StringUtils.isNotBlank("  boo  "), "Returned value is false");
        assertFalse(StringUtils.isNotBlank(CONTROLS_AND_SPACES), "Returned value is false");
    }

    @Test
    public void testContainsIgnoreCase() {
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
    public void testMatches() {
        assertFalse(StringUtils.matches(Pattern.compile("\\d"), ""), "Returned value is true");
        assertTrue(StringUtils.matches(Pattern.compile("\\d"), "1"), "Returned value is true");
        assertTrue(StringUtils.matches(Pattern.compile("\\d"), "2"), "Returned value is true");
        assertFalse(StringUtils.matches(Pattern.compile("\\d"), "12"), "Returned value is true");
    }

    @Test
    public void testTrim() {
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
    public void testTrimToNull() {
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
    public void testTrimToEmpty() {
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
    public void testCapitalize() {
        assertNull(StringUtils.capitalize(null), "Returned string is not valid");
        assertEquals("", StringUtils.capitalize(""), "Returned string is not valid");
        assertEquals("Foo", StringUtils.capitalize("foo"), "Returned string is not valid");
        assertEquals("FOo", StringUtils.capitalize("fOo"), "Returned string is not valid");
        assertEquals("МУу", StringUtils.capitalize("мУу"), "Returned string is not valid");
    }

    @Test
    public void testUnCapitalize() {
        assertNull(StringUtils.uncapitalize(null), "Returned string is not valid");
        assertEquals("", StringUtils.uncapitalize(""), "Returned string is not valid");
        assertEquals("foo", StringUtils.uncapitalize("Foo"), "Returned string is not valid");
        assertEquals("fOO", StringUtils.uncapitalize("FOO"), "Returned string is not valid");
        assertEquals("муУ", StringUtils.uncapitalize("МуУ"), "Returned string is not valid");
    }

    @Test
    public void testCamelToKebab() {
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

    @Test
    public void testFirst() {
        IntPredicate tester = (int x) -> x == '!';
        assertEquals( StringUtils.first(CONTROLS_AND_SPACES,0,CONTROLS_AND_SPACES.length(), tester), -1);
        assertEquals( StringUtils.first("",0,0, tester), -1);
        assertEquals( StringUtils.first("",1,0, tester), -1);
        assertEquals( StringUtils.first("",-1,1, tester), -1);
        assertEquals( StringUtils.first("",0,-1, tester), -1);

        assertEquals( StringUtils.first("X",-1,1, tester), -1);
        assertEquals( StringUtils.first("X",0,1, tester), -1);

        assertEquals( StringUtils.first("XY",0,2, tester), -1);
        assertEquals( StringUtils.first("XY",1,2, tester), -1);
        assertEquals( StringUtils.first("XY",2,2, tester), -1);

        assertEquals( StringUtils.first("!",0,0, tester), -1);
        assertEquals( StringUtils.first("!",0,1, tester), 0);
        assertEquals( StringUtils.first("!",1,0, tester), -1);
        assertEquals( StringUtils.first("!",0,-1, tester), -1);
        assertEquals( StringUtils.first("!",-1,2, tester), 0);

        assertEquals( StringUtils.first("X!",0,0, tester), -1);
        assertEquals( StringUtils.first("X!",0,1, tester), -1);
        assertEquals( StringUtils.first("X!",0,2, tester), 1);
        assertEquals( StringUtils.first("X!",1,0, tester), -1);
        assertEquals( StringUtils.first("X!",1,1, tester), -1);
        assertEquals( StringUtils.first("X!",1,2, tester), 1);
        assertEquals( StringUtils.first("X!",2,0, tester), -1);
        assertEquals( StringUtils.first("X!",2,1, tester), -1);
        assertEquals( StringUtils.first("X!",2,2, tester), -1);
        assertEquals( StringUtils.first("X!",1,3, tester), 1);
        assertEquals( StringUtils.first("X!",2,3, tester), -1);

        assertEquals( StringUtils.first("!!!",0,3, tester), 0);
        assertEquals( StringUtils.first("X!!",0,3, tester), 1);
        assertEquals( StringUtils.first("XY!",0,3, tester), 2);
        assertEquals( StringUtils.first("XYZ",0,3, tester), -1);
        assertEquals( StringUtils.first("!YZ",0,3, tester), 0);
        assertEquals( StringUtils.first("!!Z",0,3, tester), 0);
        assertEquals( StringUtils.first("X!Z",0,3, tester), 1);
        assertEquals( StringUtils.first("!Y!",0,3, tester), 0);
    }

    @Test
    public void testLast() {
        IntPredicate tester = (int x) -> x == '!';
        assertEquals( StringUtils.last(CONTROLS_AND_SPACES,0,CONTROLS_AND_SPACES.length(), tester), -1);
        assertEquals( StringUtils.last("",0,0, tester), -1);
        assertEquals( StringUtils.last("",1,0, tester), -1);
        assertEquals( StringUtils.last("",-1,1, tester), -1);
        assertEquals( StringUtils.last("",0,-1, tester), -1);

        assertEquals( StringUtils.last("X",-1,1, tester), -1);
        assertEquals( StringUtils.last("X",0,1, tester), -1);

        assertEquals( StringUtils.last("XY",0,2, tester), -1);
        assertEquals( StringUtils.last("XY",1,2, tester), -1);
        assertEquals( StringUtils.last("XY",2,2, tester), -1);

        assertEquals( StringUtils.last("!",0,0, tester), -1);
        assertEquals( StringUtils.last("!",0,1, tester), 0);
        assertEquals( StringUtils.last("!",1,0, tester), -1);
        assertEquals( StringUtils.last("!",0,-1, tester), -1);
        assertEquals( StringUtils.last("!",-1,2, tester), 0);

        assertEquals( StringUtils.last("X!",0,0, tester), -1);
        assertEquals( StringUtils.last("X!",0,1, tester), -1);
        assertEquals( StringUtils.last("X!",0,2, tester), 1);
        assertEquals( StringUtils.last("X!",1,0, tester), -1);
        assertEquals( StringUtils.last("X!",1,1, tester), -1);
        assertEquals( StringUtils.last("X!",1,2, tester), 1);
        assertEquals( StringUtils.last("X!",2,0, tester), -1);
        assertEquals( StringUtils.last("X!",2,1, tester), -1);
        assertEquals( StringUtils.last("X!",2,2, tester), -1);
        assertEquals( StringUtils.last("X!",1,3, tester), 1);
        assertEquals( StringUtils.last("X!",2,3, tester), -1);

        assertEquals( StringUtils.last("!!!",0,3, tester), 2);
        assertEquals( StringUtils.last("X!!",0,3, tester), 2);
        assertEquals( StringUtils.last("XY!",0,3, tester), 2);
        assertEquals( StringUtils.last("XYZ",0,3, tester), -1);
        assertEquals( StringUtils.last("!YZ",0,3, tester), 0);
        assertEquals( StringUtils.last("!!Z",0,3, tester), 1);
        assertEquals( StringUtils.last("X!Z",0,3, tester), 1);
        assertEquals( StringUtils.last("!Y!",0,3, tester), 2);
    }

    @Test
    public void testFirstNonSpace() {
        assertEquals( StringUtils.firstNonSpace(CONTROLS_AND_SPACES,0,CONTROLS_AND_SPACES.length()), -1);
        assertEquals( StringUtils.firstNonSpace("",0,0), -1);
        assertEquals( StringUtils.firstNonSpace("",1,0), -1);
        assertEquals( StringUtils.firstNonSpace("",-1,1), -1);
        assertEquals( StringUtils.firstNonSpace("",0,-1), -1);
        assertEquals( StringUtils.firstNonSpace("X",-1,1), 0);
        assertEquals( StringUtils.firstNonSpace("X",0,1), 0);
        assertEquals( StringUtils.firstNonSpace("XY",0,2), 0);
        assertEquals( StringUtils.firstNonSpace("XY",1,2), 1);
        assertEquals( StringUtils.firstNonSpace("XY",2,2), -1);
        assertEquals( StringUtils.firstNonSpace(" \b\t\r\nXY",0,0), -1);
        assertEquals( StringUtils.firstNonSpace(" \b\t\r\nXY",0,1), -1);
        assertEquals( StringUtils.firstNonSpace(" \b\t\r\nXY",0,2), -1);
        assertEquals( StringUtils.firstNonSpace(" \b\t\r\nXY",0,3), -1);
        assertEquals( StringUtils.firstNonSpace(" \b\t\r\nXY",0,4), -1);
        assertEquals( StringUtils.firstNonSpace(" \b\t\r\nXY",0,5), -1);
        assertEquals( StringUtils.firstNonSpace(" \b\t\r\nXY",0,6), 5);
        assertEquals( StringUtils.firstNonSpace(" \b\t\r\nXY",0,7), 5);
        assertEquals( StringUtils.firstNonSpace(" \b\t\r\nXY",1,7), 5);
        assertEquals( StringUtils.firstNonSpace(" \b\t\r\nXY",2,7), 5);
        assertEquals( StringUtils.firstNonSpace(" \b\t\r\nXY",3,7), 5);
        assertEquals( StringUtils.firstNonSpace(" \b\t\r\nXY",4,7), 5);
        assertEquals( StringUtils.firstNonSpace(" \b\t\r\nXY",5,7), 5);
        assertEquals( StringUtils.firstNonSpace(" \b\t\r\nXY",6,7), 6);
        assertEquals( StringUtils.firstNonSpace(" \b\t\r\nXY",7,7), -1);
        assertEquals( StringUtils.firstNonSpace(" \b\t\r\nXY",7,6), -1);
        assertEquals( StringUtils.firstNonSpace(" \b\t\r\nXY",6,6), -1);
        assertEquals( StringUtils.firstNonSpace(" \b\t\r\nXY",6,5), -1);
        assertEquals( StringUtils.firstNonSpace(" \b\t\r\nXY",5,4), -1);
        assertEquals( StringUtils.firstNonSpace(" \b\t\r\nXY",4,3), -1);
        assertEquals( StringUtils.firstNonSpace("   \b\t\r\n   ",0,10), -1);
        assertEquals( StringUtils.firstNonSpace("   \b\t\r\n   ",1,9), -1);
        assertEquals( StringUtils.firstNonSpace("   \b\t\r\n   ",-1,11), -1);
        assertEquals( StringUtils.firstNonSpace("   \b\t\r\n   ",1,11), -1);
        assertEquals( StringUtils.firstNonSpace("   \b\t\r\n   ",-1,9), -1);
        assertEquals( StringUtils.firstNonSpace("X  \b\t\r\n    Y",1,11), -1);
        assertEquals( StringUtils.firstNonSpace("X  \b\t\r\n    Y",1,12), 11);
        assertEquals( StringUtils.firstNonSpace("X  \b\t\r\n    Y",0,11), 0);
        assertEquals( StringUtils.firstNonSpace("X  \b\t\r\n    Y",0,0), -1);
        assertEquals( StringUtils.firstNonSpace("X  \b\t\r\n    Y",12,12), -1);
    }

    @Test
    public void testLastNonSpace() {
        assertEquals( StringUtils.lastNonSpace(CONTROLS_AND_SPACES,0,CONTROLS_AND_SPACES.length()), -1);
        assertEquals( StringUtils.lastNonSpace("",0,0), -1);
        assertEquals( StringUtils.lastNonSpace("",1,0), -1);
        assertEquals( StringUtils.lastNonSpace("",-1,1), -1);
        assertEquals( StringUtils.lastNonSpace("",0,-1), -1);
        assertEquals( StringUtils.lastNonSpace("X",-1,1), 0);
        assertEquals( StringUtils.lastNonSpace("X",0,1), 0);
        assertEquals( StringUtils.lastNonSpace("XY",0,2), 1);
        assertEquals( StringUtils.lastNonSpace("XY",1,2), 1);
        assertEquals( StringUtils.lastNonSpace("XY",2,2), -1);
        assertEquals( StringUtils.lastNonSpace("XY \b\t\r\n",0,0), -1);
        assertEquals( StringUtils.lastNonSpace("XY \b\t\r\n",0,1), 0);
        assertEquals( StringUtils.lastNonSpace("XY \b\t\r\n",0,2), 1);
        assertEquals( StringUtils.lastNonSpace("XY \b\t\r\n",0,3), 1);
        assertEquals( StringUtils.lastNonSpace("XY \b\t\r\n",0,4), 1);
        assertEquals( StringUtils.lastNonSpace("XY \b\t\r\n",0,5), 1);
        assertEquals( StringUtils.lastNonSpace("XY \b\t\r\n",0,6), 1);
        assertEquals( StringUtils.lastNonSpace("XY \b\t\r\n",0,7), 1);
        assertEquals( StringUtils.lastNonSpace("XY \b\t\r\n",1,7), 1);
        assertEquals( StringUtils.lastNonSpace("XY \b\t\r\n",2,7), -1);
        assertEquals( StringUtils.lastNonSpace("XY \b\t\r\n",3,7), -1);
        assertEquals( StringUtils.lastNonSpace("XY \b\t\r\n",4,7), -1);
        assertEquals( StringUtils.lastNonSpace("XY \b\t\r\n",5,7), -1);
        assertEquals( StringUtils.lastNonSpace("XY \b\t\r\n",6,7), -1);
        assertEquals( StringUtils.lastNonSpace("XY \b\t\r\n",7,7), -1);
        assertEquals( StringUtils.lastNonSpace("XY \b\t\r\n",1,5), 1);
        assertEquals( StringUtils.lastNonSpace("XY \b\t\r\n",1,2), 1);
        assertEquals( StringUtils.lastNonSpace("XY \b\t\r\n",1,1), -1);
        assertEquals( StringUtils.lastNonSpace("XY \b\t\r\n",1,0), -1);
        assertEquals( StringUtils.lastNonSpace("   \b\t\r\n   ",0,10), -1);
        assertEquals( StringUtils.lastNonSpace("   \b\t\r\n   ",1,9), -1);
        assertEquals( StringUtils.lastNonSpace("   \b\t\r\n   ",-1,11), -1);
        assertEquals( StringUtils.lastNonSpace("   \b\t\r\n   ",1,11), -1);
        assertEquals( StringUtils.lastNonSpace("   \b\t\r\n   ",-1,9), -1);
        assertEquals( StringUtils.lastNonSpace("X  \b\t\r\n    Y",1,11), -1);
        assertEquals( StringUtils.lastNonSpace("X  \b\t\r\n    Y",1,12), 11);
        assertEquals( StringUtils.lastNonSpace("X  \b\t\r\n    Y",0,11), 0);
        assertEquals( StringUtils.lastNonSpace("X  \b\t\r\n    Y",0,0), -1);
        assertEquals( StringUtils.lastNonSpace("X  \b\t\r\n    Y",12,12), -1);
    }
}
