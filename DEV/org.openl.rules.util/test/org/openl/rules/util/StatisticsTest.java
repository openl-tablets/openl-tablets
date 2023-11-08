package org.openl.rules.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import static org.openl.rules.util.Avg.avg;
import static org.openl.rules.util.Statistics.rsq;
import static org.openl.rules.util.Statistics.forecast;
import static org.openl.rules.util.Statistics.intercept;
import static org.openl.rules.util.Statistics.correl;
import static org.openl.rules.util.Statistics.max;
import static org.openl.rules.util.Statistics.min;
import static org.openl.rules.util.Statistics.covarP;
import static org.openl.rules.util.Statistics.varP;
import static org.openl.rules.util.Statistics.covarS;
import static org.openl.rules.util.Statistics.stdevS;
import static org.openl.rules.util.Statistics.varS;
import static org.openl.rules.util.Statistics.slope;
import static org.openl.rules.util.Statistics.stdevP;
import static org.openl.rules.util.Sum.sum;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

import org.junit.jupiter.api.Test;

public class StatisticsTest {

    private static final double DELTA = 1e-9;

    @Test
    public void testMax() {
        assertNull(max());
        assertNull(max(null));
        assertNull(max(new Integer[0]));
        assertNull(max(new Integer[]{null}));

        assertEquals(Integer.valueOf(10), max(1, 10, 9));
        assertEquals(Double.valueOf(9.5), max(9.5));
        assertEquals(Integer.valueOf(8), max(8, null, 2));
        assertEquals(Double.valueOf(7.0), max(-10.0, 6.0, 7.0));
    }

    @Test
    public void testMin() {
        assertNull(min());
        assertNull(min(null));
        assertNull(min(new Integer[0]));
        assertNull(min(new Integer[]{null}));

        assertEquals(Integer.valueOf(1), min(1, 10, 9));
        assertEquals(Double.valueOf(9.5), min(9.5));
        assertEquals(Integer.valueOf(2), min(8, null, 2));
        assertEquals(Double.valueOf(-10.0), min(-10.0, 6.0, 7.0));
    }

    @Test
    public void testSum() {
        assertNull(sum((Integer[]) null));
        assertNull(sum(new Integer[0]));
        assertNull(sum(new Integer[]{null}));

        assertEquals(Integer.valueOf(20), sum(1, 10, 9));
        assertEquals(Double.valueOf(9.5), sum(9.5));
        assertEquals(Integer.valueOf(10), sum(8, null, 2));
        assertEquals(Double.valueOf(3.0), sum(-10.0, 6.0, 7.0));

        assertEquals(Integer.valueOf(15), sum((byte) 3, (byte) 4, (byte) 8));
        assertEquals(Integer.valueOf(15), sum((short) 3, (short) 4, (short) 8));
        assertEquals(Integer.valueOf(15), sum(3, 4, 8));
        assertEquals(Long.valueOf(15), sum(3L, 4L, 8L));
        assertEquals(Float.valueOf(15), sum(3f, 4f, 8f));
        assertEquals(Double.valueOf(15), sum(3d, 4d, 8d));
        assertEquals(Double.valueOf(15), sum((byte) 3, (short) 4, 8));
        assertEquals(BigInteger.valueOf(15), sum(BigInteger.valueOf(3), BigInteger.valueOf(4), BigInteger.valueOf(8)));
        assertEquals(BigDecimal.valueOf(15), sum(BigDecimal.valueOf(3), BigDecimal.valueOf(4), BigDecimal.valueOf(8)));
    }

