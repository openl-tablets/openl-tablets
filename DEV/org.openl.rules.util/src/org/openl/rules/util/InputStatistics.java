package org.openl.rules.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.function.Function;
import java.util.function.IntFunction;

final class InputStatistics {

    private InputStatistics() {
        //Utility Class
    }

    static <X extends Number, Y extends Number> InputStatistics.InputStatsDouble loadInputStats(Y[] knownY,
                                                                                                X[] knownX) {
        return (InputStatistics.InputStatsDouble) validateAndGetInputStats(knownY,
                knownX,
                Number::doubleValue,
                InputStatistics::getInputStatsDouble);
    }

    static InputStatistics.InputStatsBigDecimal loadInputStats(BigDecimal[] knownY, BigDecimal[] knownX) {
        return (InputStatistics.InputStatsBigDecimal) validateAndGetInputStats(knownY,
                knownX,
                Function.identity(),
                InputStatistics::getInputStatsBigDecimal);
    }

    static InputStatistics.InputStatsBigDecimal loadInputStats(BigInteger[] knownY, BigInteger[] knownX) {
        return (InputStatistics.InputStatsBigDecimal) validateAndGetInputStats(knownY,
                knownX,
                BigDecimal::new,
                InputStatistics::getInputStatsBigDecimal);
    }

    static <T extends Number> InputStatistics.InputStats validateAndGetInputStats(T[] knownY,
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

    private static InputStatistics.InputStatsBigDecimal getInputStatsBigDecimal(Integer len) {
        return new InputStatistics.InputStatsBigDecimal(len);
    }

    private static InputStatistics.InputStatsDouble getInputStatsDouble(Integer len) {
        return new InputStatistics.InputStatsDouble(len);
    }

    abstract static class InputStats<T extends Number> {
        T[] x;
        T[] y;

        abstract T getAvgX();

        abstract T getAvgY();
    }

    static class InputStatsDouble extends InputStats<Double> {
        Double avgX;
        Double avgY;

        InputStatsDouble(int len) {
            x = new Double[len];
            y = new Double[len];
        }

        Double getAvgX() {
            if (avgX == null) {
                avgX = Avg.avg(x);
            }
            return avgX;
        }

        Double getAvgY() {
            if (avgY == null) {
                avgY = Avg.avg(y);
            }
            return avgY;
        }
    }

    static class InputStatsBigDecimal extends InputStats<BigDecimal> {
        BigDecimal avgX;
        BigDecimal avgY;

        InputStatsBigDecimal(int len) {
            x = new BigDecimal[len];
            y = new BigDecimal[len];
        }

        BigDecimal getAvgX() {
            if (avgX == null) {
                avgX = Avg.avg(x);
            }
            return avgX;
        }

        BigDecimal getAvgY() {
            if (avgY == null) {
                avgY = Avg.avg(y);
            }
            return avgY;
        }
    }
}