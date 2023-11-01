package org.openl.rules.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import static org.openl.rules.util.Avg.avg;
import static org.openl.rules.util.Statistics.max;
import static org.openl.rules.util.Statistics.min;
import static org.openl.rules.util.Statistics.populationVariance;
import static org.openl.rules.util.Statistics.samplePopulationDeviation;
import static org.openl.rules.util.Statistics.sampleVariance;
import static org.openl.rules.util.Statistics.standardPopulationDeviation;
import static org.openl.rules.util.Sum.sum;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

import org.junit.jupiter.api.Test;

public class StatisticsTest {

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
        assertNull(standardPopulationDeviation(null));
        assertNull(standardPopulationDeviation(new Double[0]));
        assertNull(standardPopulationDeviation(new Double[] { null, null }));

        assertEquals(Double.valueOf(4.0276819911981905), standardPopulationDeviation(1, 10, 9));
        assertEquals(Double.valueOf(0.0), standardPopulationDeviation(9.5));
        assertEquals(Double.valueOf(3), standardPopulationDeviation(8, null, 2));
        assertEquals(Double.valueOf(7.788880963698615), standardPopulationDeviation(-10.0, 6.0, 7.0));

        assertEquals(Double.valueOf(2.160246899469287), standardPopulationDeviation((byte) 3, (byte) 4, (byte) 8));
        assertEquals(Double.valueOf(2.160246899469287), standardPopulationDeviation((short) 3, (short) 4, (short) 8));
        assertEquals(Double.valueOf(2.160246899469287), standardPopulationDeviation(3, 4, 8));
        assertEquals(Double.valueOf(2.160246899469287), standardPopulationDeviation(3L, 4L, 8L));
        assertEquals(Double.valueOf(2.160246899469287), standardPopulationDeviation(3f, 4f, 8f));
        assertEquals(Double.valueOf(2.160246899469287), standardPopulationDeviation(3d, 4d, 8d));
        assertEquals(Double.valueOf(2.160246899469287), standardPopulationDeviation((byte) 3, (short) 4, 8));
        assertEquals(Double.valueOf(2.160246899469287),
            standardPopulationDeviation(BigInteger.valueOf(3), BigInteger.valueOf(4), BigInteger.valueOf(8)));
        assertEquals(Double.valueOf(0.5), standardPopulationDeviation(BigDecimal.valueOf(3), BigDecimal.valueOf(4)));
        assertEquals(Double.valueOf(2.0548046676563256),
            standardPopulationDeviation(BigInteger.valueOf(3), BigInteger.valueOf(5), BigInteger.valueOf(8)));

    }

    @Test
    public void testPopulationVariance() {
        assertNull(populationVariance(null));
        assertNull(populationVariance(new Double[0]));
        assertNull(populationVariance(new Double[] { null, null }));

        assertEquals(Double.valueOf(16.22222222222222), populationVariance(1, 10, 9));
        assertEquals(Double.valueOf(0.0), populationVariance(9.5));
        assertEquals(Double.valueOf(9.0), populationVariance(8, null, 2));
        assertEquals(Double.valueOf(60.666666666666664), populationVariance(-10.0, 6.0, 7.0));

        assertEquals(Double.valueOf(4.666666666666667), populationVariance((byte) 3, (byte) 4, (byte) 8));
        assertEquals(Double.valueOf(4.666666666666667), populationVariance((short) 3, (short) 4, (short) 8));
        assertEquals(Double.valueOf(4.666666666666667), populationVariance(3, 4, 8));
        assertEquals(Double.valueOf(4.666666666666667), populationVariance(3L, 4L, 8L));
        assertEquals(Double.valueOf(4.666666666666667), populationVariance(3f, 4f, 8f));
        assertEquals(Double.valueOf(4.666666666666667), populationVariance(3d, 4d, 8d));
        assertEquals(Double.valueOf(4.666666666666667), populationVariance((byte) 3, (short) 4, 8));
        assertEquals(Double.valueOf(4.666666666666667),
                populationVariance(BigInteger.valueOf(3), BigInteger.valueOf(4), BigInteger.valueOf(8)));
        assertEquals(Double.valueOf(0.25), populationVariance(BigDecimal.valueOf(3), BigDecimal.valueOf(4)));
        assertEquals(Double.valueOf(4.222222222222222),
                populationVariance(BigInteger.valueOf(3), BigInteger.valueOf(5), BigInteger.valueOf(8)));

    }

    @Test
    public void testSamplePopulationDeviation() {
        assertNull(samplePopulationDeviation(null));
        assertNull(samplePopulationDeviation(new Double[0]));
        assertNull(samplePopulationDeviation(new Double[] { null, null }));

        assertEquals(Double.valueOf(4.932882862316247), samplePopulationDeviation(1, 10, 9));
        assertEquals(Double.NaN, samplePopulationDeviation(9.5), 0);
        assertEquals(Double.valueOf(4.242640687119285), samplePopulationDeviation(8, null, 2));
        assertEquals(Double.valueOf(9.539392014169456), samplePopulationDeviation(-10.0, 6.0, 7.0));

        assertEquals(Double.valueOf(2.6457513110645907), samplePopulationDeviation((byte) 3, (byte) 4, (byte) 8));
        assertEquals(Double.valueOf(2.6457513110645907), samplePopulationDeviation((short) 3, (short) 4, (short) 8));
        assertEquals(Double.valueOf(2.6457513110645907), samplePopulationDeviation(3, 4, 8));
        assertEquals(Double.valueOf(2.6457513110645907), samplePopulationDeviation(3L, 4L, 8L));
        assertEquals(Double.valueOf(2.6457513110645907), samplePopulationDeviation(3f, 4f, 8f));
        assertEquals(Double.valueOf(2.6457513110645907), samplePopulationDeviation(3d, 4d, 8d));
        assertEquals(Double.valueOf(2.6457513110645907), samplePopulationDeviation((byte) 3, (short) 4, 8));
        assertEquals(Double.valueOf(2.6457513110645907),
                samplePopulationDeviation(BigInteger.valueOf(3), BigInteger.valueOf(4), BigInteger.valueOf(8)));
        assertEquals(Double.valueOf(0.7071067811865476), samplePopulationDeviation(BigDecimal.valueOf(3), BigDecimal.valueOf(4)));
        assertEquals(Double.valueOf(2.516611478423583),
                samplePopulationDeviation(BigInteger.valueOf(3), BigInteger.valueOf(5), BigInteger.valueOf(8)));

    }

    @Test
    public void testSampleVariance() {
        assertNull(sampleVariance(null));
        assertNull(sampleVariance(new Double[0]));
        assertNull(sampleVariance(new Double[] { null, null }));

        assertEquals(Double.valueOf(24.333333333333332), sampleVariance(1, 10, 9));
        assertEquals(Double.NaN, sampleVariance(9.5), 0);
        assertEquals(Double.valueOf(18.0), sampleVariance(8, null, 2));
        assertEquals(Double.valueOf(91.0), sampleVariance(-10.0, 6.0, 7.0));

        assertEquals(Double.valueOf(7), sampleVariance((byte) 3, (byte) 4, (byte) 8));
        assertEquals(Double.valueOf(7), sampleVariance((short) 3, (short) 4, (short) 8));
        assertEquals(Double.valueOf(7), sampleVariance(3, 4, 8));
        assertEquals(Double.valueOf(7), sampleVariance(3L, 4L, 8L));
        assertEquals(Double.valueOf(7), sampleVariance(3f, 4f, 8f));
        assertEquals(Double.valueOf(7), sampleVariance(3d, 4d, 8d));
        assertEquals(Double.valueOf(7), sampleVariance((byte) 3, (short) 4, 8));
        assertEquals(Double.valueOf(7),
                sampleVariance(BigInteger.valueOf(3), BigInteger.valueOf(4), BigInteger.valueOf(8)));
        assertEquals(Double.valueOf(0.5), sampleVariance(BigDecimal.valueOf(3), BigDecimal.valueOf(4)));
        assertEquals(Double.valueOf(6.333333333333333),
                sampleVariance(BigInteger.valueOf(3), BigInteger.valueOf(5), BigInteger.valueOf(8)));

    }
}
