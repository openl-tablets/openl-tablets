package org.openl.rules.util;

import static java.math.MathContext.DECIMAL128;

import static org.openl.rules.util.Covar.sampleCovariance;
import static org.openl.rules.util.InputStatistics.loadInputStats;
import static org.openl.rules.util.StdDev.stdevS;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.openl.rules.util.InputStatistics.InputStatsDouble;

/**
 * A utility class that provides statistical correlation calculations between arrays of numbers.
 * Supports various numeric types including primitive numbers wrapped in Number class, BigDecimal,
 * and BigInteger. The class implements both Pearson correlation coefficient and R-squared calculations.
 */
public class Corel {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private Corel() {
    }

    /**
     * Calculates the Pearson correlation coefficient between two arrays of numbers.
     * The correlation coefficient measures the strength and direction of the linear relationship
     * between two variables.
     *
     * @param y the variable array(extending Number)
     * @param x the variable array(extending Number)
     * @return the correlation coefficient as a Double, or null if the calculation is not possible
     */
    public static <X extends Number, Y extends Number> Double correl(Y[] y, X[] x) {
        InputStatsDouble inputStats = loadInputStats(y, x);
        if (inputStats == null) {
            return null;
        }
        Double covariance = sampleCovariance(inputStats);
        Double deviationX = stdevS(inputStats.x);
        Double deviationY = stdevS(inputStats.y);
        return covariance == null || deviationX == null || deviationY == null ? null : covariance / (deviationX * deviationY);
    }

    /**
     * Calculates the Pearson correlation coefficient between two arrays of BigDecimal numbers.
     *
     * @param y the variable array
     * @param x the variable array
     * @return the correlation coefficient as a BigDecimal, or null if the calculation is not possible
     */
    public static BigDecimal correl(BigDecimal[] y, BigDecimal[] x) {
        return correl(loadInputStats(y, x));
    }

    /**
     * Calculates the Pearson correlation coefficient between two arrays of BigInteger numbers.
     *
     * @param y the variable array
     * @param x the variable array
     * @return the correlation coefficient as a BigDecimal, or null if the calculation is not possible
     */
    public static BigDecimal correl(BigInteger[] y, BigInteger[] x) {
        return correl(loadInputStats(y, x));
    }

    /**
     * Internal helper method to calculate correlation using BigDecimal statistics.
     */
    private static BigDecimal correl(InputStatistics.InputStatsBigDecimal inputStats) {
        if (inputStats == null) {
            return null;
        }
        BigDecimal covariance = sampleCovariance(inputStats);
        BigDecimal deviationX = stdevS(inputStats.x);
        BigDecimal deviationY = BigDecimal.ZERO.equals(deviationX) ? null : stdevS(inputStats.y);
        return covariance == null || deviationX == null || deviationY == null ? null : covariance.divide(deviationX.multiply(deviationY), DECIMAL128);
    }

    /**
     * Calculates the R-squared (coefficient of determination) value between two arrays of numbers.
     * R-squared represents the proportion of variance in the dependent variable that is predictable
     * from the independent variable.
     *
     * @param y the variable array(extending Number)
     * @param x the variable array(extending Number)
     * @return the R-squared value as a Double, or null if the calculation is not possible
     */
    public static <X extends Number, Y extends Number> Double rsq(Y[] y, X[] x) {
        Double result = correl(y, x);
        return result == null ? null : result * result;
    }

    /**
     * Calculates the R-squared value between two arrays of BigDecimal numbers.
     *
     * @param y the variable array
     * @param x the variable array
     * @return the R-squared value as a BigDecimal, or null if the calculation is not possible
     */
    public static BigDecimal rsq(BigDecimal[] y, BigDecimal[] x) {
        BigDecimal result = correl(y, x);
        return result == null ? null : result.pow(2, DECIMAL128);
    }

    /**
     * Calculates the R-squared value between two arrays of BigInteger numbers.
     *
     * @param y the variable array
     * @param x the variable array
     * @return the R-squared value as a BigDecimal, or null if the calculation is not possible
     */
    public static BigDecimal rsq(BigInteger[] y, BigInteger[] x) {
        BigDecimal result = correl(y, x);
        return result == null ? null : result.pow(2, DECIMAL128);
    }
}
