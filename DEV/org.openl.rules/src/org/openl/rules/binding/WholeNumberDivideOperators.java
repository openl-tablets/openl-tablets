package org.openl.rules.binding;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

import org.openl.conf.OperatorsNamespace;

@Deprecated
@OperatorsNamespace
public class WholeNumberDivideOperators {
    private static final String DIVISION_BY_ZERO = "Division by zero";

    public static byte divide(byte x, byte y) {
        return (byte) (x / y);
    }

    public static short divide(short x, short y) {
        return (short) (x / y);
    }

    public static int divide(int x, int y) {
        return x / y;
    }

    public static long divide(long x, long y) {
        return x / y;
    }

    public static Byte divide(Byte x, Byte y) {
        if (y == null) {
            return x;
        } else if (x == null) {
            x = 1;
        }
        return (byte) (x / y);
    }

    public static Short divide(Short x, Short y) {
        if (y == null) {
            return x;
        } else if (x == null) {
            x = 1;
        }
        return (short) (x / y);
    }

    public static Integer divide(Integer x, Integer y) {
        if (y == null) {
            return x;
        } else if (x == null) {
            x = 1;
        }
        return x / y;
    }

    public static Long divide(Long x, Long y) {
        if (y == null) {
            return x;
        } else if (x == null) {
            x = 1L;
        }
        return x / y;
    }

    public static BigInteger divide(BigInteger x, BigInteger y) {
        if (y == null) {
            return x;
        } else if (x == null) {
            x = BigInteger.ONE;
        }
        return x.divide(y);
    }

    public static Float divide(Float x, Float y) {
        if (y == null) {
            return x;
        } else if (x == null) {
            x = 1.0f;
        }
        return x / y;
    }

    public static Double divide(Double x, Double y) {
        if (y == null) {
            return x;
        } else if (x == null) {
            x = 1.0;
        }
        return x / y;
    }

    public static BigDecimal divide(BigDecimal x, BigDecimal y) {
        if (y == null) {
            return x;
        } else if (x == null) {
            x = BigDecimal.ONE;
        }
        return x.divide(y, MathContext.DECIMAL128);
    }
}
