/**
 *
 */
package org.openl.rules.helpers;

import static org.junit.Assert.assertEquals;

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
    public void testMinMaxFormat() {
        assertEquals(new CharRange('A', 'B'), new CharRange("A-B"));
        assertEquals(new CharRange('a','z'), new CharRange("a-z"));
    }

    @Test
    public void testMoreLessFormat() {
        assertEquals(new CharRange(Character.MIN_VALUE, 'x'), new CharRange("<y"));
        assertEquals(new CharRange('b', Character.MAX_VALUE), new CharRange("b+"));
    }

    @Test
    public void testToString() {
        assertEquals("a-z", new CharRange("a-z").toString());
        assertEquals("b-'uffff'", new CharRange("b+").toString());
    }

}
