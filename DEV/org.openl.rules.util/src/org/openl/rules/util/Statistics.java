package org.openl.rules.util;

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
            public void process(T value) {
                if (value != null) {
                    if (result == null || result.compareTo(value) < 0) {
                        result = value;
                    }
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
            public void process(T value) {
                if (value != null) {
                    if (result == null || result.compareTo(value) > 0) {
                        result = value;
                    }
                }
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

    private static abstract class Simple<T> implements Processor<T, T> {
        T result;

        @Override
        public T result() {
            return result;
        }
    }

}
