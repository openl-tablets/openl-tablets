package org.openl.rules.util;

import static org.openl.rules.util.Statistics.process;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.openl.rules.util.Statistics.Result;

/**
 * Sum functions for different types of numbers.
 */
public final class Sum {

    private Sum() {
        // Utility class
    }

    /**
     * Returns the sum of values.
     */
    public static <T extends Number> Double sum(T... values) {
        return process(values, new Result<T, Double>() {
            @Override
            public void processNonNull(T value) {
                result = result == null ? value.doubleValue() : (result + value.doubleValue());
            }
        });
    }

    /**
     * Returns the sum of values.
     */
    public static Double sum(Double... values) {
        return process(values, new Result<Double, Double>() {
            @Override
            public void processNonNull(Double value) {
                result = result == null ? value : (result + value);
            }
        });
    }

    /**
     * Returns the sum of values.
     */
    public static Float sum(Float... values) {
        return process(values, new Result<Float, Float>() {
            @Override
            public void processNonNull(Float value) {
                result = result == null ? value : (result + value);
            }
        });
    }

    /**
     * Returns the sum of values.
     */
    public static Long sum(Long... values) {
        return process(values, new Result<Long, Long>() {
            @Override
            public void processNonNull(Long value) {
                result = result == null ? value : (result + value);
            }
        });
    }

    /**
     * Returns the sum of values.
     */
    public static Integer sum(Integer... values) {
        return process(values, new Result<Integer, Integer>() {
            @Override
            public void processNonNull(Integer value) {
                result = result == null ? value : (result + value);
            }
        });
    }

    /**
     * Returns the sum of values.
     */
    public static Integer sum(Short... values) {
        return process(values, new Result<Short, Integer>() {
            @Override
            public void processNonNull(Short value) {
                result = result == null ? value : (result + value);
            }
        });
    }

    /**
     * Returns the sum of values.
     */
    public static Integer sum(Byte... values) {
        return process(values, new Result<Byte, Integer>() {
            @Override
            public void processNonNull(Byte value) {
                result = result == null ? value : (result + value);
            }
        });
    }

    /**
     * Returns the sum of values.
     */
    public static BigDecimal sum(BigDecimal... values) {
        return process(values, new Result<BigDecimal, BigDecimal>() {
            @Override
            public void processNonNull(BigDecimal value) {
                result = result == null ? value : result.add(value);
            }
        });
    }

    /**
     * Returns the sum of values.
     */
    public static BigInteger sum(BigInteger... values) {
        return process(values, new Result<BigInteger, BigInteger>() {
            @Override
            public void processNonNull(BigInteger value) {
                result = result == null ? value : result.add(value);
            }
        });
    }
}
