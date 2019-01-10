/**
 *
 */
package org.openl.rules.helpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 *
 */
public class CharRangeParsingTest {

    @Test
    public void testJustNumber() {
        assertEquals(new CharRange('a', 'a'), new CharRange("a"));
    }

    @Test
    public void testBrackets() {
        assertEquals(new CharRange('D', 'X'), new CharRange("[D; X]"));
        assertEquals(new CharRange('E', 'W'), new CharRange("(D; X)"));
        assertEquals(new CharRange('E', 'X'), new CharRange("(D; X]"));
        assertEquals(new CharRange('D', 'W'), new CharRange("[D;X)"));
        assertEquals(new CharRange('D', 'X'), new CharRange("[D .. X]"));
        assertEquals(new CharRange('E', 'W'), new CharRange("(D .. X)"));
        assertEquals(new CharRange('E', 'X'), new CharRange("(D .. X]"));
        assertEquals(new CharRange('D', 'W'), new CharRange("[D .. X)"));
    }

    @Test
    public void testMinMaxFormat() {
        assertEquals(new CharRange('A', 'B'), new CharRange("A-B"));
        assertEquals(new CharRange('a', 'z'), new CharRange("a-z"));
        assertEquals(new CharRange('H', 'Z'), new CharRange("H .. Z"));
        assertEquals(new CharRange('I', 'Y'), new CharRange("H ... Z"));
        assertEquals(new CharRange('I', 'Y'), new CharRange("H … Z"));
    }

    @Test
    public void testMoreLessFormat() {
        assertEquals(new CharRange(Character.MIN_VALUE, 'x'), new CharRange("<y"));
        assertEquals(new CharRange(Character.MIN_VALUE, 'y'), new CharRange("<=y"));
        assertEquals(new CharRange('B', Character.MAX_VALUE), new CharRange(">A"));
        assertEquals(new CharRange('A', Character.MAX_VALUE), new CharRange(">=A"));
        assertEquals(new CharRange('b', Character.MAX_VALUE), new CharRange("b+"));
    }

    @Test
    public void testMoreLessFormatBothBounds() {
        assertEquals(new CharRange('0', '8'), new CharRange(">=0 <9"));
        assertEquals(new CharRange('4', '7'), new CharRange("<=7 >3"));
        assertEquals(new CharRange('3', '8'), new CharRange(" > 2   < 9 "));
        assertEquals(new CharRange('2', '9'), new CharRange(" >= 2   <=9 "));
        assertEquals(new CharRange('2', '8'), new CharRange(" < 9   > 1 "));
        assertEquals(new CharRange('1', '8'), new CharRange(" < 9   >= 1 "));
        assertEquals(new CharRange('3', '9'), new CharRange(" > 2   <= 9 "));
        assertEquals(new CharRange('A', 'Z'), new CharRange(" >=A <=Z "));
        assertEquals(new CharRange('A', 'Z'), new CharRange(" <=Z >=A "));
    }

    @Test
    public void testVerbal() {
        assertEquals(new CharRange('A', Character.MAX_VALUE), new CharRange("A and more"));
        assertEquals(new CharRange('B', Character.MAX_VALUE), new CharRange("more than A"));
        assertEquals(new CharRange(Character.MIN_VALUE, 'X'), new CharRange("less than Y"));
    }

    @Test
    public void testToString() {
        assertEquals("a-z", new CharRange("a-z").toString());
        assertEquals("b-'uffff'", new CharRange("b+").toString());
    }

    @Test
    public void testNegative() {
        try {
            new CharRange(">=A >=Z");
            fail("Must be failed!");
        } catch (RuntimeException e) {
            assertEquals("Invalid Char Range: >=A >=Z", e.getMessage());
        }
    }

}