    @Test
    public void testAvg() {
        assertNull(avg((Integer[]) null));
        assertNull(avg(new Integer[0]));
        assertNull(avg(new Integer[]{null}));

        assertEquals(Double.valueOf(20.0 / 3.0), avg(1, 10, 9));
        assertEquals(Double.valueOf(9.5), avg(9.5));
        assertEquals(Double.valueOf(5), avg(8, null, 2));
        assertEquals(Double.valueOf(1), avg(-10.0, 6.0, 7.0));

        assertEquals(Double.valueOf(5), avg((byte) 3, (byte) 4, (byte) 8));
        assertEquals(Double.valueOf(5), avg((short) 3, (short) 4, (short) 8));
        assertEquals(Double.valueOf(5), avg(3, 4, 8));
        assertEquals(Double.valueOf(5), avg(3L, 4L, 8L));
        assertEquals(Float.valueOf(5), avg(3f, 4f, 8f));
        assertEquals(Double.valueOf(5), avg(3d, 4d, 8d));
        assertEquals(Double.valueOf(5), avg((byte) 3, (short) 4, 8));
        assertEquals(BigDecimal.valueOf(5), avg(BigInteger.valueOf(3), BigInteger.valueOf(4), BigInteger.valueOf(8)));
        assertEquals(BigDecimal.valueOf(3.5), avg(BigDecimal.valueOf(3), BigDecimal.valueOf(4)));
        assertEquals(new BigDecimal("5.333333333333333333333333333333333", MathContext.DECIMAL128),
                avg(BigInteger.valueOf(3), BigInteger.valueOf(5), BigInteger.valueOf(8)));
    }

    @Test
    public void testStandardPopulationDeviation() {
        assertNull(stdevP(null));
        assertNull(stdevP(new Double[2]));

        assertEquals(4.0276819911981905, stdevP(1, 10, 9), DELTA);
        assertEquals(0, stdevP(9.5), DELTA);
        assertEquals(3, stdevP(8, null, 2), DELTA);
        assertEquals(7.788880963698615, stdevP(-10.0, 6.0, 7.0), DELTA);

        assertEquals(2.160246899469287, stdevP((byte) 3, (byte) 4, (byte) 8), DELTA);
        assertEquals(2.160246899469287, stdevP((short) 3, (short) 4, (short) 8), DELTA);
        assertEquals(2.160246899469287, stdevP(3, 4, 8), DELTA);
        assertEquals(2.160246899469287, stdevP(3L, 4L, 8L), DELTA);
        assertEquals(2.160246899469287, stdevP(3f, 4f, 8f), DELTA);
        assertEquals(2.160246899469287, stdevP(3d, 4d, 8d), DELTA);
        assertEquals(2.160246899469287, stdevP((byte) 3, (short) 4, 8), DELTA);
        assertEquals(2.494438257849294,
                stdevP(BigInteger.valueOf(10), BigInteger.valueOf(4), BigInteger.valueOf(8)), DELTA);
        assertEquals(0.5, stdevP(BigDecimal.valueOf(3), BigDecimal.valueOf(4)), DELTA);
        assertEquals(2.0548046676563256,
                stdevP(BigInteger.valueOf(3), BigInteger.valueOf(5), BigInteger.valueOf(8)), DELTA);

    }

    @Test
    public void testPopulationVariance() {
        assertNull(varP(null));
        assertNull(varP(new Double[2]));

        assertEquals(16.22222222222222, varP(1, 10, 9), DELTA);
        assertEquals(0, varP(9.5), DELTA);
        assertEquals(9.0, varP(8, null, 2), DELTA);
        assertEquals(60.666666666666664, varP(-10.0, 6.0, 7.0), DELTA);

        assertEquals(4.666666666666667, varP((byte) 3, (byte) 4, (byte) 8), DELTA);
        assertEquals(4.666666666666667, varP((short) 3, (short) 4, (short) 8), DELTA);
        assertEquals(4.666666666666667, varP(3, 4, 8), DELTA);
        assertEquals(4.666666666666667, varP(3L, 4L, 8L), DELTA);
        assertEquals(4.666666666666667, varP(3f, 4f, 8f), DELTA);
        assertEquals(4.666666666666667, varP(3d, 4d, 8d), DELTA);
        assertEquals(4.666666666666667, varP((byte) 3, (short) 4, 8), DELTA);
        assertEquals(6.222222222222222,
                varP(BigInteger.valueOf(10), BigInteger.valueOf(4), BigInteger.valueOf(8)), DELTA);
        assertEquals(0.25, varP(BigDecimal.valueOf(3), BigDecimal.valueOf(4)), DELTA);
        assertEquals(4.222222222222222,
                varP(BigInteger.valueOf(3), BigInteger.valueOf(5), BigInteger.valueOf(8)), DELTA);

    }

