package org.openl.rules.util;

import static org.openl.rules.util.InputStatistics.loadInputStats;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.openl.rules.util.InputStatistics.InputStatsDouble;
import org.openl.rules.util.InputStatistics.InputStatsFloat;

/**
 * Utility class for calculating the Y-intercept (alpha coefficient) of a linear regression line.
 *
 * <p>The Y-intercept represents the predicted value of Y when X equals zero. It is calculated as:
 * intercept = avgY - (slope * avgX)</p>
 *
 * <p>This class supports various numeric types including:
 * <ul>
 *   <li>Generic Number types (Double, Integer, etc.)</li>
 *   <li>BigDecimal calculations</li>
 *   <li>BigInteger calculations</li>
 * </ul></p>
 */
public final class Intercept {

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private Intercept() {
        //Utility Class
    }

    /**
     * Calculates the Y-intercept of the regression line for two arrays of numbers.
     *
     * @param y the variable array(extending Number)
     * @param x the variable array(extending Number)
     * @return the Y-intercept as a Double, or null
     */
    public static <X extends Number, Y extends Number> Double intercept(Y[] y, X[] x) {
        InputStatsDouble inputStats = loadInputStats(y, x);
        if (inputStats == null || inputStats.y.length <= 1) {
            return null;
        }
        Double avgY = inputStats.getAvgY();
        Double avgX = inputStats.getAvgX();
        Double slopeB = avgX == null ? null : Slope.slope(inputStats);
        return slopeB == null ? null : avgY - (slopeB * avgX);
    }

    /**
     * Calculates the Y-intercept of the regression line for two arrays of BigDecimal numbers.
     *
     * @param y the variable array
     * @param x the variable array
     * @return the Y-intercept as a BigDecimal, or null
     */
    public static BigDecimal intercept(BigDecimal[] y, BigDecimal[] x) {
        InputStatistics.InputStatsBigDecimal inputStats = loadInputStats(y, x);
        return intercept(inputStats);
    }

    /**
     * Calculates the Y-intercept of the regression line for two arrays of BigInteger numbers.
     *
     * @param y the variable array
     * @param x the variable array
     * @return the Y-intercept as a BigDecimal, or null
     */
    public static BigDecimal intercept(BigInteger[] y, BigInteger[] x) {
        InputStatistics.InputStatsBigDecimal inputStats = loadInputStats(y, x);
        return intercept(inputStats);
    }

    /**
     * Calculates the Y-intercept of the regression line for two arrays of Float numbers.
     *
     * @param y the variable array
     * @param x the variable array
     * @return the Y-intercept as a Float, or null
     */
    public static Float intercept(Float[] y, Float[] x) {
        InputStatsFloat inputStats = loadInputStats(y, x);
        return intercept(inputStats);
    }

    private static Float intercept(InputStatsFloat inputStats) {
        if (inputStats == null || inputStats.y.length <= 1) {
            return null;
        }
        Float avgY = inputStats.getAvgY();
        Float avgX = inputStats.getAvgX();
        Float slopeB = avgX == null ? null : Slope.slope(inputStats);
        return slopeB == null ? null : avgY - (slopeB * avgX);
    }

    private static BigDecimal intercept(InputStatistics.InputStatsBigDecimal inputStats) {
        if (inputStats == null || inputStats.y.length <= 1) {
            return null;
        }
        BigDecimal avgY = inputStats.getAvgY();
        BigDecimal avgX = inputStats.getAvgX();
        BigDecimal slopeB = avgX == null ? null : Slope.slope(inputStats);
        return slopeB == null ? null : avgY.subtract(slopeB.multiply(avgX));
    }
}
