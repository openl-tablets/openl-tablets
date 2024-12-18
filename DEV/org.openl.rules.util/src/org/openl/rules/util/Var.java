package org.openl.rules.util;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.openl.rules.util.Statistics.Result;

import static java.math.MathContext.DECIMAL128;
import static org.openl.rules.util.Avg.avg;
import static org.openl.rules.util.Statistics.process;

public final class Var {

    private Var() {
        //Utility Class
    }

    public static <T extends Number> Double varS(T... values) {
        Double avg = avg(values);
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

    public static BigDecimal varS(BigInteger... values) {
        BigDecimal avg = avg(values);
        return avg == null || values.length == 1 ? null : process(values, new Result<>() {
            @Override
            public void processNonNull(BigInteger value) {
                result = variance(new BigDecimal(value), avg, result);
            }

            @Override
            public BigDecimal result() {
                return result == null ? null : result.divide(BigDecimal.valueOf(counter - 1L), DECIMAL128);
            }
        });
    }

    public static BigDecimal varS(BigDecimal... values) {
        BigDecimal avg = avg(values);
        return avg == null || values.length == 1 ? null : process(values, new Result<>() {
            @Override
            public void processNonNull(BigDecimal value) {
                result = variance(value, avg, result);
            }

            @Override
            public BigDecimal result() {
                return result == null ? null : result.divide(BigDecimal.valueOf(counter - 1L), DECIMAL128);
            }
        });
    }

    public static <T extends Number> Double varP(T... values) {
        Double avg = avg(values);
        if (avg == null) {
            return null;
        }
        if (values.length == 1) {
            return 0.0;
        }
        return process(values, new Result<>() {
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
        BigDecimal avg = avg(values);
        if (avg == null) {
            return null;
        }
        if (values.length == 1) {
            return BigDecimal.ZERO;
        }
        return process(values, new Result<>() {
            @Override
            public void processNonNull(BigDecimal value) {
                result = variance(value, avg, result);
            }

            @Override
            public BigDecimal result() {
                return result == null ? null : result.divide(BigDecimal.valueOf(counter), DECIMAL128);
            }
        });
    }

    public static BigDecimal varP(BigInteger... values) {
        BigDecimal avg = avg(values);
        if (avg == null) {
            return null;
        }
        if (values.length == 1) {
            return BigDecimal.ZERO;
        }
        return process(values, new Result<>() {
            @Override
            public void processNonNull(BigInteger value) {
                result = variance(new BigDecimal(value), avg, result);
            }

            @Override
            public BigDecimal result() {
                return result == null ? null : result.divide(BigDecimal.valueOf(counter), DECIMAL128);
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
}