    @Test
    public void testSampleVariance() {
        assertNull(varS(null));
        assertNull(varS(new Double[2]));

        assertEquals(24.333333333333332, varS(1, 10, 9), DELTA);
        assertNull(varS(9.5));
        assertEquals(18.0, varS(8, null, 2), DELTA);
        assertEquals(91.0, varS(-10.0, 6.0, 7.0), DELTA);

        assertEquals(7.0, varS((byte) 3, (byte) 4, (byte) 8), DELTA);
        assertEquals(7.0, varS((short) 3, (short) 4, (short) 8), DELTA);
        assertEquals(7.0, varS(3, 4, 8), DELTA);
        assertEquals(7.0, varS(3L, 4L, 8L), DELTA);
        assertEquals(7.0, varS(3f, 4f, 8f), DELTA);
        assertEquals(7.0, varS(3d, 4d, 8d), DELTA);
        assertEquals(7.0, varS((byte) 3, (short) 4, 8), DELTA);
        assertEquals(9.333333333333334,
                varS(BigInteger.valueOf(10), BigInteger.valueOf(4), BigInteger.valueOf(8)), DELTA);
        assertEquals(0.5, varS(BigDecimal.valueOf(3), BigDecimal.valueOf(4)), DELTA);
        assertEquals(6.333333333333333,
                varS(BigInteger.valueOf(3), BigInteger.valueOf(5), BigInteger.valueOf(8)), DELTA);

    }

    @Test
    public void testSampleStandardDeviation() {
        assertNull(stdevS(null));
        assertNull(stdevS(new Double[2]));

        assertEquals(4.932882862316247, stdevS(1, 10, 9), DELTA);
        assertNull(stdevS(9.5));
        assertEquals(4.242640687119285, stdevS(8, null, 2), DELTA);
        assertEquals(9.539392014169456, stdevS(-10.0, 6.0, 7.0), DELTA);

        assertEquals(2.6457513110645907, stdevS((byte) 3, (byte) 4, (byte) 8), DELTA);
        assertEquals(2.6457513110645907, stdevS((short) 3, (short) 4, (short) 8), DELTA);
        assertEquals(2.6457513110645907, stdevS(3, 4, 8), DELTA);
        assertEquals(2.6457513110645907, stdevS(3L, 4L, 8L), DELTA);
        assertEquals(2.6457513110645907, stdevS(3f, 4f, 8f), DELTA);
        assertEquals(2.6457513110645907, stdevS(3d, 4d, 8d), DELTA);
        assertEquals(2.6457513110645907, stdevS((byte) 3, (short) 4, 8), DELTA);
        assertEquals(3.0550504633038935,
                stdevS(BigInteger.valueOf(10), BigInteger.valueOf(4), BigInteger.valueOf(8)), DELTA);
        assertEquals(0.7071067811865476, stdevS(BigDecimal.valueOf(3), BigDecimal.valueOf(4)), DELTA);
        assertEquals(2.516611478423583,
                stdevS(BigInteger.valueOf(3), BigInteger.valueOf(5), BigInteger.valueOf(8)), DELTA);

    }

    @Test
    public void testSampleCovariance() {
        assertNull(covarS(null, null));
        assertNull(covarS(new Double[2], new Double[2]));
        assertNull(covarS(new Double[]{1.0, null}, new Double[2]));
        assertNull(covarS(new Double[]{1.0, 2.0}, new Double[2]));

        assertNull(covarS(new Double[]{9.5}, new Double[]{5.0}));
        assertEquals(24.333333333333332, covarS(new Double[]{1.0, 10.0, 9.0}, new Double[]{1.0, 10.0, 9.0}), DELTA);
        assertEquals(32, covarS(new Double[]{1.0, null, 9.0}, new Double[]{1.0, 10.0, 9.0}), DELTA);
        assertEquals(-20, covarS(new Double[]{-1.0, 10.0, 9.0}, new Double[]{1.0, -15.0, 9.0}), DELTA);

        assertEquals(0.5, covarS(new Long[]{3L, 4L, 8L}, new Long[]{3L, 4L}), DELTA);
        assertEquals(7, covarS(new Byte[]{(byte) 3, (byte) 4, (byte) 8}, new Byte[]{(byte) 3, (byte) 4, (byte) 8}), DELTA);
        assertEquals(7, covarS(new Short[]{(short) 3, (short) 4, (short) 8}, new Short[]{(short) 3, (short) 4, (short) 8}), DELTA);
        assertEquals(7, covarS(new Integer[]{3, 4, 8}, new Integer[]{3, 4, 8}), DELTA);
        assertEquals(7, covarS(new Long[]{3L, 4L, 8L}, new Long[]{3L, 4L, 8L}), DELTA);
        assertEquals(7, covarS(new Float[]{3f, 4f, 8f}, new Float[]{3f, 4f, 8f}), DELTA);
        assertEquals(7, covarS(new Byte[]{(byte) 3, (byte) 4, (byte) 8}, new Short[]{(short) 3, (short) 4, (short) 8}), DELTA);
        assertEquals(0.0,
                covarS(new BigInteger[]{BigInteger.valueOf(10), BigInteger.valueOf(4), BigInteger.valueOf(8)},
                        new BigInteger[]{BigInteger.valueOf(3), BigInteger.valueOf(4), BigInteger.valueOf(8)}), DELTA);
        assertEquals(0.5, covarS(new BigDecimal[]{BigDecimal.valueOf(3), BigDecimal.valueOf(4)}, new BigDecimal[]{BigDecimal.valueOf(3), BigDecimal.valueOf(4)}), DELTA);

    }

