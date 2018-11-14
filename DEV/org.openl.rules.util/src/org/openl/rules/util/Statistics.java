package org.openl.rules.util;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * A set of function for statistical analyze.
 */
public class Statistics {

    /**
     * Returns the greatest of values. If values are equal, the first instance will return.
     */
    public static <T extends Comparable<T>> T max(T... values) {
        return process(values, new Simple<T>() {
            @Override
            public void processNonNull(T value) {
                if (result == null || result.compareTo(value) < 0) {
                    result = value;
                }
            }
        });
    }

    /**
     * Returns the smallest of values. If values are equal, the first instance will return.
     */
    public static <T extends Comparable<T>> T min(T... values) {
        return process(values, new Simple<T>() {
            @Override
            public void processNonNull(T value) {
                if (result == null || result.compareTo(value) > 0) {
                    result = value;
                }
            }
        });
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
        return process(values, new Simple<Double>() {
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
        return process(values, new Simple<Float>() {
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
        return process(values, new Simple<Long>() {
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
        return process(values, new Simple<Integer>() {
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
        return process(values, new Simple<BigDecimal>() {
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
        return process(values, new Simple<BigInteger>() {
            @Override
            public void processNonNull(BigInteger value) {
                result = result == null ? value : result.add(value);
            }
        });
    }

    private static <V, R> R process(V[] values, Processor<V, R> processor) {
        if (values == null || values.length == 0) {
            return null;
        }
        for (V value : values) {
            processor.process(value);
        }
        return processor.result();
    }

    private interface Processor<V, R> {
        void process(V value);

        R result();
    }

    private static abstract class Simple<T> extends Result<T, T> {
    }

    private static abstract class Result<V, R> implements Processor<V, R> {
        R result;
        int counter;

        void processNonNull(V value) {
        }

        @Override
        public void process(V value) {
            if (value != null) {
                processNonNull(value);
                counter++;
            }
        }

        @Override
        public R result() {
            return result;
        }
    }

}
