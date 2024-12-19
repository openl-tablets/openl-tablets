package org.openl.rules.util;

import static java.math.MathContext.DECIMAL128;

import static org.openl.rules.util.Var.varP;
import static org.openl.rules.util.Var.varS;

import java.math.BigDecimal;
import java.math.BigInteger;

public final class StdDev {

    private StdDev() {
        //Utility Class
    }

    public static <T extends Number> Double stdevP(T... values) {
        Double populationVariance = varP(values);
        return populationVariance != null ? Math.sqrt(populationVariance) : null;
    }

    public static BigDecimal stdevP(BigDecimal... values) {
        BigDecimal populationVariance = varP(values);
        return populationVariance != null ? populationVariance.sqrt(DECIMAL128) : null;
    }

    public static BigDecimal stdevP(BigInteger... values) {
        BigDecimal populationVariance = varP(values);
        return populationVariance != null ? populationVariance.sqrt(DECIMAL128) : null;
    }

    public static <T extends Number> Double stdevS(T... values) {
        Double sampleVariance = varS(values);
        return sampleVariance == null ? null : Math.sqrt(sampleVariance);
    }

    public static BigDecimal stdevS(BigDecimal... values) {
        BigDecimal sampleVariance = varS(values);
        return sampleVariance == null ? null : sampleVariance.sqrt(DECIMAL128);
    }

    public static BigDecimal stdevS(BigInteger... values) {
        BigDecimal sampleVariance = varS(values);
        return sampleVariance == null ? null : sampleVariance.sqrt(DECIMAL128);
    }
}
