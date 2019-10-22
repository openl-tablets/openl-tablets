package org.openl.rules.dt.type;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openl.rules.helpers.DoubleRange;

public class DoubleRangeAdaptorTest {

    @Test
    public void testMax() {
        IRangeAdaptor<DoubleRange, Double> adaptor = DoubleRangeAdaptor.getInstance();

        DoubleRange range = new DoubleRange("[1;15]");
        assertEquals(15, adaptor.getMax(range), Math.ulp(15));

        DoubleRange range1 = new DoubleRange("[1;15)");
        assertEquals(15, adaptor.getMax(range1), Math.ulp(15));
    }
}
