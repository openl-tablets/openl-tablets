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
        if(avg !=null && values.length == 1) {
            return null;
        }
        return process(values, new Result<>() {
            @Override
            public void processNonNull(T value) {
                result = variance(value.doubleValue(), avg, result);
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
        if(avg !=null && values.length == 1) {
            return null;
        }
        return process(values, new Result<>() {
            @Override
            public void processNonNull(T value) {
                result = variance(value.doubleValue(), avg, result);
            }

            @Override
            public Double result() {
                return result == null ? null : result / (counter-1);
            }
        });
    }

    private static Double variance(Double value, Double avg,  Double result) {
        return result == null ? Math.pow((value-avg), 2) : (result + Math.pow((value-avg), 2));
    }


    public static <T extends Number> Double sampleCovariance(T[] x, T[] y) {
        Double avgX = Avg.avg(x);
        Double avgY = Avg.avg(y);

        if((avgX!=null && x.length ==1) || (avgY != null && y.length == 1)) {
            return null;
        }
        return biProcess(x,y, new Result<>() {
            @Override
            public void processNonNull(T x, T y) {
                result = covariance(x.doubleValue(), y.doubleValue(), avgX, avgY, result);
            }

            @Override
            public Double result() {
                return result == null ? null : result / (counter - 1);
            }
        });
    }

    public static <T extends Number> Double populationCovariance(T[] x, T[] y) {
        Double avgX = Avg.avg(x);
        Double avgY = Avg.avg(y);
        return biProcess(x, y, new Result<>() {
            @Override
            public void processNonNull(T x, T y) {
                result = covariance(x.doubleValue(), y.doubleValue(), avgX, avgY, result);
            }
            @Override
            public Double result() {
                return result == null ? null : result / counter;
            }
        });
    }

    private static Double covariance(Double doubleX, Double doubleY, Double avgX, Double avgY, Double result){
        return result == null ? (doubleX-avgX)*(doubleY-avgY) : result + (doubleX-avgX)*(doubleY-avgY);
    }

    public static <T extends Number> Double correlationCoefficient(T[] x, T[] y) {
        Double covariance = sampleCovariance(x, y);
        if (covariance != null){
            Double deviationX = samplePopulationDeviation(x);
            Double deviationY = samplePopulationDeviation(y);
            return deviationX == null || deviationY == null ? null : covariance/(deviationX*deviationY);
        }
        return null;
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

    static <V, R> R biProcess(V[] x, V[] y, Processor<V, R> processor) {
        if (x == null || x.length == 0
            || y == null || y.length == 0
            || x.length != y.length) {
            return null;
        }
        for(int i=0;i< x.length;i++){
            processor.process(x[i], y[i]);
        }
        return processor.result();
    }

    interface Processor<V, R> {
        void process(V value);

        void process(V x, V y);

        R result();
    }

    abstract static class Result<V, R> implements Processor<V, R> {
        R result;
        int counter;

        void processNonNull(V value) {
        }

        void processNonNull(V x, V y) {
        }

        @Override
        public void process(V value) {
            if (value != null) {
                processNonNull(value);
                counter++;
            }
        }

        @Override
        public void process(V x, V y) {
            if (x != null && y != null) {
                processNonNull(x, y);
                counter++;
            }
        }

        @Override
        public R result() {
            return result;
        }
    }

}
