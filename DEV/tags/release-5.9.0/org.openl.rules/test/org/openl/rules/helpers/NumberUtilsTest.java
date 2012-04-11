package org.openl.rules.helpers;

import static org.junit.Assert.*;

import java.math.BigDecimal;

import org.junit.Test;
import org.openl.meta.BigDecimalValue;
import org.openl.meta.DoubleValue;

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
	
	@Test
	public void testFloat() {
		assertEquals(15, NumberUtils.getScale(Float.valueOf(((float)12.45678))));
		assertEquals(0, NumberUtils.getScale(Float.NaN));
		assertEquals(0, NumberUtils.getScale(Float.NEGATIVE_INFINITY));
		assertEquals(0, NumberUtils.getScale(Float.POSITIVE_INFINITY));
	}
	
	@Test
	public void testDoubleValue() {
		assertEquals(4, NumberUtils.getScale(new DoubleValue(1234.5553)));
		assertEquals(0, NumberUtils.getScale(new DoubleValue(Double.NaN)));
		assertEquals(0, NumberUtils.getScale(new DoubleValue(Double.NEGATIVE_INFINITY)));
		assertEquals(0, NumberUtils.getScale(new DoubleValue(Double.POSITIVE_INFINITY)));
	}
	
	@Test
	public void testBigDecimal() {
		assertEquals(36, NumberUtils.getScale(new BigDecimal("12.123456789123456789123456789123456789")));
		assertEquals(35, NumberUtils.getScale(new BigDecimalValue(new BigDecimal("12.12345678912345678912345678912345678"))));
	}
	
}