    @Test
    public void testPopulationCovariance() {
        assertNull(covarP(null, null));
        assertNull(covarP(new Double[2], new Double[2]));
        assertNull(covarP(new Double[]{1.0, null}, new Double[]{null, null}));
        assertNull(covarP(new Double[]{1.0, 2.0}, new Double[]{null, null}));

        assertEquals(0, covarP(new Double[]{9.5}, new Double[]{5.0}), DELTA);
        assertEquals(16.22222222222222, covarP(new Double[]{1.0, 10.0, 9.0}, new Double[]{1.0, 10.0, 9.0}), DELTA);
        assertEquals(16, covarP(new Double[]{1.0, null, 9.0}, new Double[]{1.0, 10.0, 9.0}), DELTA);
        assertEquals(-13.333333333333334, covarP(new Double[]{-1.0, 10.0, 9.0}, new Double[]{1.0, -15.0, 9.0}), DELTA);

        assertEquals(0.25, covarP(new Long[]{3L, 4L, 8L}, new Long[]{3L, 4L}), DELTA);
        assertEquals(4.666666666666667, covarP(new Byte[]{(byte) 3, (byte) 4, (byte) 8}, new Byte[]{(byte) 3, (byte) 4, (byte) 8}), DELTA);
        assertEquals(4.666666666666667, covarP(new Short[]{(short) 3, (short) 4, (short) 8}, new Short[]{(short) 3, (short) 4, (short) 8}), DELTA);
        assertEquals(4.666666666666667, covarP(new Integer[]{3, 4, 8}, new Integer[]{3, 4, 8}), DELTA);
        assertEquals(4.666666666666667, covarP(new Long[]{3L, 4L, 8L}, new Long[]{3L, 4L, 8L}), DELTA);
        assertEquals(4.666666666666667, covarP(new Float[]{3f, 4f, 8f}, new Float[]{3f, 4f, 8f}), DELTA);
        assertEquals(4.666666666666667, covarP(new Byte[]{(byte) 3, (byte) 4, (byte) 8}, new Short[]{(short) 3, (short) 4, (short) 8}), DELTA);
        assertEquals(0.0,
                covarP(new BigInteger[]{BigInteger.valueOf(10), BigInteger.valueOf(4), BigInteger.valueOf(8)},
                        new BigInteger[]{BigInteger.valueOf(3), BigInteger.valueOf(4), BigInteger.valueOf(8)}), DELTA);
        assertEquals(0.25, covarP(new BigDecimal[]{BigDecimal.valueOf(3), BigDecimal.valueOf(4)}, new BigDecimal[]{BigDecimal.valueOf(3), BigDecimal.valueOf(4)}), DELTA);

    }

