package org.openl.rules.helpers;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.util.RangeWithBounds.BoundType;

/**
 * 
 * @author PUdalau
 */
public class DoubleRangeParsingTest {

    @Test
    public void testBrackets() {
        assertTrue(new DoubleRange("(2;3.01)").contains(3));
        assertFalse(new DoubleRange("(2;3.01)").contains(2));
        assertTrue(new DoubleRange("[2 .. 3.01)").contains(2));
        assertEquals(new DoubleRange("[4;5]"), new DoubleRange(4, 5));
        assertEquals(new DoubleRange("( 1.0002; 6 ]"), new DoubleRange(1.0002, 6, BoundType.EXCLUDING,
                BoundType.INCLUDING));
    }

    @Test
    public void testJustNumber() {
        assertEquals(new DoubleRange(37.1, 37.1), new DoubleRange("37.1"));
    }

    @Test
    public void testKMB() {
        assertEquals(new DoubleRange(1100000, Double.POSITIVE_INFINITY), new DoubleRange("1.1M+"));
        assertEquals(new DoubleRange(2330000000d, 2330000000d), new DoubleRange("2.33B"));
        assertEquals(new DoubleRange(1200, 36000000), new DoubleRange("1.2K .. 36M"));
        assertEquals(new DoubleRange(Double.NEGATIVE_INFINITY, 24001), new DoubleRange("<=24.001K"));
    }

    @Test
    public void testMinMaxFormat() {
        assertEquals(new DoubleRange(1, 2.3), new DoubleRange("1-2.3"));
        assertEquals(new DoubleRange(13.01, 200.7), new DoubleRange("13.01 .. 200.7"));
        assertEquals(new DoubleRange(10, 123, BoundType.EXCLUDING, BoundType.EXCLUDING), new DoubleRange("10 ... 123"));
    }

    @Test
    public void testMoreLessFormat() {
        assertEquals(new DoubleRange(Double.NEGATIVE_INFINITY, 12.123, BoundType.INCLUDING, BoundType.EXCLUDING),
                new DoubleRange("<12.123"));
        assertEquals(new DoubleRange(Double.NEGATIVE_INFINITY, 7), new DoubleRange("<=7"));
        assertEquals(new DoubleRange(0.0000001, Double.POSITIVE_INFINITY, BoundType.EXCLUDING, BoundType.INCLUDING),
                new DoubleRange(">0.0000001"));
    }

    @Test
    public void testPlusFormat() {
        assertEquals(new DoubleRange(123457890.0987654321, Double.POSITIVE_INFINITY), new DoubleRange("123457890.0987654321+"));
    }
    
    @Test
    public void testNegativeDoubleRange() {        
        assertEquals(new DoubleRange(-200.7, -13.01), new DoubleRange("[-200.7;-13.01]"));
        assertEquals(new DoubleRange(Double.NEGATIVE_INFINITY, -7), new DoubleRange("<= -7"));
    }
}
