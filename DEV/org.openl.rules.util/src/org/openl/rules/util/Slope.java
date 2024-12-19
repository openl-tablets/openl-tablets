package org.openl.rules.util;

import static org.openl.rules.util.Covar.sampleCovariance;
import static org.openl.rules.util.InputStatistics.loadInputStats;
import static org.openl.rules.util.Var.varS;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

public final class Slope {

    private Slope() {
        //Utility Class
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

    static Double slope(InputStatistics.InputStatsDouble inputStats) {
        if (inputStats == null) {
            return null;
        }
        Double sampleCovariance = sampleCovariance(inputStats);
        Double sampleVariance = varS(inputStats.x);
        return sampleCovariance == null || sampleVariance == null ? null : sampleCovariance / sampleVariance;

    }

    static BigDecimal slope(InputStatistics.InputStatsBigDecimal inputStats) {
        if (inputStats == null) {
            return null;
        }
        BigDecimal sampleCovariance = sampleCovariance(inputStats);
        BigDecimal sampleVariance = varS(inputStats.x);
        return sampleCovariance == null || sampleVariance == null || BigDecimal.ZERO.equals(sampleCovariance) ?
                null :
                sampleCovariance.divide(sampleVariance, MathContext.DECIMAL128);
    }
}
