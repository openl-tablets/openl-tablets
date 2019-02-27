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
    public static Double product(Double... values) {
        return process(values, new Result<Double, Double>() {
            @Override
            public void processNonNull(Double value) {
                result = result == null ? value : (result * value);
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
                result = result == null ? value : (result * value);
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
                result = result == null ? value : result.multiply(value);
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
                result = result == null ? value : result.multiply(value);
            }
        });
    }
}
