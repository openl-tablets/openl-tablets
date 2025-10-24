package org.openl.rules.util;

import static org.openl.rules.util.Covar.sampleCovariance;
import static org.openl.rules.util.InputStatistics.loadInputStats;
import static org.openl.rules.util.Var.varS;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

import org.openl.rules.util.InputStatistics.InputStatsFloat;

/**
 * Utility class for calculating the slope (beta coefficient) of a linear regression line.
 *
 * <p>The slope represents the change in Y for a one-unit change in X. It is calculated as:
 * slope = covariance(x,y) / variance(x)</p>
 *
 * <p>This class supports various numeric types including:
 * <ul>
 *   <li>Generic Number types (Double, Integer, etc.)</li>
 *   <li>BigDecimal calculations</li>
 *   <li>BigInteger calculations</li>
 * </ul></p>
 */
public final class Slope {

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private Slope() {
        //Utility Class
    }

    /**
     * Calculates the slope of the regression line for two arrays of numbers.
     *
     * @param y the variable array(extending Number)
     * @param x the variable array(extending Number)
     * @return the slope as a Double, or null
     */
    public static <X extends Number, Y extends Number> Double slope(Y[] y, X[] x) {
        return slope(loadInputStats(y, x));
    }

    /**
     * Calculates the slope of the regression line for two arrays of BigDecimal numbers.
     *
     * @param y the variable array
     * @param x the variable array
     * @return the slope as a BigDecimal, or null
     */
    public static BigDecimal slope(BigDecimal[] y, BigDecimal[] x) {
        return slope(loadInputStats(y, x));
    }

    /**
     * Calculates the slope of the regression line for two arrays of BigInteger numbers.
     *
     * @param y the variable array
     * @param x the variable array
     * @return the slope as a BigDecimal, or null
     */
    public static BigDecimal slope(BigInteger[] y, BigInteger[] x) {
        return slope(loadInputStats(y, x));
    }

    /**
     * Calculates the slope of the regression line for two arrays of Float numbers.
     *
     * @param y the variable array
     * @param x the variable array
     * @return the slope as a Float, or null
     */
    public static Float slope(Float[] y, Float[] x) {
        return slope(loadInputStats(y, x));
    }

    static Double slope(InputStatistics.InputStatsDouble inputStats) {
        if (inputStats == null) {
            return null;
        }
        Double sampleCovariance = sampleCovariance(inputStats);
        Double sampleVariance = sampleCovariance == null ? null : varS(inputStats.x);
        return sampleVariance == null || sampleVariance == 0 ? null : sampleCovariance / sampleVariance;

    }

    static Float slope(InputStatsFloat inputStats) {
        if (inputStats == null) {
            return null;
        }
        Float sampleCovariance = sampleCovariance(inputStats);
        Float sampleVariance = sampleCovariance == null ? null : varS(inputStats.x);
        return sampleVariance == null || sampleVariance == 0 ? null : sampleCovariance / sampleVariance;

    }

    static BigDecimal slope(InputStatistics.InputStatsBigDecimal inputStats) {
        if (inputStats == null) {
            return null;
        }
        BigDecimal sampleCovariance = sampleCovariance(inputStats);
        BigDecimal sampleVariance = sampleCovariance == null ? null : varS(inputStats.x);
        return sampleVariance == null || BigDecimal.ZERO.equals(sampleVariance) ? null : sampleCovariance.divide(sampleVariance, MathContext.DECIMAL128);
    }
}
