package org.openl.util;

import static org.junit.Assert.*;

import java.util.function.IntPredicate;
import java.util.regex.Pattern;

import org.junit.Test;

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
        assertArrayEquals("Returned array is not empty", new String[]{}, StringUtils.split("", '*'));
        assertArrayEquals("Returned array is not valid",
                new String[]{"a", "b", "c"},
                StringUtils.split("a.b.c", '.'));
        assertArrayEquals("Returned array is not valid",
                new String[]{"a", "b", "c"},
                StringUtils.split("a..b.c", '.'));
        assertArrayEquals("Returned array is not valid", new String[]{"a:b:c"}, StringUtils.split("a:b:c", '.'));
        assertArrayEquals("Returned array is not valid",
                new String[]{"a", "b", "c"},
                StringUtils.split("a b c", ' '));
        assertArrayEquals("Returned array is not valid",
                new String[]{"a", "b", "c"},
                StringUtils.split("a..b.c.", '.'));
        assertArrayEquals("Returned array is not valid",
                new String[]{"a", "b", "c"},
                StringUtils.split("..a..b.c..", '.'));
        assertArrayEquals("Returned array is not valid", new String[]{"a"}, StringUtils.split("a..", '.'));
        assertArrayEquals("Returned array is not valid", new String[]{"a"}, StringUtils.split("a.", '.'));
        assertArrayEquals("Returned array is not valid", new String[]{"a"}, StringUtils.split(".a", '.'));
        assertArrayEquals("Returned array is not valid", new String[]{"a"}, StringUtils.split("..a", '.'));
        assertArrayEquals("Returned array is not valid", new String[]{"a"}, StringUtils.split("..a.", '.'));
        assertArrayEquals("Returned array is not valid", new String[]{"a"}, StringUtils.split("..a..", '.'));

        assertArrayEquals("Returned array is not empty", new String[]{}, StringUtils.split(" \t\r\n", '*'));
        assertArrayEquals("Returned array is not empty",
                new String[]{},
                StringUtils.split(" \t\r\n *  * * \t\n", '*'));
        assertArrayEquals("Returned array is not valid",
                new String[]{"a", "b", "c"},
                StringUtils.split(" a .b .c ", '.'));
        assertArrayEquals("Returned array is not valid",
                new String[]{"a", "b", "c"},
                StringUtils.split(" a . . b . c ", '.'));
        assertArrayEquals("Returned array is not valid",
                new String[]{"a : b : c"},
                StringUtils.split(" a : b : c ", '.'));
        assertArrayEquals("Returned array is not valid",
                new String[]{"a", "b", "c"},
                StringUtils.split("a b \t\r\nc", ' '));
        assertArrayEquals("Returned array is not valid",
                new String[]{"a", "b", "c"},
                StringUtils.split("a. .b.c .", '.'));
        assertArrayEquals("Returned array is not valid",
                new String[]{"a", "b", "c"},
                StringUtils.split(". . a..b.c..", '.'));
        assertArrayEquals("Returned array is not valid", new String[]{"a"}, StringUtils.split("a\t..\n", '.'));
        assertArrayEquals("Returned array is not valid", new String[]{"a"}, StringUtils.split("a\t", '.'));
        assertArrayEquals("Returned array is not valid", new String[]{"a"}, StringUtils.split("\na", '.'));
        assertArrayEquals("Returned array is not valid", new String[]{"a"}, StringUtils.split("  a", '.'));
        assertArrayEquals("Returned array is not valid", new String[]{"a"}, StringUtils.split("  a ", '.'));
        assertArrayEquals("Returned array is not valid", new String[]{"a"}, StringUtils.split(". a. ", '.'));
    }

    @Test
    public void testSplitWS() {
        assertNull(StringUtils.split(null));
        assertArrayEquals("Returned array is not empty", new String[]{}, StringUtils.split(""));
        assertArrayEquals("Returned array is not empty", new String[]{}, StringUtils.split("  \n\r  \t \r\n  \t\t"));
        assertArrayEquals("Returned array is not valid", new String[]{"a", "b", "c"}, StringUtils.split("a b c"));
        assertArrayEquals("Returned array is not valid", new String[]{"a", "b", "c"}, StringUtils.split("a \tb\nc"));
        assertArrayEquals("Returned array is not valid", new String[]{"a:b:c"}, StringUtils.split("a:b:c"));
        assertArrayEquals("Returned array is not valid", new String[]{"a", "b", "c"}, StringUtils.split("a\tb\rc"));
        assertArrayEquals("Returned array is not valid",
                new String[]{"a", "b", "c"},
                StringUtils.split("a\n\nb c\n"));
        assertArrayEquals("Returned array is not valid",
                new String[]{"a", "b", "c"},
                StringUtils.split("\t\ta  b c  "));
        assertArrayEquals("Returned array is not valid", new String[]{"a"}, StringUtils.split("a  "));
        assertArrayEquals("Returned array is not valid", new String[]{"a"}, StringUtils.split("a "));
        assertArrayEquals("Returned array is not valid", new String[]{"a"}, StringUtils.split(" a"));
        assertArrayEquals("Returned array is not valid", new String[]{"a"}, StringUtils.split("  a"));
        assertArrayEquals("Returned array is not valid", new String[]{"a"}, StringUtils.split("  a "));
        assertArrayEquals("Returned array is not valid", new String[]{"a"}, StringUtils.split("\t a\n\r"));
    }

    @Test
    public void testToLines() {
        assertNull(StringUtils.toLines(null));
        assertNull(StringUtils.toLines(""));
        assertNull(StringUtils.toLines("\r\n\t "));
        assertArrayEquals("Returned array is not valid", new String[]{"a"}, StringUtils.toLines("a"));
        assertArrayEquals("Returned array is not valid", new String[]{"a"}, StringUtils.toLines("\r\n\t  a\r\n\t "));
        assertArrayEquals("Returned array is not valid", new String[]{"a", "b", "c"}, StringUtils.toLines("a\r\nb\r\nc"));
        assertArrayEquals("Returned array is not valid", new String[]{"a", "b", "c"}, StringUtils.toLines("\na\rb\nc\r"));
        assertArrayEquals("Returned array is not valid", new String[]{"a", "b", "c"}, StringUtils.toLines("\n\ra\r\tb \nc\n"));
        assertArrayEquals("Returned array is not valid", new String[]{"a", "c"}, StringUtils.toLines("\r a\n\t\r \n c \n"));
    }

    @Test
    public void testJoinObject() {
        assertNull("Returned string is not valid", StringUtils.join(null, "*"));
        assertEquals("Returned string is not valid", "", StringUtils.join(new Object[]{}, "*"));
        assertEquals("Returned string is not valid", "null", StringUtils.join(new Object[]{null}, "*"));
        assertEquals("Returned string is not valid", "null,null", StringUtils.join(new Object[]{null, null}, ","));
        assertEquals("Returned string is not valid", "a--b--c", StringUtils.join(new Object[]{"a", "b", "c"}, "--"));
        assertEquals("Returned string is not valid", "null,,a", StringUtils.join(new Object[]{null, "", "a"}, ","));
    }

    @Test
    public void testIsEmpty() {
        assertTrue("Returned value is false", StringUtils.isEmpty(null));
        assertTrue("Returned value is false", StringUtils.isEmpty(""));
        assertFalse("Returned value is true", StringUtils.isEmpty(" "));
        assertFalse("Returned value is true", StringUtils.isEmpty("boo"));
        assertFalse("Returned value is true", StringUtils.isEmpty("  boo  "));
    }

    @Test
    public void testIsNotEmpty() {
        assertFalse("Returned value is true", StringUtils.isNotEmpty(null));
        assertFalse("Returned value is true", StringUtils.isNotEmpty(""));
        assertTrue("Returned value is false", StringUtils.isNotEmpty(" "));
        assertTrue("Returned value is false", StringUtils.isNotEmpty("boo"));
        assertTrue("Returned value is false", StringUtils.isNotEmpty("  boo  "));
    }

    @Test
    public void testIsBlank() {
        assertTrue("Returned value is false", StringUtils.isBlank(null));
        assertTrue("Returned value is false", StringUtils.isBlank(""));
        assertTrue("Returned value is false", StringUtils.isBlank(" "));
        assertFalse("Returned value is true", StringUtils.isBlank("boo"));
        assertFalse("Returned value is true", StringUtils.isBlank("  boo  "));
        assertTrue("Returned value is true", StringUtils.isBlank(CONTROLS_AND_SPACES));
    }

    @Test
    public void testIsNotBlank() {
        assertFalse("Returned value is true", StringUtils.isNotBlank(null));
        assertFalse("Returned value is true", StringUtils.isNotBlank(""));
        assertFalse("Returned value is true", StringUtils.isNotBlank(" "));
        assertTrue("Returned value is false", StringUtils.isNotBlank("boo"));
        assertTrue("Returned value is false", StringUtils.isNotBlank("  boo  "));
        assertFalse("Returned value is false", StringUtils.isNotBlank(CONTROLS_AND_SPACES));
    }

    @Test
    public void testContainsIgnoreCase() {
        assertFalse("Returned value is true", StringUtils.containsIgnoreCase(null, ""));
        assertFalse("Returned value is true", StringUtils.containsIgnoreCase("", null));
        assertFalse("Returned value is true", StringUtils.containsIgnoreCase("abc", "z"));
        assertFalse("Returned value is true", StringUtils.containsIgnoreCase("абя", "в"));
        assertFalse("Returned value is true", StringUtils.containsIgnoreCase("abc", "Z"));

        assertTrue("Returned value is false", StringUtils.containsIgnoreCase("", ""));
        assertTrue("Returned value is false", StringUtils.containsIgnoreCase("abc", ""));
        assertTrue("Returned value is false", StringUtils.containsIgnoreCase("abc", "a"));
        assertTrue("Returned value is false", StringUtils.containsIgnoreCase("абв", "б"));
        assertTrue("Returned value is false", StringUtils.containsIgnoreCase("abc", "B"));
    }

    @Test
    public void testMatches() {
        assertFalse("Returned value is true", StringUtils.matches(Pattern.compile("\\d"), ""));
        assertTrue("Returned value is true", StringUtils.matches(Pattern.compile("\\d"), "1"));
        assertTrue("Returned value is true", StringUtils.matches(Pattern.compile("\\d"), "2"));
        assertFalse("Returned value is true", StringUtils.matches(Pattern.compile("\\d"), "12"));
    }

    @Test
    public void testTrim() {
        assertNull("Returned string is not valid", StringUtils.trim(null));
        assertEquals("Returned string is not valid", "", StringUtils.trim(""));
        assertEquals("Returned string is not valid", "", StringUtils.trim("     "));
        assertEquals("Returned string is not valid", "boo", StringUtils.trim("boo"));
        assertEquals("Returned string is not valid", "boo", StringUtils.trim("    boo    "));
        assertEquals("Returned string is not valid", "bar", StringUtils.trim("     bar     "));
        assertEquals("Returned string is not valid", "foo", StringUtils.trim(CONTROLS_AND_SPACES + "  foo" + CONTROLS_AND_SPACES + "   "));
        assertEquals("Returned string is not valid", "", StringUtils.trim(CONTROLS_AND_SPACES));
    }

    @Test
    public void testTrimToNull() {
        assertNull("Returned string is not valid", StringUtils.trimToNull(null));
        assertNull("Returned string is not valid", StringUtils.trimToNull(""));
        assertNull("Returned string is not valid", StringUtils.trimToNull("     "));
        assertEquals("Returned string is not valid", "boo", StringUtils.trimToNull("boo"));
        assertEquals("Returned string is not valid", "boo", StringUtils.trimToNull("    boo    "));
        assertEquals("Returned string is not valid", "bar", StringUtils.trimToNull("     bar     "));
        assertEquals("Returned string is not valid", "foo", StringUtils.trimToNull(CONTROLS_AND_SPACES + "  foo" + CONTROLS_AND_SPACES + "   "));
        assertNull("Returned string is not valid", StringUtils.trimToNull(CONTROLS_AND_SPACES));
    }

    @Test
    public void testTrimToEmpty() {
        assertEquals("Returned string is not valid", "", StringUtils.trimToEmpty(null));
        assertEquals("Returned string is not valid", "", StringUtils.trimToEmpty(""));
        assertEquals("Returned string is not valid", "", StringUtils.trimToEmpty("     "));
        assertEquals("Returned string is not valid", "boo", StringUtils.trimToEmpty("boo"));
        assertEquals("Returned string is not valid", "boo", StringUtils.trimToEmpty("    boo    "));
        assertEquals("Returned string is not valid", "bar", StringUtils.trimToEmpty("     bar     "));
        assertEquals("Returned string is not valid", "foo", StringUtils.trimToEmpty(CONTROLS_AND_SPACES + "  foo" + CONTROLS_AND_SPACES + "   "));
        assertEquals("Returned string is not valid", "", StringUtils.trimToEmpty(CONTROLS_AND_SPACES));
    }

    @Test
    public void testCapitalize() {
        assertNull("Returned string is not valid", StringUtils.capitalize(null));
        assertEquals("Returned string is not valid", "", StringUtils.capitalize(""));
        assertEquals("Returned string is not valid", "Foo", StringUtils.capitalize("foo"));
        assertEquals("Returned string is not valid", "FOo", StringUtils.capitalize("fOo"));
        assertEquals("Returned string is not valid", "МУу", StringUtils.capitalize("мУу"));
    }

    @Test
    public void testUnCapitalize() {
        assertNull("Returned string is not valid", StringUtils.uncapitalize(null));
        assertEquals("Returned string is not valid", "", StringUtils.uncapitalize(""));
        assertEquals("Returned string is not valid", "foo", StringUtils.uncapitalize("Foo"));
        assertEquals("Returned string is not valid", "fOO", StringUtils.uncapitalize("FOO"));
        assertEquals("Returned string is not valid", "муУ", StringUtils.uncapitalize("МуУ"));
    }

    @Test
    public void testCamelToKebab() {
        assertNull("Returned string is not valid", StringUtils.camelToKebab(null));
        assertEquals("Returned string is not valid", "", StringUtils.camelToKebab(""));
        assertEquals("Returned string is not valid", "foo", StringUtils.camelToKebab("FOO"));
        assertEquals("Returned string is not valid", "foo", StringUtils.camelToKebab("Foo"));
        assertEquals("Returned string is not valid", "foo", StringUtils.camelToKebab("foo"));
        assertEquals("Returned string is not valid", "foo-bar", StringUtils.camelToKebab("FooBar"));
        assertEquals("Returned string is not valid", "foo-bar", StringUtils.camelToKebab("fooBar"));
        assertEquals("Returned string is not valid", "foo-bar", StringUtils.camelToKebab("FOOBar"));
        assertEquals("Returned string is not valid", "a-bar", StringUtils.camelToKebab("ABar"));
        assertEquals("Returned string is not valid", "a-bar", StringUtils.camelToKebab("aBar"));
        assertEquals("Returned string is not valid", "a-bar", StringUtils.camelToKebab("aBAR"));
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
