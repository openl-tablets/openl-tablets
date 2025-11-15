package org.openl.rules.util;

import static java.math.MathContext.DECIMAL128;

import static org.openl.rules.util.Var.varP;
import static org.openl.rules.util.Var.varS;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Utility class for calculating standard deviation of numeric arrays.
 *
 * <p>Standard deviation measures the amount of variation in a dataset. It is calculated as
 * the square root of variance. This class provides both population (stdevP) and sample (stdevS)
 * standard deviation calculations.</p>
 *
 * <p>The difference between population and sample standard deviation:
 * <ul>
 *   <li>Population (stdevP) - uses n as denominator, used when data represents entire population</li>
 *   <li>Sample (stdevS) - uses (n-1) as denominator, used when data represents a sample of population</li>
 * </ul></p>
 */
public final class StdDev {

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private StdDev() {
        //Utility Class
    }

    /**
     * Calculates the population standard deviation of an array of numbers.
     *
     * @param values the array of values(extending Number)
     * @return the population standard deviation as a Double, or null
     */
    public static <T extends Number> Double stdevP(T... values) {
        Double populationVariance = varP(values);
        return populationVariance != null ? Math.sqrt(populationVariance) : null;
    }

    /**
     * Calculates the population standard deviation of an array of BigDecimal numbers.
     *
     * @param values the array of BigDecimal values
     * @return the population standard deviation as a BigDecimal, or null
     */
    public static BigDecimal stdevP(BigDecimal... values) {
        BigDecimal populationVariance = varP(values);
        return populationVariance != null ? populationVariance.sqrt(DECIMAL128) : null;
    }

    /**
     * Calculates the population standard deviation of an array of BigInteger numbers.
     *
     * @param values the array of BigInteger values
     * @return the population standard deviation as a BigDecimal, or null
     */
    public static BigDecimal stdevP(BigInteger... values) {
        BigDecimal populationVariance = varP(values);
        return populationVariance != null ? populationVariance.sqrt(DECIMAL128) : null;
    }

    /**
     * Calculates the sample standard deviation of an array of numbers.
     *
     * @param values the array of values(extending Number)
     * @return the sample standard deviation as a Double, or null
     */
    public static <T extends Number> Double stdevS(T... values) {
        Double sampleVariance = varS(values);
        return sampleVariance == null ? null : Math.sqrt(sampleVariance);
    }

    /**
     * Calculates the sample standard deviation of an array of BigDecimal numbers.
     *
     * @param values the array of BigDecimal values
     * @return the sample standard deviation as a BigDecimal, or null
     */
    public static BigDecimal stdevS(BigDecimal... values) {
        BigDecimal sampleVariance = varS(values);
        return sampleVariance == null ? null : sampleVariance.sqrt(DECIMAL128);
    }

    /**
     * Calculates the sample standard deviation of an array of BigInteger numbers.
     *
     * @param values the array of BigInteger values
     * @return the sample standard deviation as a BigDecimal, or null
     */
    public static BigDecimal stdevS(BigInteger... values) {
        BigDecimal sampleVariance = varS(values);
        return sampleVariance == null ? null : sampleVariance.sqrt(DECIMAL128);
    }

    /**
     * Calculates the population standard deviation of an array of Float numbers.
     *
     * @param values the array of Float values
     * @return the population standard deviation as a Float, or null
     */
    public static Float stdevP(Float... values) {
        Float populationVariance = varP(values);
        return populationVariance != null ? (float) Math.sqrt(populationVariance) : null;
    }

    /**
     * Calculates the sample standard deviation of an array of Float numbers.
     *
     * @param values the array of Float values
     * @return the sample standard deviation as a Float, or null
     */
    public static Float stdevS(Float... values) {
        Float sampleVariance = varS(values);
        return sampleVariance == null ? null : (float) Math.sqrt(sampleVariance);
    }
}
