package org.openl.rules.util;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.openl.rules.util.InputStatistics.InputStatsDouble;

import static java.math.MathContext.DECIMAL128;
import static org.openl.rules.util.Covar.sampleCovariance;
import static org.openl.rules.util.InputStatistics.loadInputStats;
import static org.openl.rules.util.StdDev.stdevS;

public class Corel {

    private Corel() {
        //Utility Class
    }

    public static <X extends Number, Y extends Number> Double correl(Y[] y, X[] x) {
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
        return correl(loadInputStats(y, x));
    }

    public static BigDecimal correl(BigInteger[] y, BigInteger[] x) {
        return correl(loadInputStats(y, x));
    }

    private static BigDecimal correl(InputStatistics.InputStatsBigDecimal inputStats) {
        if (inputStats == null) {
            return null;
        }
        BigDecimal covariance = sampleCovariance(inputStats);
        BigDecimal deviationX = stdevS(inputStats.x);
        BigDecimal deviationY = BigDecimal.ZERO.equals(deviationX) ? null : stdevS(inputStats.y);
        return covariance == null || deviationX == null || deviationY == null ?
                null :
                covariance.divide(deviationX.multiply(deviationY), DECIMAL128);
    }

    public static <X extends Number, Y extends Number> Double rsq(Y[] y, X[] x) {
        Double result = correl(y, x);
        return result == null ? null : Math.pow(result, 2);
    }

    public static BigDecimal rsq(BigDecimal[] y, BigDecimal[] x) {
        BigDecimal result = correl(y, x);
        return result == null ? null : result.pow(2, DECIMAL128);
    }

    public static BigDecimal rsq(BigInteger[] y, BigInteger[] x) {
        BigDecimal result = correl(y, x);
        return result == null ? null : result.pow(2, DECIMAL128);
    }
}
