package org.openl.meta;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.exception.OpenlNotCheckedException;

public class BigIntegerValueTest {
    
    @Test
    public void testEquals() {
        BigIntegerValue value1 = new BigIntegerValue("1234", new ValueMetaInfo("shortName", "fullName", null));
        BigIntegerValue value2 = new BigIntegerValue("1234", new ValueMetaInfo("shortName2", "fullName2", null));
        assertEquals(value1, value2);
        value2.setMetaInfo(value1.getMetaInfo());
        assertEquals(value1, value2);
    }
    
    @Test
    public void testEq() {
        BigIntegerValue value1 = new BigIntegerValue("1234", new ValueMetaInfo("shortName", "fullName", null));
        BigIntegerValue value2 = new BigIntegerValue("1234", new ValueMetaInfo("shortName2", "fullName2", null));
        boolean result = BigIntegerValue.eq(value1, value2);
        assertTrue(result);
        
        value1 = new BigIntegerValue("34", new ValueMetaInfo("shortName", "fullName", null));
        result = BigIntegerValue.eq(value1, value2);
        assertFalse(result);        
        
        value1 = null;
        try {
            result = BigIntegerValue.eq(value1, value2);
        } catch (OpenlNotCheckedException e) {
            assertTrue(true);
        }
        assertFalse(result);        
    }
    
    @Test
    public void testMax() {
        BigIntegerValue value1 = new BigIntegerValue("1234", new ValueMetaInfo("shortName", "fullName", null));
        BigIntegerValue value2 = new BigIntegerValue("12345", new ValueMetaInfo("shortName2", "fullName2", null));
        assertEquals(value2, BigIntegerValue.max(value1, value2));
        
        value1 = null;
        value2 = new BigIntegerValue("12345", new ValueMetaInfo("shortName2", "fullName2", null));
        try {
            assertEquals(value2, BigIntegerValue.max(value1, value2));
            fail();
        } catch (OpenlNotCheckedException e) {
            assertTrue(true);
        }
        
        
        value1 = new BigIntegerValue("12345", new ValueMetaInfo("shortName2", "fullName2", null));
        value2 = null;
        try {
            assertEquals(value1, BigIntegerValue.max(value1, value2));
            fail();
        } catch (OpenlNotCheckedException e) {
            assertTrue(true);
        }
        
        value1 = null;
        value2 = null;
        try {
            assertNull(BigIntegerValue.max(value1, value2));
        } catch (OpenlNotCheckedException e) {
            assertTrue(true);
        }           
    }
    
    @Test
    public void testMin() {
        BigIntegerValue value1 = new BigIntegerValue("12", new ValueMetaInfo("shortName", "fullName", null));
        BigIntegerValue value2 = new BigIntegerValue("12345", new ValueMetaInfo("shortName2", "fullName2", null));
        assertEquals(value1, BigIntegerValue.min(value1, value2));
        
        value1 = null;
        value2 = new BigIntegerValue("12345", new ValueMetaInfo("shortName2", "fullName2", null));
        
        try {
            assertEquals(value1, BigIntegerValue.min(value1, value2));
        } catch (OpenlNotCheckedException e) {
            assertTrue(true);
        }
        
        value1 = new BigIntegerValue("12", new ValueMetaInfo("shortName", "fullName", null));
        value2 = null;
        try {
            assertEquals(value2, BigIntegerValue.min(value1, value2));
        } catch (OpenlNotCheckedException e) {
            assertTrue(true);
        }
        
        value1 = null;
        value2 = null;
        try {
            assertNull(BigIntegerValue.min(value1, value2));
        } catch (OpenlNotCheckedException e) {
            assertTrue(true);
        }
           
    }
    
    @Test
    public void testAutocastLong() {
        BigIntegerValue expectedResult = new BigIntegerValue("1234");
        
        BigIntegerValue result = BigIntegerValue.autocast((long)1234, null);
        
        assertEquals(expectedResult, result);
    }
    
    
    
    
}
