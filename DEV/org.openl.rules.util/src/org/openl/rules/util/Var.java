package org.openl.rules.util;

import static java.math.MathContext.DECIMAL128;

import static org.openl.rules.util.Avg.avg;
import static org.openl.rules.util.Statistics.process;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.openl.rules.util.Statistics.Result;

/**
 * Utility class for calculating variance of numeric arrays.
 *
 * <p>Variance measures how far a set of numbers are spread out from their average value.
 * This class provides both population (varP) and sample (varS) variance calculations.</p>
 *
 * <p>The difference between population and sample variance:
 * <ul>
 *   <li>Population (varP) - uses n as denominator, used when data represents entire population</li>
 *   <li>Sample (varS) - uses (n-1) as denominator, used when data represents a sample of population</li>
 * </ul></p>
 */
public final class Var {

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private Var() {
        //Utility Class
    }

    /**
     * Calculates the sample variance of an array of numbers.
     * Sample variance uses (n-1) as denominator to provide an unbiased estimate of population variance.
     *
     * @param values the array of values(extending Number)
     * @return the sample variance as a Double, or null
     */
    public static <T extends Number> Double varS(T... values) {
        Double avg = avg(values);
        return avg == null || values.length == 1 ? null : process(values, new Result<>() {
            @Override
            public void processNonNull(T value) {
                result = variance(value.doubleValue(), avg, result);
            }

            @Override
            public Double result() {
                return result == null ? null : result / (counter - 1);
            }
        });
    }

    /**
     * Calculates the sample variance of an array of numbers.
     * Sample variance uses (n-1) as denominator to provide an unbiased estimate of population variance.
     *
     * @param values the array of Float values
     * @return the sample variance as a Float, or null
     */
    public static Float varS(Float... values) {
        Float avg = avg(values);
        return avg == null || values.length == 1 ? null : process(values, new Result<>() {
            @Override
            public void processNonNull(Float value) {
                result = variance(value, avg, result);
            }

            @Override
            public Float result() {
                return result == null ? null : result / (counter - 1);
            }
        });
    }

    /**
     * Calculates the sample variance of an array of BigInteger numbers.
     *
     * @param values the array of BigInteger values
     * @return the sample variance as a BigDecimal, or null if:
     * <ul>
     *   <li>Input array is null</li>
     *   <li>Array contains only null values</li>
     *   <li>Array contains only one value</li>
     * </ul>
     */
    public static BigDecimal varS(BigInteger... values) {
        BigDecimal avg = avg(values);
        return avg == null || values.length == 1 ? null : process(values, new Result<>() {
            @Override
            public void processNonNull(BigInteger value) {
                result = variance(new BigDecimal(value), avg, result);
            }

            @Override
            public BigDecimal result() {
                return result == null || BigDecimal.ZERO.equals(result) ? result : result.divide(BigDecimal.valueOf(counter - 1L), DECIMAL128);
            }
        });
    }

    /**
     * Calculates the sample variance of an array of BigDecimal numbers.
     *
     * @param values the array of BigDecimal values
     * @return the sample variance as a BigDecimal, or null if:
     * <ul>
     *   <li>Input array is null</li>
     *   <li>Array contains only null values</li>
     *   <li>Array contains only one value</li>
     * </ul>
     */
    public static BigDecimal varS(BigDecimal... values) {
        BigDecimal avg = avg(values);
        return avg == null || values.length == 1 ? null : process(values, new Result<>() {
            @Override
            public void processNonNull(BigDecimal value) {
                result = variance(value, avg, result);
            }

            @Override
            public BigDecimal result() {
                return result == null || BigDecimal.ZERO.equals(result) ? result : result.divide(BigDecimal.valueOf(counter - 1L), DECIMAL128);
            }
        });
    }

    /**
     * Calculates the population variance of an array of numbers.
     * Population variance uses n as denominator and is used when the data represents the entire population.
     *
     * @param values the array of values(extending Number)
     * @return the population variance as a Double, or null
     */
    public static <T extends Number> Double varP(T... values) {
        Double avg = avg(values);
        if (avg == null) {
            return null;
        }
        if (values.length == 1) {
            return 0.0;
        }
        return process(values, new Result<>() {
            @Override
            public void processNonNull(T value) {
                result = variance(value.doubleValue(), avg, result);
            }

            @Override
            public Double result() {
                return result == null ? null : result / counter;
            }
        });
    }

    /**
     * Calculates the population variance of an array of numbers.
     * Population variance uses n as denominator and is used when the data represents the entire population.
     *
     * @param values the array of Float values
     * @return the population variance as a Float, or null
     */
    public static Float varP(Float... values) {
        Float avg = avg(values);
        if (avg == null) {
            return null;
        }
        if (values.length == 1) {
            return 0.0f;
        }
        return process(values, new Result<>() {
            @Override
            public void processNonNull(Float value) {
                result = variance(value, avg, result);
            }

            @Override
            public Float result() {
                return result == null ? null : result / counter;
            }
        });
    }

    /**
     * Calculates the population variance of an array of BigDecimal numbers.
     *
     * @param values the array of BigDecimal values
     * @return the population variance as a BigDecimal, or null
     */
    public static BigDecimal varP(BigDecimal... values) {
        BigDecimal avg = avg(values);
        if (avg == null) {
            return null;
        }
        if (values.length == 1) {
            return BigDecimal.ZERO;
        }
        return process(values, new Result<>() {
            @Override
            public void processNonNull(BigDecimal value) {
                result = variance(value, avg, result);
            }

            @Override
            public BigDecimal result() {
                return result == null || BigDecimal.ZERO.equals(result) ? result : result.divide(BigDecimal.valueOf(counter), DECIMAL128);
            }
        });
    }

    /**
     * Calculates the population variance of an array of BigInteger numbers.
     *
     * @param values the array of BigInteger values
     * @return the population variance as a BigDecimal, or null
     */
    public static BigDecimal varP(BigInteger... values) {
        BigDecimal avg = avg(values);
        if (avg == null) {
            return null;
        }
        if (values.length == 1) {
            return BigDecimal.ZERO;
        }
        return process(values, new Result<>() {
            @Override
            public void processNonNull(BigInteger value) {
                result = variance(new BigDecimal(value), avg, result);
            }

            @Override
            public BigDecimal result() {
                return result == null || BigDecimal.ZERO.equals(result) ? null : result.divide(BigDecimal.valueOf(counter), DECIMAL128);
            }
        });
    }

    private static Double variance(Double value, Double avg, Double result) {
        var tmp = (value - avg);
        tmp *= tmp;
        return result == null ? tmp : result + tmp;
    }

    private static Float variance(Float value, Float avg, Float result) {
        var tmp = (value - avg);
        tmp *= tmp;
        return result == null ? tmp : result + tmp;
    }

    private static BigDecimal variance(BigDecimal value, BigDecimal avg, BigDecimal result) {
        BigDecimal tmp = value.subtract(avg).pow(2, DECIMAL128);
        return result == null ? tmp : result.add(tmp);
    }
}
