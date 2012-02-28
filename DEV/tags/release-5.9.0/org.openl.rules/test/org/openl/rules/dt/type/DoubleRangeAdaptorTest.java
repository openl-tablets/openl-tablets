package org.openl.rules.dt.type;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.rules.helpers.DoubleRange;

public class DoubleRangeAdaptorTest {
	
	@Test
	public void testMax() {
		DoubleRangeAdaptor adaptor = new DoubleRangeAdaptor();
		
		DoubleRange range = new DoubleRange("[1;15]");		
		assertEquals(Double.valueOf(15), (Double)adaptor.getMax(range), Math.ulp(Double.valueOf(15)));
		
		DoubleRange range1 = new DoubleRange("[1;15)");
		assertEquals(Double.valueOf(15), (Double)adaptor.getMax(range1), Math.ulp(Double.valueOf(15)));
	}
}
