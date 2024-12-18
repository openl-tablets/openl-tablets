package org.openl.rules.util;

/**
 * A set of function for statistical analyze.
 */
public final class Statistics {

    private Statistics() {
        // Utility class
    }

    /**
     * Returns the greatest of values. If values are equal, the first instance will return.
     */
    public static <T extends Comparable<T>> T max(T... values) {
        return process(values, new Result<T, T>() {
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
        return process(values, new Result<T, T>() {
            @Override
            public void processNonNull(T value) {
                if (result == null || result.compareTo(value) > 0) {
                    result = value;
                }
            }
        });
    }

    public static <V, R> R process(V[] values, Processor<V, R> processor) {
        if (values == null || values.length == 0) {
            return null;
        }
        for (V value : values) {
            processor.process(value);
        }
        return processor.result();
    }

    static <V, R> R biProcess(V[] y, V[] x, Processor<V, R> processor) {
        if (x == null || x.length == 0
                || y == null || y.length == 0) {
            return null;
        }
        for (int i = 0; i < y.length; i++) {
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
