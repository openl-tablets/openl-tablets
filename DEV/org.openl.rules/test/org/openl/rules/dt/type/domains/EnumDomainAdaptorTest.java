package org.openl.rules.dt.type.domains;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.openl.domain.EnumDomain;

/**
 * @author Yury Molchan
 */
class EnumDomainAdaptorTest {

    @Test
    void ordersNumbersAscending() {
        var adaptor = new EnumDomainAdaptor(new EnumDomain<>(new Double[]{3.5, 1.0, 2.25}));
        assertTrue(adaptor.isNumeric());
        assertArrayEquals(new Object[]{1.0, 2.25, 3.5}, adaptor.getValues());
    }

    @Test
    void keepsTheOrderOfNumbersWithAnInfinity() {
        var adaptor = new EnumDomainAdaptor(new EnumDomain<>(new Double[]{3.5, Double.POSITIVE_INFINITY, Double.NaN}));
        assertFalse(adaptor.isNumeric());
        assertArrayEquals(new Object[]{3.5, Double.POSITIVE_INFINITY, Double.NaN}, adaptor.getValues());
    }
}
