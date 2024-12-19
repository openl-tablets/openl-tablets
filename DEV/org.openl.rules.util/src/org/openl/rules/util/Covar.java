package org.openl.rules.util;

import static org.openl.rules.util.InputStatistics.loadInputStats;
import static org.openl.rules.util.Statistics.biProcess;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

import org.openl.rules.util.InputStatistics.InputStatsBigDecimal;
import org.openl.rules.util.InputStatistics.InputStatsDouble;
import org.openl.rules.util.Statistics.Result;

public final class Covar {

    private Covar() {
        //Utility Class
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

    static Double sampleCovariance(InputStatsDouble inputStats) {
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

    static BigDecimal sampleCovariance(InputStatsBigDecimal inputStats) {
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

    public static <X extends Number, Y extends Number> Double covarP(Y[] y, X[] x) {
        //covarP
        InputStatsDouble inputStats = loadInputStats(y, x);
        if (inputStats == null) {
            return null;
        }
        Double avgX = inputStats.getAvgX();
        Double avgY = inputStats.getAvgY();
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

    private static BigDecimal covarP(InputStatsBigDecimal inputStats) {
        if (inputStats == null) {
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
                return result == null ? null : result.divide(BigDecimal.valueOf(counter), MathContext.DECIMAL128);
            }
        });
    }

}