    @Test
    public void testCorrelationCoefficient() {
        assertNull(correl(null, null));
        assertNull(correl(new Double[2], new Double[2]));
        assertNull(correl(new Double[]{1.0, null}, new Double[]{null, null}));
        assertNull(correl(new Double[]{1.0, 2.0}, new Double[]{null, null}));

        assertNull(correl(new Double[]{9.5}, new Double[]{5.0}));
        assertEquals(1, correl(new Double[]{1.0, 10.0, 9.0}, new Double[]{1.0, 10.0, 9.0}), DELTA);
        assertEquals(0.9999999999999998, correl(new Double[]{1.0, null, 9.0}, new Double[]{1.0, 10.0, 9.0}), DELTA);
        assertEquals(-0.2690610012503157, correl(new Double[]{-1.0, 10.0, 9.0}, new Double[]{1.0, -15.0, 9.0}), DELTA);

        assertEquals(0.9999999999999998, correl(new Long[]{3L, 4L, 8L}, new Long[]{3L, 4L}), DELTA);
        assertEquals(0.9999999999999999, correl(new Byte[]{(byte) 3, (byte) 4, (byte) 8}, new Byte[]{(byte) 3, (byte) 4, (byte) 8}), DELTA);
        assertEquals(0.9999999999999999, correl(new Short[]{(short) 3, (short) 4, (short) 8}, new Short[]{(short) 3, (short) 4, (short) 8}), DELTA);
        assertEquals(0.9999999999999999, correl(new Integer[]{3, 4, 8}, new Integer[]{3, 4, 8}), DELTA);
        assertEquals(0.9999999999999999, correl(new Long[]{3L, 4L, 8L}, new Long[]{3L, 4L, 8L}), DELTA);
        assertEquals(0.9999999999999999, correl(new Float[]{3f, 4f, 8f}, new Float[]{3f, 4f, 8f}), DELTA);
        assertEquals(0.9999999999999999, correl(new Byte[]{(byte) 3, (byte) 4, (byte) 8}, new Short[]{(short) 3, (short) 4, (short) 8}), DELTA);
        assertEquals(0.0,
                correl(new BigInteger[]{BigInteger.valueOf(10), BigInteger.valueOf(4), BigInteger.valueOf(8)},
                        new BigInteger[]{BigInteger.valueOf(3), BigInteger.valueOf(4), BigInteger.valueOf(8)}), DELTA);
        assertEquals(0.9999999999999998, correl(new BigDecimal[]{BigDecimal.valueOf(3), BigDecimal.valueOf(4)}, new BigDecimal[]{BigDecimal.valueOf(3), BigDecimal.valueOf(4)}), DELTA);
    }

    @Test
    public void testRSQ() {
        assertNull(rsq(null, null));
        assertNull(rsq(new Double[2], new Double[2]));
        assertNull(rsq(new Double[]{1.0, null}, new Double[]{null, null}));
        assertNull(rsq(new Double[]{1.0, 2.0}, new Double[]{null, null}));

        assertNull(rsq(new Double[]{9.5}, new Double[]{5.0}));
        assertEquals(1, rsq(new Double[]{1.0, 10.0, 9.0}, new Double[]{1.0, 10.0, 9.0}), DELTA);
        assertEquals(0.9999999999999996, rsq(new Double[]{1.0, null, 9.0}, new Double[]{1.0, 10.0, 9.0}), DELTA);
        assertEquals(0.07239382239382237, rsq(new Double[]{-1.0, 10.0, 9.0}, new Double[]{1.0, -15.0, 9.0}), DELTA);

        assertEquals(0.9999999999999996, rsq(new Long[]{3L, 4L, 8L}, new Long[]{3L, 4L}), DELTA);
        assertEquals(0.9999999999999998, rsq(new Byte[]{(byte) 3, (byte) 4, (byte) 8}, new Byte[]{(byte) 3, (byte) 4, (byte) 8}), DELTA);
        assertEquals(0.9999999999999998, rsq(new Short[]{(short) 3, (short) 4, (short) 8}, new Short[]{(short) 3, (short) 4, (short) 8}), DELTA);
        assertEquals(0.9999999999999998, rsq(new Integer[]{3, 4, 8}, new Integer[]{3, 4, 8}), DELTA);
        assertEquals(0.9999999999999998, rsq(new Long[]{3L, 4L, 8L}, new Long[]{3L, 4L, 8L}), DELTA);
        assertEquals(0.9999999999999998, rsq(new Float[]{3f, 4f, 8f}, new Float[]{3f, 4f, 8f}), DELTA);
        assertEquals(0.9999999999999998, rsq(new Byte[]{(byte) 3, (byte) 4, (byte) 8}, new Short[]{(short) 3, (short) 4, (short) 8}), DELTA);
        assertEquals(0,
                rsq(new BigInteger[]{BigInteger.valueOf(10), BigInteger.valueOf(4), BigInteger.valueOf(8)},
                        new BigInteger[]{BigInteger.valueOf(3), BigInteger.valueOf(4), BigInteger.valueOf(8)}), DELTA);
        assertEquals(0.9999999999999996, rsq(new BigDecimal[]{BigDecimal.valueOf(3), BigDecimal.valueOf(4)}, new BigDecimal[]{BigDecimal.valueOf(3), BigDecimal.valueOf(4)}), DELTA);
    }

