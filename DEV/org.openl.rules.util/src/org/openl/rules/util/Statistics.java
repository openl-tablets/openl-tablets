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

    public static <T extends Number> Double standardPopulationDeviation(T... values) {
        Double populationVariance = populationVariance(values);
        return populationVariance == null ? null : Math.sqrt(populationVariance);
    }

    public static <T extends Number> Double populationVariance(T... values) {
        Double avg = Avg.avg(values);
        return process(values, new Result<>() {
            @Override
            public void processNonNull(T value) {
                double doubleValue = value.doubleValue();
                result = result == null ? Math.pow((doubleValue-avg), 2) : (result + Math.pow((doubleValue-avg), 2));
            }

            @Override
            public Double result() {
                return result == null ? null : result / counter;
            }
        });
    }

    public static <T extends Number> Double samplePopulationDeviation(T... values) {
        Double sampleVariance = sampleVariance(values);
        return sampleVariance == null ? null : Math.sqrt(sampleVariance);
    }

    public static <T extends Number> Double sampleVariance(T... values) {
        Double avg = Avg.avg(values);
        return process(values, new Result<>() {
            @Override
            public void processNonNull(T value) {
                double doubleValue = value.doubleValue();
                result = result == null ? Math.pow((doubleValue-avg), 2) : (result + Math.pow((doubleValue-avg), 2));
            }

            @Override
            public Double result() {
                return result == null ? null : result / (counter-1);
            }
        });
    }


    static <V, R> R process(V[] values, Processor<V, R> processor) {
        if (values == null || values.length == 0) {
            return null;
        }
        for (V value : values) {
            processor.process(value);
        }
        return processor.result();
    }

    interface Processor<V, R> {
        void process(V value);

        R result();
    }

    abstract static class Result<V, R> implements Processor<V, R> {
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
