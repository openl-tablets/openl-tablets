package org.openl.rules.util;

import static org.openl.rules.util.Statistics.process;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.openl.rules.util.Statistics.Result;

/**
 * Product functions for different types of numbers.
 */
public final class Product {

    private Product() {
        // Utility class
    }

    /**
     * Returns the product of values.
     */
    public static <T extends Number> Double product(T... values) {
        return process(values, new Result<>() {
            @Override
            public void processNonNull(T value) {
                current = current == null ? value.doubleValue() : current * value.doubleValue();
            }
        });
    }

    /**
     * Returns the product of values.
     */
    public static Double product(Double... values) {
        return process(values, new Result<Double, Double>() {
            @Override
            public void processNonNull(Double value) {
                current = current == null ? value : current * value;
            }
        });
    }

    /**
     * Returns the product of values.
     */
    public static Long product(Long... values) {
        return process(values, new Result<Long, Long>() {
            @Override
            public void processNonNull(Long value) {
                current = current == null ? value : current * value;
            }
        });
    }

    /**
     * Returns the product of values.
     */
    public static BigDecimal product(BigDecimal... values) {
        return process(values, new Result<BigDecimal, BigDecimal>() {
            @Override
            public void processNonNull(BigDecimal value) {
                current = current == null ? value : current.multiply(value);
            }
        });
    }

    /**
     * Returns the product of values.
     */
    public static BigInteger product(BigInteger... values) {
        return process(values, new Result<BigInteger, BigInteger>() {
            @Override
            public void processNonNull(BigInteger value) {
                current = current == null ? value : current.multiply(value);
            }
        });
    }
}
