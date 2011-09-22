package org.openl.rules.helpers;

import static org.junit.Assert.*;

import org.junit.Test;

public class NumberUtilsTest {
	
	@Test
	public void testGetDoubleScale() {		
		assertEquals(3, NumberUtils.getScale(12.678));
		assertEquals(1, NumberUtils.getScale(12.1));
		assertEquals(5, NumberUtils.getScale(12.67867));
		assertEquals(1, NumberUtils.getScale(0.2));
		
		try {
			assertEquals(0, NumberUtils.getScale(null));
		} catch (NullPointerException e) {
			assertTrue("Expecting NullPointerException", true);
		}
		
		assertEquals(0, NumberUtils.getScale(Double.NaN));
		assertEquals(0, NumberUtils.getScale(Double.NEGATIVE_INFINITY));		
	}
	
	@Test
	public void testGetNumberScale() {
		assertEquals(3, NumberUtils.getScale((Number)new Double(12.678)));
		assertEquals(0, NumberUtils.getScale(new Integer(12)));
		assertEquals(0, NumberUtils.getScale(new Integer(0)));
		
		try {
			assertEquals(0, NumberUtils.getScale((Number)null));
		} catch (NullPointerException e) {
			assertTrue("Expecting NullPointerException", true);
		}
		
		assertEquals(0, NumberUtils.getScale((Number)Double.NaN));
		assertEquals(0, NumberUtils.getScale((Number)Double.NEGATIVE_INFINITY));
	}
}
