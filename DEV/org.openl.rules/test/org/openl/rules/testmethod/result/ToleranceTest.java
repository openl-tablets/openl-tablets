package org.openl.rules.testmethod.result;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

/**
 * @author Yury Molchan
 */
class ToleranceTest {

    @Test
    void decimalPlaces() {
        assertEquals(new Tolerance.Delta(new BigDecimal("0.00001")), Tolerance.of("5"));
        assertEquals(new Tolerance.Delta(new BigDecimal("1E-11")), Tolerance.of("11"));
        assertEquals(new Tolerance.Delta(new BigDecimal("0.01")), Tolerance.of("2"));
        assertEquals(new Tolerance.Delta(BigDecimal.ONE), Tolerance.of("0"));
        assertEquals(new Tolerance.Delta(new BigDecimal("1E+2")), Tolerance.of("-2"));
    }

    @Test
    void limits() {
        assertEquals(new Tolerance.Delta(new BigDecimal("1E+99")), Tolerance.of("-99"));
        assertEquals(new Tolerance.Delta(new BigDecimal("1E-99")), Tolerance.of("99"));
        assertEquals(new Tolerance.SignificantFigures(99), Tolerance.of("99sf"));
        assertNull(Tolerance.of("-100"));
        assertNull(Tolerance.of("100"));
        assertNull(Tolerance.of("100sf"));
        assertNull(Tolerance.of("-2147483648"));
        assertNull(Tolerance.of("2147483647sf"));
    }

    @Test
    void significantFigures() {
        assertEquals(new Tolerance.SignificantFigures(1), Tolerance.of("1sf"));
        assertEquals(new Tolerance.SignificantFigures(4), Tolerance.of("4sf"));
        assertEquals(new Tolerance.SignificantFigures(40), Tolerance.of("40sf"));
        assertEquals(new Tolerance.ExpectedFigures(null), Tolerance.of("sf"));
    }

    @Test
    void notPrecision() {
        assertNull(Tolerance.of(""));
        assertNull(Tolerance.of("sfsf"));
        assertNull(Tolerance.of(" sf"));
        assertNull(Tolerance.of("0sf"));
        assertNull(Tolerance.of("04sf"));
        assertNull(Tolerance.of("-2sf"));
        assertNull(Tolerance.of("4 sf"));
        assertNull(Tolerance.of("4SF"));
        assertNull(Tolerance.of("1.5"));
        assertNull(Tolerance.of("-1000000000000000"));
        assertNull(Tolerance.of("10000000sf"));
    }
}
