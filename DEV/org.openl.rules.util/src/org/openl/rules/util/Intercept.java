package org.openl.rules.util;

import static org.openl.rules.util.InputStatistics.loadInputStats;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.openl.rules.util.InputStatistics.InputStatsDouble;

public final class Intercept {

    private Intercept() {
        //Utility Class
    }

    public static <X extends Number, Y extends Number> Double intercept(Y[] y, X[] x) {
        InputStatsDouble inputStats = loadInputStats(y, x);
        if (inputStats == null || inputStats.y.length <= 1) {
            return null;
        }
        Double avgY = inputStats.getAvgY();
        Double avgX = inputStats.getAvgX();
        Double slopeB = Slope.slope(inputStats);
        return avgY - (slopeB * avgX);
    }

    public static BigDecimal intercept(BigDecimal[] y, BigDecimal[] x) {
        InputStatistics.InputStatsBigDecimal inputStats = loadInputStats(y, x);
        return intercept(inputStats);
    }

    public static BigDecimal intercept(BigInteger[] y, BigInteger[] x) {
        InputStatistics.InputStatsBigDecimal inputStats = loadInputStats(y, x);
        return intercept(inputStats);
    }

    private static BigDecimal intercept(InputStatistics.InputStatsBigDecimal inputStats) {
        if (inputStats == null || inputStats.y.length <= 1) {
            return null;
        }
        BigDecimal avgY = inputStats.getAvgY();
        BigDecimal avgX = inputStats.getAvgX();
        BigDecimal slopeB = Slope.slope(inputStats);
        return avgX == null || slopeB == null ? avgY : avgY.subtract(slopeB.multiply(avgX));
    }
}
