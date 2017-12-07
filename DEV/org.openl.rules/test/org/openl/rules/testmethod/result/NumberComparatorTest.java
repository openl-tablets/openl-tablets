package org.openl.rules.testmethod.result;

import static org.junit.Assert.*;

import org.junit.Test;

public class NumberComparatorTest {
    @Test
    public void test() {
        NumberComparator comp = new NumberComparator();

        assertTrue(comp.compareResult(null, null));

        Double value = Double.valueOf(10);

        assertFalse(comp.compareResult(null, value));

        assertFalse(comp.compareResult(value, null));

        assertTrue(comp.compareResult(value, value));

        //Tests with delta
        Double value1 = Double.valueOf(590.4563546464);
        Double value2 = Double.valueOf(590.456377867);
        Double value3 = Double.valueOf(550.46);

        assertFalse(new NumberComparator(null).compareResult(value1, value2));
        assertFalse(new NumberComparator(0.00001).compareResult(value1, value2));
        assertTrue(new NumberComparator(0.0001).compareResult(value1, value2));
        assertTrue(new NumberComparator(100.0).compareResult(value1, value3));    }
}