    @Test
    public void testSlope() {
        assertNull(slope(null, null));
        assertNull(slope(new Double[2], new Double[2]));
        assertNull(slope(new Double[]{1.0, null}, new Double[]{null, null}));
        assertNull(slope(new Double[]{1.0, 2.0}, new Double[]{null, null}));

        assertNull(slope(new Double[]{9.5}, new Double[]{5.0}));
        assertEquals(1, slope(new Double[]{1.0, 10.0, 9.0}, new Double[]{1.0, 10.0, 9.0}), DELTA);
        assertEquals(1, slope(new Double[]{1.0, null, 9.0}, new Double[]{1.0, 10.0, 9.0}), DELTA);
        assertEquals(-0.13392857142857142, slope(new Double[]{-1.0, 10.0, 9.0}, new Double[]{1.0, -15.0, 9.0}), DELTA);

        assertEquals(1, slope(new Long[]{3L, 4L, 8L}, new Long[]{3L, 4L}), DELTA);
        assertEquals(1, slope(new Byte[]{(byte) 3, (byte) 4, (byte) 8}, new Byte[]{(byte) 3, (byte) 4, (byte) 8}), 0);
        assertEquals(1, slope(new Short[]{(short) 3, (short) 4, (short) 8}, new Short[]{(short) 3, (short) 4, (short) 8}), DELTA);
        assertEquals(1, slope(new Integer[]{3, 4, 8}, new Integer[]{3, 4, 8}), DELTA);
        assertEquals(1, slope(new Long[]{3L, 4L, 8L}, new Long[]{3L, 4L, 8L}), DELTA);
        assertEquals(1, slope(new Float[]{3f, 4f, 8f}, new Float[]{3f, 4f, 8f}), DELTA);
        assertEquals(1, slope(new Byte[]{(byte) 3, (byte) 4, (byte) 8}, new Short[]{(short) 3, (short) 4, (short) 8}), DELTA);
        assertEquals(0,
                slope(new BigInteger[]{BigInteger.valueOf(10), BigInteger.valueOf(4), BigInteger.valueOf(8)},
                        new BigInteger[]{BigInteger.valueOf(3), BigInteger.valueOf(4), BigInteger.valueOf(8)}), DELTA);
        assertEquals(1, slope(new BigDecimal[]{BigDecimal.valueOf(3), BigDecimal.valueOf(4)}, new BigDecimal[]{BigDecimal.valueOf(3), BigDecimal.valueOf(4)}), DELTA);
    }

