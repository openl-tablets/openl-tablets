/**
 * 
 */
package org.openl.rules.helpers;

import static org.junit.Assert.*;

import org.junit.Test;


/**
 *
 */
public class IntRangeParsingTest {

    @Test
    public void testMinMaxFormat() {
       assertEquals(new IntRange(1, 2), new IntRange("1-2"));
       assertEquals(new IntRange(13, 200), new IntRange("13-200"));
       assertEquals(new IntRange(-15, -8), new IntRange("-15 - -8"));
       assertEquals(new IntRange(-3, 5), new IntRange("-3 - +5"));
    }
    
    @Test
    public void testMoreLessFormat() {
       assertEquals(new IntRange(Integer.MIN_VALUE, -13), new IntRange("<-12"));
       assertEquals(new IntRange(Integer.MIN_VALUE, 7), new IntRange("<=+7"));
       assertEquals(new IntRange(100, Integer.MAX_VALUE), new IntRange(">= 100"));
       assertEquals(new IntRange(3, Integer.MAX_VALUE), new IntRange(">2"));
    }

    
    @Test
    public void testPlusFormat() {
       assertEquals(new IntRange(-100, Integer.MAX_VALUE), new IntRange("-100+"));
       assertEquals(new IntRange(2, Integer.MAX_VALUE), new IntRange("+2 +"));
       assertEquals(new IntRange(0, Integer.MAX_VALUE), new IntRange("0+"));
    }
}
