package org.openl.rules.util;

import static org.openl.rules.util.InputStatistics.loadInputStats;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.openl.rules.util.InputStatistics.InputStatsBigDecimal;
import org.openl.rules.util.InputStatistics.InputStatsDouble;

/**
 * Utility class for calculating forecasted values using linear regression.
 * Provides methods to predict Y values for a given X value based on known X-Y data points.
 *
 * <p>The forecast is calculated using the linear regression equation: Y = a + bX, where:
 * <ul>
 *   <li>a is the Y-intercept</li>
 *   <li>b is the slope of the regression line</li>
 *   <li>X is the input value to forecast</li>
 * </ul></p>
 */
public final class Forecast {

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private Forecast() {
    }

    /**
     * Calculates a forecasted Y value for a given X value using linear regression.
     *
     * @param x      the X value to forecast for(extending Number)
     * @param knownY the array of known Y values used for regression(extending Number)
     * @param knownX the array of known X values used for regression(extending Number)
     * @return the forecasted value as a Double, or null
     */
    public static <X extends Number, Y extends Number> Double forecast(X x, Y[] knownY, X[] knownX) {
        InputStatsDouble inputStats = loadInputStats(knownY, knownX);
        if (inputStats == null || x == null) {
            return null;
        }
        Double slopeB = Slope.slope(inputStats);
        var a = slopeB != null ? inputStats.getAvgY() - (slopeB * inputStats.getAvgX()) : null;
        return slopeB == null ? null : a + (slopeB * x.doubleValue());
    }

    /**
     * Calculates a forecasted Y value for a given BigDecimal X value using linear regression.
     *
     * @param x      the X value to forecast for
     * @param knownY the array of known Y values used for regression
     * @param knownX the array of known X values used for regression
     * @return the forecasted value as a BigDecimal, or null
     */
    public static BigDecimal forecast(BigDecimal x, BigDecimal[] knownY, BigDecimal[] knownX) {
        if (x == null) {
            return null;
        }
        InputStatsBigDecimal inputStats = loadInputStats(knownY, knownX);
        return forecastBigNumber(inputStats, x);
    }

    /**
     * Calculates a forecasted Y value for a given BigInteger X value using linear regression.
     *
     * @param x      the X value to forecast for
     * @param knownY the array of known Y values used for regression
     * @param knownX the array of known X values used for regression
     * @return the forecasted value as a BigDecimal, or null
     */
    public static BigDecimal forecast(BigInteger x, BigInteger[] knownY, BigInteger[] knownX) {
        if (x == null) {
            return null;
        }
        InputStatsBigDecimal inputStats = loadInputStats(knownY, knownX);
        return forecastBigNumber(inputStats, new BigDecimal(x));
    }

    private static BigDecimal forecastBigNumber(InputStatsBigDecimal inputStats, BigDecimal x) {
        if (inputStats == null) {
            return null;
        }
        BigDecimal slopeB = Slope.slope(inputStats);
        return slopeB != null ? inputStats.getAvgY().subtract(slopeB.multiply(inputStats.getAvgX())).add(slopeB.multiply(x)) : null;
    }
}
