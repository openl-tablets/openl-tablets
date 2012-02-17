package org.openl.meta;

import static junit.framework.Assert.*;

import org.junit.Test;
import org.openl.exception.OpenlNotCheckedException;

public class TestDoubleValue {
    @Test
    public void testEquals() {
        DoubleValue value1 = new DoubleValue(10.2, new ValueMetaInfo("shortName", "fullName", null), "");
        DoubleValue value2 = new DoubleValue(10.2, new ValueMetaInfo("shortName2", "fullName2", null), "");
        assertEquals(value1, value2);
        value2.setMetaInfo(value1.getMetaInfo());
        assertEquals(value1, value2);
    }
    
    @Test
    public void testAdd() {
    	DoubleValue v1 = new DoubleValue(13.33);
    	DoubleValue v2 = new DoubleValue(11.11);
    	assertEquals(24.44, DoubleValue.add(v1, v2).doubleValue(), 0.001);
    	
    	v2 = new DoubleValue(0);
    	assertEquals(13.33, DoubleValue.add(v1, v2).doubleValue(), 0.001);
    	
    	v1 = new DoubleValue(0);
    	v2 = new DoubleValue(0);
    	assertEquals(0, DoubleValue.add(v1, v2).doubleValue(), 0.001);
    	
    	v1 = new DoubleValue(14.25);
    	v2 = null;
    	assertEquals(14.25, DoubleValue.add(v1, v2).doubleValue(), 0.001);
    	
    	v1 = null;
    	v2 = new DoubleValue(17.88);
    	assertEquals(17.88, DoubleValue.add(v1, v2).doubleValue(), 0.001);
    	
    	v1 = null;
    	v2 = null;
    	assertNull(DoubleValue.add(v1, v2));
    }
    
    @Test
    public void testSubtract() {
    	DoubleValue v1 = new DoubleValue(45.55);
    	DoubleValue v2 = new DoubleValue(22.22);
    	assertEquals(23.33, DoubleValue.subtract(v1, v2).doubleValue(), 0.001);
    	
    	v1 = new DoubleValue(0);
    	assertEquals(-22.22, DoubleValue.subtract(v1, v2).doubleValue(), 0.001);
    	
    	v1 = new DoubleValue(45.55);
    	v2 = new DoubleValue(0);
    	assertEquals(45.55, DoubleValue.subtract(v1, v2).doubleValue(), 0.001);
    	
    	v1 = null;
    	v2 = new DoubleValue(22.22);
    	assertEquals(-22.22, DoubleValue.subtract(v1, v2).doubleValue(), 0.001);
    	
    	v1 = new DoubleValue(22.22);
    	v2 = null;
    	assertEquals(22.22, DoubleValue.subtract(v1, v2).doubleValue(), 0.001);
    	
    	v1 = null;
    	v2 = new DoubleValue(225.22);
    	assertEquals(-225.22, DoubleValue.subtract(v1, v2).doubleValue(), 0.001);
    	
    	v1 = null;
    	v2 = null;
    	assertNull(DoubleValue.subtract(v1, v2));
    }
    
    @Test 
    public void testMultiply() {
    	DoubleValue v1 = new DoubleValue(2);
    	DoubleValue v2 = new DoubleValue(22.22);
    	assertEquals(44.44, DoubleValue.multiply(v1, v2).doubleValue(), 0.001);
    	
    	v1 = new DoubleValue(0);
    	v2 = new DoubleValue(22.22);
    	assertEquals(0, DoubleValue.multiply(v1, v2).doubleValue(), 0.001);
    	
    	v1 = new DoubleValue(0);
    	v2 = new DoubleValue(0);
    	assertEquals(0, DoubleValue.multiply(v1, v2).doubleValue(), 0.001);
    	
    	v1 = new DoubleValue(2);
    	v2 = new DoubleValue(-5.55);
    	assertEquals(-11.1, DoubleValue.multiply(v1, v2).doubleValue(), 0.001);
    	
    	v1 = new DoubleValue(-1);
    	v2 = new DoubleValue(-22.22);
    	assertEquals(22.22, DoubleValue.multiply(v1, v2).doubleValue(), 0.001);
    	
    	v1 = new DoubleValue(0);
    	v2 = new DoubleValue(-22.22);
    	assertEquals(0, DoubleValue.multiply(v1, v2).doubleValue(), 0.001);
    	
    	v1 = null;
    	v2 = new DoubleValue(22.22);
    	assertEquals(22.22, DoubleValue.multiply(v1, v2).doubleValue(), 0.001);
    	
    	v1 = new DoubleValue(25.29);
    	v2 = null;
    	assertEquals(25.29, DoubleValue.multiply(v1, v2).doubleValue(), 0.001);
    	
    	v1 = null;
    	v2 = null;
    	assertNull(DoubleValue.multiply(v1, v2));
    	
    	v1 = new DoubleValue(11.22);
    	v2 = new DoubleValue(0);
    	assertEquals(0, DoubleValue.multiply(v1, v2).doubleValue(), 0.001);
    }
    
    @Test
    public void testDivide() {
    	DoubleValue v1 = new DoubleValue(22.22);
    	DoubleValue v2 = new DoubleValue(2);
    	assertEquals(11.11, DoubleValue.divide(v1, v2).doubleValue(), 0.001);
    	
    	v1 = new DoubleValue(0);
    	v2 = new DoubleValue(22.22);
    	assertEquals(0, DoubleValue.divide(v1, v2).doubleValue(), 0.001);
    	
    	v1 = new DoubleValue(34);
    	v2 = new DoubleValue(0.00000000);
    	try {
    		DoubleValue.divide(v1, v2);
    		fail("Division by zero cannot pass");
    	} catch (OpenlNotCheckedException e) {
    		assertTrue(true);
		}
    	
    	v1 = new DoubleValue(0);
    	v2 = new DoubleValue(0.00000000);
    	try {
    		DoubleValue.divide(v1, v2);
    		fail("Division by zero cannot pass");
    	} catch (OpenlNotCheckedException e) {
    		assertTrue(true);
		}
    	    	
    	v1 = null;
    	v2 = new DoubleValue(12);
    	assertEquals(0.08333, DoubleValue.divide(v1, v2).doubleValue(), 0.001);
    	
    	v1 = new DoubleValue(34);
    	v2 = null;
    	
    	assertEquals(34,DoubleValue.divide(v1, v2).doubleValue(), 0.1);    	
    	
    	v1 = null;
    	v2 = null;
    	assertNull(DoubleValue.divide(v1, v2));
    	
    	v1 = null;
    	v2 = new DoubleValue(0);
    	try {
    		DoubleValue.divide(v1, v2);
    		fail("Division by zero cannot pass");
    	} catch (OpenlNotCheckedException e) {
    		assertTrue(true);
		}
    }
    
    @Test
    public void testRound() {
        DoubleValue value1 = new DoubleValue(1.23456789);        
        
        assertEquals("1.2346", DoubleValue.round(value1, 4).toString());
        
        assertEquals("1.0", DoubleValue.round(value1, 0).toString());
        
        value1 = new DoubleValue(12.513456789);
        
        assertEquals("13.0", DoubleValue.round(value1, 0).toString());
    }
}
