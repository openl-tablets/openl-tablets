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
        return populationVariance != null ? Math.sqrt(populationVariance) : null;
    }

    public static <T extends Number> Double populationVariance(T... values) {
        Double avg = Avg.avg(values);
        return avg == null ? null : values.length == 1 ? Double.valueOf(0) : process(values, new Result<>() {
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

    public static <T extends Number> Double sampleStandardDeviation(T... values) {
        Double sampleVariance = sampleVariance(values);
        return sampleVariance == null ? null : Math.sqrt(sampleVariance);
    }

    public static <T extends Number> Double sampleVariance(T... values) {
        Double avg = Avg.avg(values);
        return avg == null || values.length == 1 ? null : process(values, new Result<>() {
            @Override
            public void processNonNull(T value) {
                result = variance(value.doubleValue(), avg, result);
            }

            @Override
            public Double result() {
                return result == null ? null : result / (counter - 1);
            }
        });
    }

    private static Double variance(Double value, Double avg, Double result) {
        return result == null ? Math.pow((value - avg), 2) : (result + Math.pow((value - avg), 2));
    }

    public static <T extends Number> Double sampleCovariance(T[] y, T[] x) {
        return sampleCovariance(loadInputStats(y, x));
    }

    private static Double sampleCovariance(InputStats inputStats) {
        //covarS
        if (inputStats != null) {
            if (inputStats.x.length == 1) {
                return null;
            }

            Double avgX = inputStats.getAvgX();
            Double avgY = inputStats.getAvgY();

            return biProcess(inputStats.y, inputStats.x, new Result<>() {
                @Override
                public void processNonNull(Double y, Double x) {
                    result = covariance(y, x, avgY, avgX, result);
                }

                @Override
                public Double result() {
                    return result == null ? null : result / (counter - 1);
                }
            });
        }
        return null;
    }

    public static <T extends Number> Double populationCovariance(T[] y, T[] x) {
        //covarP
        InputStats inputStats = loadInputStats(y, x);

        if (inputStats != null) {
            Double avgX = Avg.avg(x);
            Double avgY = Avg.avg(y);
            return biProcess(inputStats.y, inputStats.x, new Result<>() {
                @Override
                public void processNonNull(Double y, Double x) {
                    result = covariance(y, x, avgY, avgX, result);
                }

                @Override
                public Double result() {
                    return result == null ? null : result / counter;
                }
            });
        }
        return null;
    }

    private static Double covariance(Double y, Double x, Double avgY, Double avgX, Double result) {
        return result == null ? (x - avgX) * (y - avgY) : result + (x - avgX) * (y - avgY);
    }

    public static <T extends Number> Double pearsonPopulationCorrelationCoefficient(T[] y, T[] x) {
        //correl
        InputStats inputStats = loadInputStats(y, x);

        if (inputStats != null) {
            Double covariance = sampleCovariance(inputStats);
            Double deviationX = sampleStandardDeviation(inputStats.x);
            Double deviationY = sampleStandardDeviation(inputStats.y);
            return covariance == null || deviationX == null || deviationY == null ? null : covariance / (deviationX * deviationY);
        }

        return null;
    }

    public static <T extends Number> Double RSQ(T[] y, T[] x) {
        Double result = pearsonPopulationCorrelationCoefficient(y, x);
        return result == null ? null : Math.pow(result, 2);
    }

    public static <T extends Number> Double slope(T[] y, T[] x) {
        return slope(loadInputStats(y, x));
    }

    private static Double slope(InputStats inputStats) {
        if (inputStats != null) {
            Double sampleCovariance = sampleCovariance(inputStats);
            Double sampleVariance = sampleVariance(inputStats.x);
            return sampleCovariance == null || sampleVariance == null ? null : sampleCovariance / sampleVariance;
        }
        return null;
    }

    public static <T extends Number> Double intercept(T[] y, T[] x) {
        InputStats inputStats = loadInputStats(y, x);
        if (inputStats != null && inputStats.y.length > 1) {
            Double avgY = inputStats.getAvgY();
            Double avgX = inputStats.getAvgX();
            Double slopeB = slope(inputStats);
            return avgX == null? avgY : avgY - (slopeB * avgX);
        }
        return null;
    }

    public static <T extends Number> Double forecast(T x, T[] knownY, T[] knownX) {
        InputStats inputStats = loadInputStats(knownY, knownX);
        if (inputStats != null) {
            Double slopeB = slope(inputStats);
            Double a = slopeB != null ? inputStats.getAvgY() - (slopeB * inputStats.getAvgX()) : null;
            return x == null || slopeB == null ? null : a + (slopeB * x.doubleValue());
        }
        return null;
    }

    private static <T extends Number> InputStats loadInputStats(T[] knownY, T[] knownX) {
        if (knownX == null || knownY == null) {
            return null;
        }
        int len = Math.min(knownX.length, knownY.length);
        InputStats inputStats = new InputStats(len);
        int j = 0;
        for (int i = 0; i < len; i++) {
            if (knownX[i] != null && knownY[i] != null) {
                inputStats.x[j] = knownX[i].doubleValue();
                inputStats.y[j++] = knownY[i].doubleValue();
            }
        }
        return j == 0 ? null : inputStats;
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

    static <V, R> R biProcess(Double[] y, Double[] x, Processor<V, R> processor) {
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

        void process(Double y, Double x);

        R result();
    }

    abstract static class Result<V, R> implements Processor<V, R> {
        R result;
        int counter;

        void processNonNull(V value) {
        }

        void processNonNull(Double y, Double x) {
        }

        @Override
        public void process(V value) {
            if (value != null) {
                processNonNull(value);
                counter++;
            }
        }

        @Override
        public void process(Double y, Double x) {
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

    private static class InputStats {
        Double[] x;
        Double[] y;
        Double avgX;
        Double avgY;

        InputStats(int len) {
            x = new Double[len];
            y = new Double[len];
        }

        public Double getAvgX() {
            if (avgX == null) {
                avgX = Avg.avg(x);
            }
            return avgX;
        }

        public Double getAvgY() {
            if (avgY == null) {
                avgY = Avg.avg(y);
            }
            return avgY;
        }
    }

}
