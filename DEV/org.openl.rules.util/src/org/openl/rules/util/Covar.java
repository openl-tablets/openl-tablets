package org.openl.rules.util;

import static org.openl.rules.util.InputStatistics.loadInputStats;
import static org.openl.rules.util.Statistics.biProcess;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

import org.openl.rules.util.InputStatistics.InputStatsBigDecimal;
import org.openl.rules.util.InputStatistics.InputStatsDouble;
import org.openl.rules.util.InputStatistics.InputStatsFloat;
import org.openl.rules.util.Statistics.Result;

/**
 * Utility class providing methods to calculate covariance between two arrays of numbers.
 * Supports both sample covariance (covarS) and population covariance (covarP) calculations.
 *
 * <p>Covariance measures how two variables change together. A positive covariance indicates
 * that the variables tend to move in the same direction, while a negative covariance indicates
 * they tend to move in opposite directions.</p>
 *
 * <p>This class supports various numeric types including:
 * <ul>
 *   <li>Generic Number types (Double, Integer, etc.)</li>
 *   <li>BigDecimal calculations</li>
 *   <li>BigInteger calculations</li>
 * </ul></p>
 */
public final class Covar {

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private Covar() {
    }

    /**
     * Calculates the sample covariance between two arrays of numbers.
     * Sample covariance is calculated using (n-1) as the denominator, where n is the number of non-null pairs.
     *
     * @param y the variable array(extending Number)
     * @param x the variable array(extending Number)
     * @return the sample covariance as a Double, or null
     */
    public static <X extends Number, Y extends Number> Double covarS(Y[] y, X[] x) {
        return sampleCovariance(loadInputStats(y, x));
    }

    /**
     * Calculates the sample covariance between two arrays of BigDecimal numbers.
     * Sample covariance is calculated using (n-1) as the denominator, where n is the number of non-null pairs.
     *
     * @param y the variable array of BigDecimal values
     * @param x the variable array of BigDecimal values
     * @return the sample covariance as a BigDecimal, or null
     */
    public static BigDecimal covarS(BigDecimal[] y, BigDecimal[] x) {
        return sampleCovariance(loadInputStats(y, x));
    }

    /**
     * Calculates the sample covariance between two arrays of BigInteger numbers.
     * Sample covariance is calculated using (n-1) as the denominator, where n is the number of non-null pairs.
     *
     * @param y the variable array of BigInteger values
     * @param x the variable array of BigInteger values
     * @return the sample covariance as a BigDecimal, or null
     */
    public static BigDecimal covarS(BigInteger[] y, BigInteger[] x) {
        return sampleCovariance(loadInputStats(y, x));
    }

    /**
     * Calculates the sample covariance between two arrays of Float numbers.
     * Sample covariance is calculated using (n-1) as the denominator, where n is the number of non-null pairs.
     *
     * @param y the variable array of Float values
     * @param x the variable array of Float values
     * @return the sample covariance as a Float, or null
     */
    public static Float covarS(Float[] y, Float[] x) {
        return sampleCovariance(loadInputStats(y, x));
    }

    static Double sampleCovariance(InputStatsDouble inputStats) {
        //covarS
        if (inputStats == null) {
            return null;
        }
        Double avgX = inputStats.getAvgX();
        Double avgY = inputStats.getAvgY();
        return biProcess(inputStats.y, inputStats.x, new Result<>() {
            @Override
            public void processNonNull(Double y, Double x) {
                result = covariance(y, x, avgY, avgX, result);
            }

            @Override
            public Double result() {
                return counter <= 1 ? null : (result == null ? null : result / (counter - 1));
            }
        });
    }

    static Float sampleCovariance(InputStatsFloat inputStats) {
        //covarS
        if (inputStats == null) {
            return null;
        }
        Float avgX = inputStats.getAvgX();
        Float avgY = inputStats.getAvgY();
        return biProcess(inputStats.y, inputStats.x, new Result<>() {
            @Override
            public void processNonNull(Float y, Float x) {
                result = covariance(y, x, avgY, avgX, result);
            }

            @Override
            public Float result() {
                return counter <= 1 ? null : (result == null ? null : result / (counter - 1));
            }
        });
    }

    static BigDecimal sampleCovariance(InputStatsBigDecimal inputStats) {
        //covarS
        if (inputStats == null) {
            return null;
        }
        BigDecimal avgX = inputStats.getAvgX();
        BigDecimal avgY = inputStats.getAvgY();
        return biProcess(inputStats.y, inputStats.x, new Result<>() {
            @Override
            public void processNonNull(BigDecimal y, BigDecimal x) {
                result = covariance(y, x, avgY, avgX, result);
            }

            @Override
            public BigDecimal result() {
                return counter <= 1 ? null : (result == null || BigDecimal.ZERO.equals(result) ? result : result.divide(BigDecimal.valueOf(counter - 1L), MathContext.DECIMAL128));
            }
        });

    }

