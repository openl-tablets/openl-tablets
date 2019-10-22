package org.openl.rules.testmethod.result;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NumberComparatorTest {
    @Test
    public void test() {
        NumberComparator comp = (NumberComparator) NumberComparator.getInstance();

        assertTrue(comp.isEqual(null, null));

        Double value = (double) 10;

        assertFalse(comp.isEqual(value, null));

        assertFalse(comp.isEqual(null, value));

        assertTrue(comp.isEqual(value, value));

        // Tests with delta
        Double value1 = 590.4563546464;
        Double value2 = 590.456377867;
        Double value3 = 550.46;

        assertFalse(new NumberComparator(null).isEqual(value2, value1));
        assertFalse(new NumberComparator(0.00001).isEqual(value2, value1));
        assertTrue(new NumberComparator(0.0001).isEqual(value2, value1));
        assertTrue(new NumberComparator(100.0).isEqual(value3, value1));
    }
}
