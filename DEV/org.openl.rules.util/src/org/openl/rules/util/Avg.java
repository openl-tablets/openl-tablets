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
                var doubleValue = value.doubleValue();
                current = current == null ? doubleValue : (current + doubleValue);
            }

            @Override
            public Double result() {
                return current == null ? null : (current / counter);
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
                current = current == null ? value : (current + value);
            }

            @Override
            public Float result() {
                return current == null ? null : (current / counter);
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
                current = current == null ? value : current.add(value);
            }

            @Override
            public BigDecimal result() {
                return devide(current, counter);
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
                var bigDecimal = new BigDecimal(value);
                current = current == null ? bigDecimal : current.add(bigDecimal);
            }

            @Override
            public BigDecimal result() {
                return devide(current, counter);
            }
        });
    }

    private static BigDecimal devide(BigDecimal a, int b) {
        return a == null ? null : a.divide(BigDecimal.valueOf(b), MathContext.DECIMAL128);
    }
}