    private static Double covariance(Double y, Double x, Double avgY, Double avgX, Double result) {
        var tmp = (x - avgX) * (y - avgY);
        return result == null ? tmp : result + tmp;
    }

    private static Float covariance(Float y, Float x, Float avgY, Float avgX, Float result) {
        var tmp = (x - avgX) * (y - avgY);
        return result == null ? tmp : result + tmp;
    }

    private static BigDecimal covariance(BigDecimal y, BigDecimal x, BigDecimal avgY, BigDecimal avgX, BigDecimal result) {
        var tmp = (x.subtract(avgX)).multiply(y.subtract(avgY));
        return result == null ? tmp : result.add(tmp);
    }

    /**
     * Calculates the population covariance between two arrays of numbers.
     * Population covariance is calculated using n as the denominator, where n is the number of non-null pairs.
     *
     * @param y the variable array(extending Number)
     * @param x the variable array(extending Number)
     * @return the population covariance as a Double, or null
     */
    public static <X extends Number, Y extends Number> Double covarP(Y[] y, X[] x) {
        //covarP
        InputStatsDouble inputStats = loadInputStats(y, x);
        if (inputStats == null) {
            return null;
        }
        Double avgX = inputStats.getAvgX();
        Double avgY = inputStats.getAvgY();
        return biProcess(inputStats.y, inputStats.x, new Result<Number, Double>() {
            @Override
            public void processNonNull(Number y, Number x) {
                result = covariance(y.doubleValue(), x.doubleValue(), avgY, avgX, result);
            }

            @Override
            public Double result() {
                return result == null ? null : result / counter;
            }
        });
    }

    /**
     * Calculates the population covariance between two arrays of BigDecimal numbers.
     * Population covariance is calculated using n as the denominator, where n is the number of non-null pairs.
     *
     * @param y the variable array of BigDecimal values
     * @param x the variable array of BigDecimal values
     * @return the population covariance as a BigDecimal, or null
     */
    public static BigDecimal covarP(BigDecimal[] y, BigDecimal[] x) {
        //covarP
        InputStatsBigDecimal inputStats = loadInputStats(y, x);
        return covarP(inputStats);
    }

    /**
     * Calculates the population covariance between two arrays of BigInteger numbers.
     * Population covariance is calculated using n as the denominator, where n is the number of non-null pairs.
     *
     * @param y the variable array of BigInteger values
     * @param x the variable array of BigInteger values
     * @return the population covariance as a BigDecimal, or null
     */
    public static BigDecimal covarP(BigInteger[] y, BigInteger[] x) {
        //covarP
        InputStatsBigDecimal inputStats = loadInputStats(y, x);
        return covarP(inputStats);
    }

    /**
     * Calculates the population covariance between two arrays of Float numbers.
     * Population covariance is calculated using n as the denominator, where n is the number of non-null pairs.
     *
     * @param y the variable array of Float values
     * @param x the variable array of Float values
     * @return the population covariance as a Float, or null
     */
    public static Float covarP(Float[] y, Float[] x) {
        //covarP
        InputStatsFloat inputStats = loadInputStats(y, x);
        return covarP(inputStats);
    }

    private static BigDecimal covarP(InputStatsBigDecimal inputStats) {
        if (inputStats == null) {
            return null;
        }
        BigDecimal avgX = inputStats.getAvgX();
        BigDecimal avgY = inputStats.getAvgY();
        return biProcess(inputStats.y, inputStats.x, new Result<>() {
            @Override
            public void processNonNull(BigDecimal y, BigDecimal x) {
                result = covariance(y, x, avgY, avgX, result);
            }

            @Override
            public BigDecimal result() {
                return result == null || BigDecimal.ZERO.equals(result) ? result : result.divide(BigDecimal.valueOf(counter), MathContext.DECIMAL128);
            }
        });
    }

    private static Float covarP(InputStatsFloat inputStats) {
        if (inputStats == null) {
            return null;
        }
        Float avgX = inputStats.getAvgX();
        Float avgY = inputStats.getAvgY();
        return biProcess(inputStats.y, inputStats.x, new Result<>() {
            @Override
            public void processNonNull(Float y, Float x) {
                result = covariance(y, x, avgY, avgX, result);
            }

            @Override
            public Float result() {
                return result == null ? null : result / counter;
            }
        });
    }

}
