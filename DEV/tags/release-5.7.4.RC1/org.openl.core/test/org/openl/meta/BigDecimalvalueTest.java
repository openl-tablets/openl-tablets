package org.openl.meta;

import static org.junit.Assert.*;

import org.junit.Test;

public class BigDecimalvalueTest {
    
    @Test
    public void testAutocastByte() {
        BigDecimalValue expectedResult = new BigDecimalValue("12");
        
        BigDecimalValue result = BigDecimalValue.autocast((byte)12, null);
        
        assertEquals(expectedResult, result);
    }
    
    @Test
    public void testAutocastShort() {
        BigDecimalValue expectedResult = new BigDecimalValue("11");
        
        BigDecimalValue result = BigDecimalValue.autocast((short)11, null);
        
        assertEquals(expectedResult, result);
    }
    
    @Test
    public void testAutocastChar() {
        char charVal = 'c';
        BigDecimalValue expectedResult = new BigDecimalValue(String.valueOf((int)charVal));        
        
        BigDecimalValue result = BigDecimalValue.autocast(charVal, null);
        
        assertEquals(expectedResult, result);
    }
    
    @Test
    public void testAutocastInt() {
        BigDecimalValue expectedResult = new BigDecimalValue("12");
        
        BigDecimalValue result = BigDecimalValue.autocast((int)12, null);
        
        assertEquals(expectedResult, result);
    }
    
    @Test
    public void testAutocastLong() {
        BigDecimalValue expectedResult = new BigDecimalValue("2000000000");
        
        Long value = Long.valueOf("2000000000");
        BigDecimalValue result = BigDecimalValue.autocast(value.longValue(), null);
        
        assertEquals(expectedResult, result);
    }
    
    @Test
    public void testAutocastFloat() {
        BigDecimalValue expectedResult = new BigDecimalValue("12.23");
        
        BigDecimalValue result = BigDecimalValue.autocast((float)12.23, null);
        
        assertEquals(expectedResult, result);
    }
    
    @Test
    public void testAutocastDouble() {
        BigDecimalValue expectedResult = new BigDecimalValue("12.23");
        
        BigDecimalValue result = BigDecimalValue.autocast((double)12.23, null);
        
        assertEquals(expectedResult, result);
    }

}
