package org.openl.meta;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.exception.OpenlNotCheckedException;

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
        try {
            result = LongValue.add(value1, value2);
            assertNull(result);            
        } catch (OpenlNotCheckedException e) {
            assertTrue(true);
        }
        
        value1 = null;
        value2 = null;
        try {
            result = LongValue.add(value1, value2);
            assertNull(result);
        } catch (OpenlNotCheckedException e) {
            assertTrue(true);
        }
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
    
    @Test
    public void testMin() {
        LongValue[] la = getTestArray();
        assertEquals(new LongValue(5), LongValue.min(la));
        
        LongValue[] nullArray = null;
        try {
            assertEquals(new LongValue(0), LongValue.min(nullArray));
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
        
        LongValue[] emptyArray = new LongValue[0];
        try {
            assertEquals(new LongValue(0), LongValue.min(emptyArray));
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }        
    }

    private LongValue[] getTestArray() {
        return new LongValue[]{new LongValue((long)10), new LongValue((long)100), new LongValue((long)5)};
    }
    
    @Test
    public void testMax() {
        LongValue[] la = getTestArray();
        assertEquals(new LongValue(100), LongValue.max(la));
        
        LongValue[] nullArray = null;
        try {
            assertEquals(new LongValue(0), LongValue.max(nullArray));
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
        
        LongValue[] emptyArray = new LongValue[0];
        try {
            assertEquals(new LongValue(0), LongValue.max(emptyArray));
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }        
    }
    
    @Test
    public void testAvg() {
        LongValue[] la = getTestArray();
        assertEquals(new LongValue(38), LongValue.avg(la));
        
        LongValue[] nullArray = null;
        assertEquals(null, LongValue.avg(nullArray));
                
        LongValue[] emptyArray = new LongValue[0];
        assertEquals(null, LongValue.avg(emptyArray));                
    }
    
    @Test
    public void testSum() {
        LongValue[] la = getTestArray();
        assertEquals(new LongValue(115), LongValue.sum(la));
        
        LongValue[] nullArray = null;
        assertEquals(null, LongValue.sum(nullArray));
                
        LongValue[] emptyArray = new LongValue[0];
        assertEquals(null, LongValue.sum(emptyArray));                
    }
    
    @Test
    public void testMedian() {
        LongValue[] la = getTestArray();
        assertEquals(new LongValue(10), LongValue.median(la));
        
        LongValue[] nullArray = null;
        assertEquals(null, LongValue.median(nullArray));
                
        LongValue[] emptyArray = new LongValue[0];
        assertEquals(null, LongValue.median(emptyArray));                
    }
    
    @Test
    public void testProduct() {
        LongValue[] la = getTestArray();
        assertEquals(new DoubleValue(5000), LongValue.product(la));
        
        LongValue[] nullArray = null;
        assertEquals(null, LongValue.product(nullArray));
                
        LongValue[] emptyArray = new LongValue[0];
        assertEquals(null, LongValue.product(emptyArray));                
    }
    
    @Test
    public void testQuaotient() {        
        assertEquals(new LongValue(5), LongValue.quaotient(new LongValue(26), new LongValue(5)));
        
        LongValue nullObj = null;
        assertEquals(null, LongValue.quaotient(nullObj, new LongValue(5)));
        
        assertEquals(null, LongValue.quaotient(new LongValue(5), nullObj));
        
        try {
            assertEquals(new LongValue(0), LongValue.quaotient(new LongValue(5), new LongValue(0)));
            fail();
        } catch (ArithmeticException e) {
            assertTrue(true);
        }                
    }
    
    @Test
    public void testMod() {        
        assertEquals(new LongValue(1), LongValue.mod(new LongValue(26), new LongValue(5)));
        
        LongValue nullObj = null;
        assertEquals(null, LongValue.mod(nullObj, new LongValue(5)));
        
        assertEquals(null, LongValue.mod(new LongValue(5), nullObj));
        
        try {
            assertEquals(new LongValue(0), LongValue.mod(new LongValue(5), new LongValue(0)));
            fail();
        } catch (ArithmeticException e) {
            assertTrue(true);
        }                
    }
    
    @Test
    public void testSmall() {
        LongValue[] la = getTestArray();
        assertEquals(new LongValue(10), LongValue.small(la, 2));
        
        LongValue[] nullArray = null;
        assertEquals(null, LongValue.small(nullArray, 1));
                
        LongValue[] emptyArray = new LongValue[0];
        assertEquals(null, LongValue.small(emptyArray, 1));                
    }
    

}
