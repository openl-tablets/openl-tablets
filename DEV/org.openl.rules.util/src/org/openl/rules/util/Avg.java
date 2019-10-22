package org.openl.rules.util;

import static org.openl.rules.util.Statistics.process;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

import org.openl.rules.util.Statistics.Result;

/**
 * Avg functions for different types of numbers.
 */
public final class Avg {

    private Avg() {
        // Utility class
    }

    /**
     * Returns the average of values.
     */
    public static <T extends Number> Double avg(T... values) {
        return process(values, new Result<T, Double>() {
            @Override
            public void processNonNull(T value) {
                double doubleValue = value.doubleValue();
                result = result == null ? doubleValue : result + doubleValue;
            }

            @Override
            public Double result() {
                return result == null ? null : result / counter;
            }
        });
    }

    /**
     * Returns the average of values.
     */
    public static Float avg(Float... values) {
        return process(values, new Result<Float, Float>() {
            @Override
            public void processNonNull(Float value) {
                result = result == null ? value : result + value;
            }

            @Override
            public Float result() {
                return result == null ? null : result / counter;
            }
        });
    }

    /**
     * Returns the average of values.
     */
    public static BigDecimal avg(BigDecimal... values) {
        return process(values, new Result<BigDecimal, BigDecimal>() {
            @Override
            public void processNonNull(BigDecimal value) {
                result = result == null ? value : result.add(value);
            }

            @Override
            public BigDecimal result() {
                return devide(result, counter);
            }
        });
    }

    /**
     * Returns the average of values.
     */
    public static BigDecimal avg(BigInteger... values) {
        return process(values, new Result<BigInteger, BigDecimal>() {
            @Override
            public void processNonNull(BigInteger value) {
                BigDecimal bigDecimal = new BigDecimal(value);
                result = result == null ? bigDecimal : result.add(bigDecimal);
            }

            @Override
            public BigDecimal result() {
                return devide(result, counter);
            }
        });
    }

    private static BigDecimal devide(BigDecimal a, int b) {
        return a == null ? null : a.divide(BigDecimal.valueOf(b), MathContext.DECIMAL128);
    }
}
