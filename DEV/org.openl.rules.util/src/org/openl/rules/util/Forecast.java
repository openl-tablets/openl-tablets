package org.openl.rules.util;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.openl.rules.util.InputStatistics.InputStatsBigDecimal;
import org.openl.rules.util.InputStatistics.InputStatsDouble;

import static org.openl.rules.util.InputStatistics.loadInputStats;

public final class Forecast {

    private Forecast() {
        //Utility Class
    }

    public static <X extends Number, Y extends Number> Double forecast(X x, Y[] knownY, X[] knownX) {
        InputStatsDouble inputStats = loadInputStats(knownY, knownX);
        if (inputStats == null || x == null) {
            return null;
        }
        Double slopeB = Slope.slope(inputStats);
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
        BigDecimal slopeB = Slope.slope(inputStats);
        return slopeB != null ?
                inputStats.getAvgY().subtract(slopeB.multiply(inputStats.getAvgX())).add(slopeB.multiply(x)) :
                null;
    }
}
