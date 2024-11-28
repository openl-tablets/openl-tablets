/**
 *
 */
package org.openl.rules.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

public class CharRangeParsingTest {

    @Test
    public void testJustNumber() {
        assertEquals(new CharRange('a', 'a'), new CharRange("a"));
    }

    @Test
    public void testBrackets() {
        assertEquals("[D..X]", new CharRange("[D; X]").toString());
        assertEquals("[D..X]", new CharRange("  [  D  ;   X  ]  ").toString());
        assertEquals("(D..X)", new CharRange("(D; X)").toString());
        assertEquals("(D..X)", new CharRange("  (  D  ;   X  )  ").toString());
        assertEquals("(D..X]", new CharRange("(D; X]").toString());
        assertEquals("[D..X)", new CharRange("[D;X)").toString());
        assertEquals("[D..X]", new CharRange("[D .. X]").toString());
        assertEquals("(D..X)", new CharRange("(D .. X)").toString());
        assertEquals("(D..X]", new CharRange("(D .. X]").toString());
        assertEquals("[D..X)", new CharRange("[D .. X)").toString());
    }

    @Test
    public void testMinMaxFormat() {
        assertEquals("[A..B]", new CharRange("A-B").toString());
        assertEquals("[A..B]", new CharRange("  A  -  B  ").toString());
        assertEquals("[a..z]", new CharRange("a-z").toString());
        assertEquals("[H..Z]", new CharRange("H .. Z").toString());
        assertEquals("[H..Z]", new CharRange("  H   ..   Z  ").toString());
        assertEquals("(H..Z)", new CharRange("H ... Z").toString());
        assertEquals("(H..Z)", new CharRange("  H   ...   Z  ").toString());
        assertEquals("(H..Z)", new CharRange("H … Z").toString());
        assertEquals("(H..Z)", new CharRange("  H   …   Z  ").toString());
    }

    @Test
    public void testMoreLessFormat() {
        assertEquals("< y", new CharRange("<y").toString());
        assertEquals("< y", new CharRange("  <  y  ").toString());
        assertEquals("<= y", new CharRange("<=y").toString());
        assertEquals("> A", new CharRange(">A").toString());
        assertEquals(">= A", new CharRange(">=A").toString());
        assertEquals(">= A", new CharRange("  >=A  ").toString());
        assertEquals(">= b", new CharRange("b+").toString());
        assertEquals(">= b", new CharRange("  b+  ").toString());
    }

    @Test
    public void testMoreLessFormatBothBounds() {
        assertEquals("[0..9)", new CharRange(">=0 <9").toString());
        assertEquals("(3..7]", new CharRange("<=7 >3").toString());
        assertEquals("(2..9)", new CharRange(" > 2   < 9 ").toString());
        assertEquals("[2..9]", new CharRange(" >= 2   <=9 ").toString());
        assertEquals("(1..9)", new CharRange(" < 9   > 1 ").toString());
        assertEquals("[1..9)", new CharRange(" < 9   >= 1 ").toString());
        assertEquals("(2..9]", new CharRange(" > 2   <= 9 ").toString());
        assertEquals("[A..Z]", new CharRange(" >=A <=Z ").toString());
        assertEquals("[A..Z]", new CharRange(" <=Z >=A ").toString());
    }

    @Test
    public void testVerbal() {
        assertEquals(">= A", new CharRange("A and more").toString());
        assertEquals("> A", new CharRange("more than A").toString());
        assertEquals("< Y", new CharRange("less than Y").toString());
        assertEquals(">= A", new CharRange("  A   and   more  ").toString());
        assertEquals("> A", new CharRange("  more   than   A  ").toString());
        assertEquals("< Y", new CharRange("  less   than   Y  ").toString());
    }

    @Test
    public void testToString() {
        assertEquals("[a..z]", new CharRange("a-z").toString());
        assertEquals(">= b", new CharRange("b+").toString());
        assertEquals("[\\u0fff..\\uffff]", new CharRange("\u0fff-\uffff").toString());
    }

    @Test
    public void testNegative() {
        try {
            new CharRange(">=A >=Z");
            fail("Must be failed.");
        } catch (RuntimeException ignored) {
        }
    }

    @Test
    public void testParseException() {
        assertThrows(RuntimeException.class, () -> {
            new StringRange(null);
        });
    }

}
