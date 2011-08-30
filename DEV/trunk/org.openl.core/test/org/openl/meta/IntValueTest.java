package org.openl.meta;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.util.math.MathUtils;

public class IntValueTest {
    
    @Test
    public void testEquals() {
        IntValue value1 = new IntValue(10000, new ValueMetaInfo("shortName", "fullName", null));
        IntValue value2 = new IntValue(10000, new ValueMetaInfo("shortName2", "fullName2", null));
        assertEquals(value1, value2);
        value2.setMetaInfo(value1.getMetaInfo());
        assertEquals(value1, value2);
    }
    
    @Test
    public void testDivide() {
        IntValue value1 = new IntValue(10000, new ValueMetaInfo("shortName", "fullName", null));
        IntValue value2 = new IntValue(10000, new ValueMetaInfo("shortName2", "fullName2", null));
        assertEquals(1, IntValue.divide(value1, value2).getValue());
        
        value2 = new IntValue(0, new ValueMetaInfo("shortName2", "fullName2", null));        
        try {
            assertEquals(1, IntValue.divide(value1, value2));
            fail();
        } catch (OpenlNotCheckedException e) {
            assertTrue(true);
        }
    }
    
    @Test
    public void testBig() {
        IntValue[] mas = new IntValue[]{new IntValue(5), new IntValue(47), new IntValue(34), new IntValue(44), 
                new IntValue(11)};
        assertEquals(new IntValue(47), IntValue.big(mas, 1));
        assertEquals(new IntValue(44), IntValue.big(mas, 2));
        assertEquals(new IntValue(34), IntValue.big(mas, 3));
        assertEquals(new IntValue(11), IntValue.big(mas, 4));
        assertEquals(new IntValue(5), IntValue.big(mas, 5));
        
        try {
            assertEquals(0, IntValue.big(mas, 6));
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("There is no position '6' in the given array", e.getMessage());
        }
        
        mas = null;
        assertEquals(null, IntValue.big(mas, 5));
        
        mas = new IntValue[1];
        try {
            assertEquals(0, IntValue.big(mas, 5));
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("There is no position '5' in the given array", e.getMessage());
        }
    }

}
