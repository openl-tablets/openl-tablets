package org.openl.util;

import static org.junit.Assert.*;

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
            "\u009D\u009E\u009F\u00A0\u2007\u202F";

    @Test
    public void testSplit() throws Exception {
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
    public void testSplitWS() throws Exception {
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
    public void testJoinObject() throws Exception {
        assertEquals("Returned string is not valid", null, StringUtils.join(null, "*"));
        assertEquals("Returned string is not valid", "", StringUtils.join(new Object[]{}, "*"));
        assertEquals("Returned string is not valid", "null", StringUtils.join(new Object[]{null}, "*"));
        assertEquals("Returned string is not valid", "null,null", StringUtils.join(new Object[]{null, null}, ","));
        assertEquals("Returned string is not valid", "a--b--c", StringUtils.join(new Object[]{"a", "b", "c"}, "--"));
        assertEquals("Returned string is not valid", "null,,a", StringUtils.join(new Object[]{null, "", "a"}, ","));
    }

    @Test
    public void testIsEmpty() throws Exception {
        assertTrue("Returned value is false", StringUtils.isEmpty(null));
        assertTrue("Returned value is false", StringUtils.isEmpty(""));
        assertFalse("Returned value is true", StringUtils.isEmpty(" "));
        assertFalse("Returned value is true", StringUtils.isEmpty("boo"));
        assertFalse("Returned value is true", StringUtils.isEmpty("  boo  "));
    }

    @Test
    public void testIsNotEmpty() throws Exception {
        assertFalse("Returned value is true", StringUtils.isNotEmpty(null));
        assertFalse("Returned value is true", StringUtils.isNotEmpty(""));
        assertTrue("Returned value is false", StringUtils.isNotEmpty(" "));
        assertTrue("Returned value is false", StringUtils.isNotEmpty("boo"));
        assertTrue("Returned value is false", StringUtils.isNotEmpty("  boo  "));
    }

    @Test
    public void testIsBlank() throws Exception {
        assertTrue("Returned value is false", StringUtils.isBlank(null));
        assertTrue("Returned value is false", StringUtils.isBlank(""));
        assertTrue("Returned value is false", StringUtils.isBlank(" "));
        assertFalse("Returned value is true", StringUtils.isBlank("boo"));
        assertFalse("Returned value is true", StringUtils.isBlank("  boo  "));
    }

    @Test
    public void testIsNotBlank() throws Exception {
        assertFalse("Returned value is true", StringUtils.isNotBlank(null));
        assertFalse("Returned value is true", StringUtils.isNotBlank(""));
        assertFalse("Returned value is true", StringUtils.isNotBlank(" "));
        assertTrue("Returned value is false", StringUtils.isNotBlank("boo"));
        assertTrue("Returned value is false", StringUtils.isNotBlank("  boo  "));
    }

    @Test
    public void testContainsIgnoreCase() throws Exception {
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
    public void testMatches() throws Exception {
        assertFalse("Returned value is true", StringUtils.matches(Pattern.compile("\\d"), ""));
        assertTrue("Returned value is true", StringUtils.matches(Pattern.compile("\\d"), "1"));
        assertTrue("Returned value is true", StringUtils.matches(Pattern.compile("\\d"), "2"));
        assertFalse("Returned value is true", StringUtils.matches(Pattern.compile("\\d"), "12"));
    }

    @Test
    public void testTrim() throws Exception {
        assertEquals("Returned string is not valid", null, StringUtils.trim(null));
        assertEquals("Returned string is not valid", "", StringUtils.trim(""));
        assertEquals("Returned string is not valid", "", StringUtils.trim("     "));
        assertEquals("Returned string is not valid", "boo", StringUtils.trim("boo"));
        assertEquals("Returned string is not valid", "boo", StringUtils.trim("    boo    "));
        assertEquals("Returned string is not valid", "bar", StringUtils.trim("     bar     "));
        assertEquals("Returned string is not valid", "foo", StringUtils.trim(CONTROLS_AND_SPACES + "  foo" + CONTROLS_AND_SPACES + "   "));
        assertEquals("Returned string is not valid", "", StringUtils.trim(CONTROLS_AND_SPACES));
    }

    @Test
    public void testTrimToNull() throws Exception {
        assertEquals("Returned string is not valid", null, StringUtils.trimToNull(null));
        assertEquals("Returned string is not valid", null, StringUtils.trimToNull(""));
        assertEquals("Returned string is not valid", null, StringUtils.trimToNull("     "));
        assertEquals("Returned string is not valid", "boo", StringUtils.trimToNull("boo"));
        assertEquals("Returned string is not valid", "boo", StringUtils.trimToNull("    boo    "));
        assertEquals("Returned string is not valid", "bar", StringUtils.trimToNull("     bar     "));
        assertEquals("Returned string is not valid", "foo", StringUtils.trimToNull(CONTROLS_AND_SPACES + "  foo" + CONTROLS_AND_SPACES + "   "));
        assertEquals("Returned string is not valid", null, StringUtils.trimToNull(CONTROLS_AND_SPACES));
    }

    @Test
    public void testTrimToEmpty() throws Exception {
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
    public void testCapitalize() throws Exception {
        assertEquals("Returned string is not valid", null, StringUtils.capitalize(null));
        assertEquals("Returned string is not valid", "", StringUtils.capitalize(""));
        assertEquals("Returned string is not valid", "Foo", StringUtils.capitalize("foo"));
        assertEquals("Returned string is not valid", "FOo", StringUtils.capitalize("fOo"));
        assertEquals("Returned string is not valid", "МУу", StringUtils.capitalize("мУу"));
    }

    @Test
    public void testUnCapitalize() throws Exception {
        assertEquals("Returned string is not valid", null, StringUtils.uncapitalize(null));
        assertEquals("Returned string is not valid", "", StringUtils.uncapitalize(""));
        assertEquals("Returned string is not valid", "foo", StringUtils.uncapitalize("Foo"));
        assertEquals("Returned string is not valid", "fOO", StringUtils.uncapitalize("FOO"));
        assertEquals("Returned string is not valid", "муУ", StringUtils.uncapitalize("МуУ"));
    }
}