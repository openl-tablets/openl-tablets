package org.openl.rules.testmethod.result;

import static org.junit.Assert.*;

import org.junit.Test;

public class NumberComparatorTest {
    @Test
    public void test() {
        NumberComparator comp = new NumberComparator();

        assertTrue(comp.compareResult(null, null, null));

        Double value = Double.valueOf(10);

        assertFalse(comp.compareResult(null, value, null));

        assertFalse(comp.compareResult(value, null, null));

        assertTrue(comp.compareResult(value, value, null));

        //Tests with delta
        Double value1 = Double.valueOf(590.4563546464);
        Double value2 = Double.valueOf(590.456377867);
        Double value3 = Double.valueOf(550.46);

        assertFalse(comp.compareResult(value1, value2, null));
        assertFalse(comp.compareResult(value1, value2, 0.00001));
        assertTrue(comp.compareResult(value1, value2, 0.0001));
        assertTrue(comp.compareResult(value1, value3, 100.0));    }
}
