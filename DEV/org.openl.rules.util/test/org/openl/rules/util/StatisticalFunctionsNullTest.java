package org.openl.rules.util;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Test to ensure statistical functions return null for arrays with single non-null value
 * instead of NaN.
 */
class StatisticalFunctionsNullTest {

    @Test
    void testStdevSReturnsNull() {
        // stdevS with single non-null value should return null
        assertNull(StdDev.stdevS(5, null));
        assertNull(StdDev.stdevS(5.0));
        assertNull(StdDev.stdevS(5L, null));
        assertNull(StdDev.stdevS((short) 5, null));
        assertNull(StdDev.stdevS(5f, null));
    }

    @Test
    void testInterceptReturnsNull() {
        // intercept with single non-null value should return null
        assertNull(Intercept.intercept(new Double[]{5.0, null}, new Double[]{5.0, null}));
        assertNull(Intercept.intercept(new Float[]{5.0f, null}, new Float[]{5.0f, null}));
    }

    @Test
    void testSlopeReturnsNull() {
        // slope with single non-null value should return null
        assertNull(Slope.slope(new Double[]{5.0, null}, new Double[]{5.0, null}));
        assertNull(Slope.slope(new Float[]{5.0f, null}, new Float[]{5.0f, null}));
    }

    @Test
    void testRsqReturnsNull() {
        // rsq with single non-null value should return null
        assertNull(Correl.rsq(new Double[]{5.0, null}, new Double[]{5.0, null}));
        assertNull(Correl.rsq(new Float[]{5.0f, null}, new Float[]{5.0f, null}));
    }

    @Test
    void testForecastReturnsNull() {
        // forecast with single non-null value should return null
        assertNull(Forecast.forecast(5.0, new Double[]{5.0, null}, new Double[]{5.0, null}));
        assertNull(Forecast.forecast(5.0f, new Float[]{5.0f, null}, new Float[]{5.0f, null}));
    }

    @Test
    void testCorrelReturnsNull() {
        // correl with single non-null value should return null
        assertNull(Correl.correl(new Double[]{5.0, null}, new Double[]{5.0, null}));
        assertNull(Correl.correl(new Float[]{5.0f, null}, new Float[]{5.0f, null}));
    }

    @Test
    void testCovarSReturnsNull() {
        // covarS with single non-null value should return null
        assertNull(Covar.covarS(new Double[]{5.0, null}, new Double[]{5.0, null}));
        assertNull(Covar.covarS(new Float[]{5.0f, null}, new Float[]{5.0f, null}));
    }

    @Test
    void testIntegerArrayFunctions() {
        // Test Integer array with single non-null value
        assertNull(StdDev.stdevS(5, null));
        assertNull(Var.varS(5, null));
    }

    @Test
    void testFloatArrayFunctions() {
        // Test Float array with single non-null value
        assertNull(StdDev.stdevS(5.0f, null));
        assertNull(Var.varS(5.0f, null));
    }

    @Test
    void testLongArrayFunctions() {
        // Test Long array with single non-null value
        assertNull(StdDev.stdevS(5L, null));
        assertNull(Var.varS(5L, null));
    }

    @Test
    void testShortArrayFunctions() {
        // Test Short array with single non-null value
        assertNull(StdDev.stdevS((short) 5, null));
        assertNull(Var.varS((short) 5, null));
    }
}
