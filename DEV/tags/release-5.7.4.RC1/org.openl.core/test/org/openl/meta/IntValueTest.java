package org.openl.meta;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.*;

import org.junit.Test;

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
        } catch (ArithmeticException e) {
            assertTrue(true);
        }
        
    }

}
