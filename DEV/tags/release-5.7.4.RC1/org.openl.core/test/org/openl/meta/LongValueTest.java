package org.openl.meta;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

public class LongValueTest {
    
    @Test
    public void testEquals() {
        LongValue value1 = new LongValue(2000000000, new ValueMetaInfo("shortName", "fullName", null));
        LongValue value2 = new LongValue(2000000000, new ValueMetaInfo("shortName2", "fullName2", null));
        assertEquals(value1, value2);
        value2.setMetaInfo(value1.getMetaInfo());
        assertEquals(value1, value2);
    }
    
    @Test
    public void testAdd() {
        LongValue value1 = new LongValue(187);
        LongValue value2 = new LongValue(100);
        LongValue result = LongValue.add(value1, value2);
        assertEquals(287, result.getValue());
        
        value2 = null;
        result = LongValue.add(value1, value2);
        assertEquals(187, result.getValue());
        
        value1 = null;
        value2 = null;
        result = LongValue.add(value1, value2);
        assertEquals(null, null);
    }
    
    @Test
    public void testAutocastByte() {
        LongValue result = LongValue.autocast((byte)126, null);
        assertEquals(126, result.getValue());
    }
    
    @Test
    public void testAutocastDouble() {
        LongValue result = LongValue.autocast((double)123.873434, null);
        assertEquals(123, result.getValue());
    }
    

}
