package org.openl.rules.util;

import java.util.function.BiPredicate;

import org.jspecify.annotations.Nullable;

/**
 * A set of function for statistical analyze.
 */
public final class Statistics {

    private Statistics() {
        // Utility class
    }

    /**
     * Returns the greatest of the values.
     * <p>
     * Empty values are skipped. Of equal values, the first one is returned. The result is empty when there are no
     * values or all of them are empty.
     * <p>
     * A NaN value makes the result NaN, as in {@link Math#max(double, double)}.
     */
    public static <T extends Comparable<T>> @Nullable T max(@Nullable T @Nullable ... values) {
        return pick(values, (value, best) -> value.compareTo(best) > 0);
    }

    /**
     * Returns the smallest of the values.
     * <p>
     * Empty values are skipped. Of equal values, the first one is returned. The result is empty when there are no
     * values or all of them are empty.
     * <p>
     * A NaN value makes the result NaN, as in {@link Math#min(double, double)}.
     */
    public static <T extends Comparable<T>> @Nullable T min(@Nullable T @Nullable ... values) {
        return pick(values, (value, best) -> value.compareTo(best) < 0);
    }

    /**
     * Returns the value that beats all the others. The first NaN value beats any number.
     */
    private static <T> @Nullable T pick(@Nullable T @Nullable [] values, BiPredicate<T, T> beats) {
        return process(values, new Result<T, T>() {
            @Override
            public void processNonNull(T value) {
                if (result == null || (!isNaN(result) && (isNaN(value) || beats.test(value, result)))) {
                    result = value;
                }
            }
        });
    }

    private static boolean isNaN(Object value) {
        return (value instanceof Double d && d.isNaN()) || (value instanceof Float f && f.isNaN());
    }

    public static <V, R> R process(V @Nullable [] values, Processor<V, R> processor) {
        if (values == null || values.length == 0) {
            return null;
        }
        for (V value : values) {
            processor.process(value);
        }
        return processor.result();
    }

    static <V, R> R biProcess(V[] y, V[] x, Processor<V, R> processor) {
        if (x == null || x.length == 0 || y == null || y.length == 0 || x.length != y.length) {
            return null;
        }
        for (var i = 0; i < y.length; i++) {
            processor.process(y[i], x[i]);
        }
        return processor.result();
    }

    interface Processor<V, R> {
        void process(V value);

        void process(V y, V x);

        R result();
    }

    abstract static class Result<V, R> implements Processor<V, R> {
        R result;
        int counter;

        public void processNonNull(V value) {
        }

        public void processNonNull(V y, V x) {
        }

        @Override
        public void process(V value) {
            if (value != null) {
                processNonNull(value);
                counter++;
            }
        }

        @Override
        public void process(V y, V x) {
            if (x != null && y != null) {
                processNonNull(y, x);
                counter++;
            }
        }

        @Override
        public R result() {
            return result;
        }
    }

}