    @Test
    public void testIntercept() {
        assertNull(intercept(null, null));
        assertNull(intercept(new Double[2], new Double[2]));
        assertNull(intercept(new Double[]{1.0, null}, new Double[]{null, null}));
        assertNull(intercept(new Double[]{1.0, 2.0}, new Double[]{null, null}));

        assertNull(intercept(new Double[]{9.5}, new Double[]{5.0}));
        assertEquals(0, intercept(new Double[]{1.0, 10.0, 9.0}, new Double[]{1.0, 10.0, 9.0}), DELTA);
        assertEquals(0, intercept(new Double[]{1.0, null, 9.0}, new Double[]{1.0, 10.0, 9.0}), DELTA);
        assertEquals(5.776785714285714, intercept(new Double[]{-1.0, 10.0, 9.0}, new Double[]{1.0, -15.0, 9.0}), DELTA);

        assertEquals(0, intercept(new Long[]{3L, 4L, 8L}, new Long[]{3L, 4L}), DELTA);

        assertEquals(0, intercept(new Byte[]{(byte) 3, (byte) 4, (byte) 8}, new Byte[]{(byte) 3, (byte) 4, (byte) 8}), 0);
        assertEquals(0, intercept(new Short[]{(short) 3, (short) 4, (short) 8}, new Short[]{(short) 3, (short) 4, (short) 8}), DELTA);
        assertEquals(0, intercept(new Integer[]{3, 4, 8}, new Integer[]{3, 4, 8}), DELTA);
        assertEquals(0, intercept(new Long[]{3L, 4L, 8L}, new Long[]{3L, 4L, 8L}), DELTA);
        assertEquals(0, intercept(new Float[]{3f, 4f, 8f}, new Float[]{3f, 4f, 8f}), DELTA);
        assertEquals(0, intercept(new Byte[]{(byte) 3, (byte) 4, (byte) 8}, new Short[]{(short) 3, (short) 4, (short) 8}), DELTA);
        assertEquals(7.333333333333333,
                intercept(new BigInteger[]{BigInteger.valueOf(10), BigInteger.valueOf(4), BigInteger.valueOf(8)},
                        new BigInteger[]{BigInteger.valueOf(3), BigInteger.valueOf(4), BigInteger.valueOf(8)}), DELTA);
        assertEquals(0, intercept(new BigDecimal[]{BigDecimal.valueOf(3), BigDecimal.valueOf(4)}, new BigDecimal[]{BigDecimal.valueOf(3), BigDecimal.valueOf(4)}), DELTA);
    }

    @Test
    public void testForecast() {
        assertNull(forecast(null, null, null));
        assertNull(forecast(0, new Double[2], new Double[2]));
        assertNull(forecast(1.0, new Double[]{1.0, null}, new Double[]{null, null}));
        assertNull(forecast(1.0, new Double[]{1.0, 2.0}, new Double[]{null, null}));

        assertNull(forecast(1.0, new Double[]{9.5}, new Double[]{5.0}));
        assertEquals(1.0, forecast(1.0, new Double[]{1.0, 10.0, 9.0}, new Double[]{1.0, 10.0, 9.0}), DELTA);
        assertEquals(1, forecast(1.0, new Double[]{1.0, null, 9.0}, new Double[]{1.0, 10.0, 9.0}), DELTA);
        assertEquals(5.642857142857143, forecast(1.0, new Double[]{-1.0, 10.0, 9.0}, new Double[]{1.0, -15.0, 9.0}), DELTA);

        assertEquals(3, forecast(3L, new Long[]{3L, 4L, 8L}, new Long[]{3L, 4L}), DELTA);

        assertEquals(3.0, forecast((byte) 3, new Byte[]{(byte) 3, (byte) 4, (byte) 8}, new Byte[]{(byte) 3, (byte) 4, (byte) 8}), 0);
        assertEquals(3.0, forecast((short) 3, new Short[]{(short) 3, (short) 4, (short) 8}, new Short[]{(short) 3, (short) 4, (short) 8}), DELTA);
        assertEquals(3.0, forecast(3, new Integer[]{3, 4, 8}, new Integer[]{3, 4, 8}), DELTA);
        assertEquals(3.0, forecast(3L, new Long[]{3L, 4L, 8L}, new Long[]{3L, 4L, 8L}), DELTA);
        assertEquals(3.0, forecast(3f, new Float[]{3f, 4f, 8f}, new Float[]{3f, 4f, 8f}), DELTA);
        assertEquals(3.0, forecast((byte) 3, new Byte[]{(byte) 3, (byte) 4, (byte) 8}, new Short[]{(short) 3, (short) 4, (short) 8}), DELTA);
        assertEquals(7.333333333333333,
                forecast(BigInteger.valueOf(3), new BigInteger[]{BigInteger.valueOf(10), BigInteger.valueOf(4), BigInteger.valueOf(8)},
                        new BigInteger[]{BigInteger.valueOf(3), BigInteger.valueOf(4), BigInteger.valueOf(8)}), DELTA);
        assertEquals(3.0, forecast(BigDecimal.valueOf(3), new BigDecimal[]{BigDecimal.valueOf(3), BigDecimal.valueOf(4)}, new BigDecimal[]{BigDecimal.valueOf(3), BigDecimal.valueOf(4)}), DELTA);
    }
}
