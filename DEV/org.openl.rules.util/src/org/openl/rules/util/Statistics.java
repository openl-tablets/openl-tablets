package org.openl.rules.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.function.Function;
import java.util.function.IntFunction;

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

    public static <T extends Number> Double stdevP(T... values) {
        Double populationVariance = varP(values);
        return populationVariance != null ? Math.sqrt(populationVariance) : null;
    }

    public static BigDecimal stdevP(BigDecimal... values) {
        BigDecimal populationVariance = varP(values);
        return populationVariance != null ? populationVariance.sqrt(MathContext.DECIMAL128) : null;
    }

    public static BigDecimal stdevP(BigInteger... values) {
        BigDecimal populationVariance = varP(values);
        return populationVariance != null ? populationVariance.sqrt(MathContext.DECIMAL128) : null;
    }

    public static <T extends Number> Double varP(T... values) {
        Double avg = Avg.avg(values);
        if (avg == null) {
            return null;
        }
        if (values.length == 1) {
            return (double) 0;
        }
        return process(values, new Statistics.Result<>() {
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

    public static BigDecimal varP(BigDecimal... values) {
        BigDecimal avg = Avg.avg(values);
        if (avg == null) {
            return null;
        }
        if (values.length == 1) {
            return BigDecimal.valueOf(0);
        }
        return process(values, new Statistics.Result<>() {
            @Override
            public void processNonNull(BigDecimal value) {
                result = variance(value, avg, result);
            }

            @Override
            public BigDecimal result() {
                return result == null ? null : result.divide(BigDecimal.valueOf(counter), MathContext.DECIMAL128);
            }
        });
    }

    public static BigDecimal varP(BigInteger... values) {
        BigDecimal avg = Avg.avg(values);
        if (avg == null) {
            return null;
        }
        if (values.length == 1) {
            return BigDecimal.valueOf(0);
        }
        return process(values, new Statistics.Result<>() {
            @Override
            public void processNonNull(BigInteger value) {
                result = variance(new BigDecimal(value), avg, result);
            }

            @Override
            public BigDecimal result() {
                return result == null ? null : result.divide(BigDecimal.valueOf(counter), MathContext.DECIMAL128);
            }
        });
    }

    public static <T extends Number> Double stdevS(T... values) {
        Double sampleVariance = varS(values);
        return sampleVariance == null ? null : Math.sqrt(sampleVariance);
    }

    public static BigDecimal stdevS(BigDecimal... values) {
        BigDecimal sampleVariance = varS(values);
        return sampleVariance == null ? null : sampleVariance.sqrt(MathContext.DECIMAL128);
    }

    public static BigDecimal stdevS(BigInteger... values) {
        BigDecimal sampleVariance = varS(values);
        return sampleVariance == null ? null : sampleVariance.sqrt(MathContext.DECIMAL128);
    }

    public static <T extends Number> Double varS(T... values) {
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

    public static BigDecimal varS(BigDecimal... values) {
        BigDecimal avg = Avg.avg(values);
        return avg == null || values.length == 1 ? null : process(values, new Result<>() {
            @Override
            public void processNonNull(BigDecimal value) {
                result = variance(value, avg, result);
            }

            @Override
            public BigDecimal result() {
                return result == null ? null : result.divide(BigDecimal.valueOf(counter - 1L), MathContext.DECIMAL128);
            }
        });
    }

    public static BigDecimal varS(BigInteger... values) {
        BigDecimal avg = Avg.avg(values);
        return avg == null || values.length == 1 ? null : process(values, new Result<>() {
            @Override
            public void processNonNull(BigInteger value) {
                result = variance(new BigDecimal(value), avg, result);
            }

            @Override
            public BigDecimal result() {
                return result == null ? null : result.divide(BigDecimal.valueOf(counter - 1L), MathContext.DECIMAL128);
            }
        });
    }

    private static Double variance(Double value, Double avg, Double result) {
        Double tmp = Math.pow((value - avg), 2);
        return result == null ? tmp : result + tmp;
    }

    private static BigDecimal variance(BigDecimal value, BigDecimal avg, BigDecimal result) {
        BigDecimal tmp = value.subtract(avg).pow(2);
        return result == null ? tmp : result.add(tmp);
    }

    public static <X extends Number, Y extends Number> Double covarS(Y[] y, X[] x) {
        return sampleCovariance(loadInputStats(y, x));
    }

    public static BigDecimal covarS(BigDecimal[] y, BigDecimal[] x) {
        return sampleCovariance(loadInputStats(y, x));
    }

    public static BigDecimal covarS(BigInteger[] y, BigInteger[] x) {
        return sampleCovariance(loadInputStats(y, x));
    }

    private static Double sampleCovariance(InputStatsDouble inputStats) {
        //covarS
        if (inputStats == null || inputStats.x.length == 1) {
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

    private static BigDecimal sampleCovariance(InputStatsBigDecimal inputStats) {
        //covarS
        if (inputStats == null || inputStats.x.length == 1) {
            return null;
        }
        BigDecimal avgX = inputStats.getAvgX();
        BigDecimal avgY = inputStats.getAvgY();
        return biProcess(inputStats.y, inputStats.x, new Result<>() {
            @Override
            public void processNonNull(BigDecimal y, BigDecimal x) {
                result = covariance(y, x, avgY, avgX, result);
            }

            @Override
            public BigDecimal result() {
                return result == null ?
                        null :
                        result.divide(BigDecimal.valueOf(counter - 1L), MathContext.DECIMAL128);
            }
        });

    }

    public static <X extends Number, Y extends Number> Double covarP(Y[] y, X[] x) {
        //covarP
        InputStatsDouble inputStats = loadInputStats(y, x);
        if (inputStats == null) {
            return null;
        }
        Double avgX = Avg.avg(x);
        Double avgY = Avg.avg(y);
        return biProcess(inputStats.y, inputStats.x, new Result<Number, Double>() {
            @Override
            public void processNonNull(Number y, Number x) {
                result = covariance(y.doubleValue(), x.doubleValue(), avgY, avgX, result);
            }

            @Override
            public Double result() {
                return result == null ? null : result / counter;
            }
        });
    }

    public static BigDecimal covarP(BigDecimal[] y, BigDecimal[] x) {
        //covarP
        InputStatsBigDecimal inputStats = loadInputStats(y, x);
        return covarP(inputStats);
    }

    public static BigDecimal covarP(BigInteger[] y, BigInteger[] x) {
        //covarP
        InputStatsBigDecimal inputStats = loadInputStats(y, x);
        return covarP(inputStats);
    }

    private static BigDecimal covarP(InputStats<BigDecimal> inputStats) {
        if (inputStats == null) {
            return null;
        }
        BigDecimal avgX = Avg.avg(inputStats.x);
        BigDecimal avgY = Avg.avg(inputStats.y);
        return biProcess(inputStats.y, inputStats.x, new Result<>() {
            @Override
            public void processNonNull(BigDecimal y, BigDecimal x) {
                result = covariance(y, x, avgY, avgX, result);
            }

            @Override
            public BigDecimal result() {
                return result == null ? null : result.divide(BigDecimal.valueOf(counter), MathContext.DECIMAL128);
            }
        });
    }

    private static Double covariance(Double y, Double x, Double avgY, Double avgX, Double result) {
        var tmp = (x - avgX) * (y - avgY);
        return result == null ? tmp : result + tmp;
    }

    private static BigDecimal covariance(BigDecimal y,
            BigDecimal x,
            BigDecimal avgY,
            BigDecimal avgX,
            BigDecimal result) {
        var tmp = (x.subtract(avgX)).multiply(y.subtract(avgY));
        return result == null ? tmp : result.add(tmp);
    }

    public static <X extends Number, Y extends Number> Double correl(Y[] y, X[] x) {
        //correl
        InputStatsDouble inputStats = loadInputStats(y, x);
        if (inputStats == null) {
            return null;
        }
        Double covariance = sampleCovariance(inputStats);
        Double deviationX = stdevS(inputStats.x);
        Double deviationY = stdevS(inputStats.y);
        return covariance == null || deviationX == null || deviationY == null ?
                null :
                covariance / (deviationX * deviationY);
    }

    public static BigDecimal correl(BigDecimal[] y, BigDecimal[] x) {
        //correl
        return correl(loadInputStats(y, x));
    }

    public static BigDecimal correl(BigInteger[] y, BigInteger[] x) {
        //correl
        return correl(loadInputStats(y, x));
    }

    private static BigDecimal correl(InputStatsBigDecimal inputStats) {
        if (inputStats == null) {
            return null;
        }
        BigDecimal covariance = sampleCovariance(inputStats);
        BigDecimal deviationX = stdevS(inputStats.x);
        BigDecimal deviationY = stdevS(inputStats.y);
        return covariance == null || deviationX == null || deviationY == null ?
                null :
                covariance.divide(deviationX.multiply(deviationY), MathContext.DECIMAL128);
    }

    public static <X extends Number, Y extends Number> Double rsq(Y[] y, X[] x) {
        Double result = correl(y, x);
        return result == null ? null : Math.pow(result, 2);
    }

    public static BigDecimal rsq(BigDecimal[] y, BigDecimal[] x) {
        BigDecimal result = correl(y, x);
        return result == null ? null : result.pow(2, MathContext.DECIMAL128);
    }

    public static BigDecimal rsq(BigInteger[] y, BigInteger[] x) {
        BigDecimal result = correl(y, x);
        return result == null ? null : result.pow(2, MathContext.DECIMAL128);
    }

    public static <X extends Number, Y extends Number> Double slope(Y[] y, X[] x) {
        return slope(loadInputStats(y, x));
    }

    public static BigDecimal slope(BigDecimal[] y, BigDecimal[] x) {
        return slope(loadInputStats(y, x));
    }

    public static BigDecimal slope(BigInteger[] y, BigInteger[] x) {
        return slope(loadInputStats(y, x));
    }

    private static Double slope(InputStatsDouble inputStats) {
        if (inputStats == null) {
            return null;
        }
        Double sampleCovariance = sampleCovariance(inputStats);
        Double sampleVariance = varS(inputStats.x);
        return sampleCovariance == null || sampleVariance == null ? null : sampleCovariance / sampleVariance;

    }

    private static BigDecimal slope(InputStatsBigDecimal inputStats) {
        if (inputStats == null) {
            return null;
        }
        BigDecimal sampleCovariance = sampleCovariance(inputStats);
        BigDecimal sampleVariance = varS(inputStats.x);
        return sampleCovariance == null || sampleVariance == null ?
                null :
                sampleCovariance.divide(sampleVariance, MathContext.DECIMAL128);
    }

    public static <X extends Number, Y extends Number> Double intercept(Y[] y, X[] x) {
        InputStatsDouble inputStats = loadInputStats(y, x);
        if (inputStats == null || inputStats.y.length <= 1) {
            return null;
        }
        Double avgY = inputStats.getAvgY();
        Double avgX = inputStats.getAvgX();
        Double slopeB = slope(inputStats);
        return avgY - (slopeB * avgX);
    }

    public static BigDecimal intercept(BigDecimal[] y, BigDecimal[] x) {
        InputStatsBigDecimal inputStats = loadInputStats(y, x);
        return intercept(inputStats);
    }

    public static BigDecimal intercept(BigInteger[] y, BigInteger[] x) {
        InputStatsBigDecimal inputStats = loadInputStats(y, x);
        return intercept(inputStats);
    }

    private static BigDecimal intercept(InputStatsBigDecimal inputStats) {
        if (inputStats == null || inputStats.y.length <= 1) {
            return null;
        }
        BigDecimal avgY = inputStats.getAvgY();
        BigDecimal avgX = inputStats.getAvgX();
        BigDecimal slopeB = slope(inputStats);
        return avgX == null || slopeB == null ? avgY : avgY.subtract(slopeB.multiply(avgX));
    }

    public static <X extends Number, Y extends Number> Double forecast(X x, Y[] knownY, X[] knownX) {
        InputStatsDouble inputStats = loadInputStats(knownY, knownX);
        if (inputStats == null || x == null) {
            return null;
        }
        Double slopeB = slope(inputStats);
        var a = slopeB != null ? inputStats.getAvgY() - (slopeB * inputStats.getAvgX()) : null;
        return slopeB == null ? null : a + (slopeB * x.doubleValue());
    }

    public static BigDecimal forecast(BigDecimal x, BigDecimal[] knownY, BigDecimal[] knownX) {
        InputStatsBigDecimal inputStats = loadInputStats(knownY, knownX);
        return forecastBigNumber(inputStats, x);
    }

    public static BigDecimal forecast(BigInteger x, BigInteger[] knownY, BigInteger[] knownX) {
        InputStatsBigDecimal inputStats = loadInputStats(knownY, knownX);
        return forecastBigNumber(inputStats, new BigDecimal(x));
    }

    private static BigDecimal forecastBigNumber(InputStatsBigDecimal inputStats, BigDecimal x) {
        if (inputStats == null || x == null) {
            return null;
        }
        BigDecimal slopeB = slope(inputStats);
        return slopeB != null ?
                inputStats.getAvgY().subtract(slopeB.multiply(inputStats.getAvgX())).add(slopeB.multiply(x)) :
                null;
    }

    private static <X extends Number, Y extends Number> InputStatsDouble loadInputStats(Y[] knownY, X[] knownX) {
        return (InputStatsDouble) validateAndGetInputStats(knownY,
                knownX,
                Number::doubleValue,
                Statistics::getInputStatsDouble);
    }

    private static InputStatsBigDecimal loadInputStats(BigDecimal[] knownY, BigDecimal[] knownX) {
        return (InputStatsBigDecimal) validateAndGetInputStats(knownY,
                knownX,
                Function.identity(),
                Statistics::getInputStatsBigDecimal);
    }

    private static InputStatsBigDecimal loadInputStats(BigInteger[] knownY, BigInteger[] knownX) {
        return (InputStatsBigDecimal) validateAndGetInputStats(knownY,
                knownX,
                BigDecimal::new,
                Statistics::getInputStatsBigDecimal);
    }

    private static <T extends Number> InputStats validateAndGetInputStats(T[] knownY,
            T[] knownX,
            Function<T, Number> numberFunction,
            IntFunction<InputStats> inputStatsFunction) {
        if (knownX == null || knownY == null) {
            return null;
        }
        if (knownX.length != knownY.length) {
            throw new IndexOutOfBoundsException("The size of two arrays must be equals: x[" + knownX.length + "] and y[" + knownY.length + "]");
        }
        var len = knownY.length;
        var inputStats = inputStatsFunction.apply(len);
        var j = 0;
        for (int i = 0; i < len; i++) {
            if (knownX[i] != null && knownY[i] != null) {
                inputStats.x[j] = numberFunction.apply(knownX[i]);
                inputStats.y[j++] = numberFunction.apply(knownY[i]);
            }
        }
        return j == 0 ? null : inputStats;
    }

    private static InputStatsBigDecimal getInputStatsBigDecimal(Integer len) {
        return new InputStatsBigDecimal(len);
    }

    private static InputStatsDouble getInputStatsDouble(Integer len) {
        return new InputStatsDouble(len);
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

        void processNonNull(V value) {
        }

        void processNonNull(V y, V x) {
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

    private abstract static class InputStats<T extends Number> {
        T[] x;
        T[] y;

        public abstract T getAvgX();

        public abstract T getAvgY();
    }

    private static class InputStatsDouble extends InputStats<Double> {
        Double avgX;
        Double avgY;

        InputStatsDouble(int len) {
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

    private static class InputStatsBigDecimal extends InputStats<BigDecimal> {
        BigDecimal avgX;
        BigDecimal avgY;

        InputStatsBigDecimal(int len) {
            x = new BigDecimal[len];
            y = new BigDecimal[len];
        }

        public BigDecimal getAvgX() {
            if (avgX == null) {
                avgX = Avg.avg(x);
            }
            return avgX;
        }

        public BigDecimal getAvgY() {
            if (avgY == null) {
                avgY = Avg.avg(y);
            }
            return avgY;
        }
    }

}
