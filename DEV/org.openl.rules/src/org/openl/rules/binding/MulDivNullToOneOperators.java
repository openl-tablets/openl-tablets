package org.openl.rules.binding;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

import org.openl.conf.OperatorsNamespace;

@Deprecated
@OperatorsNamespace
public class MulDivNullToOneOperators {

    public static Byte multiply(Byte x, Byte y) {
        if (x == null) {
            return y;
        } else if (y == null) {
            return x;
        }
        return (byte) (x * y);
    }

    public static Short multiply(Short x, Short y) {
        if (x == null) {
            return y;
        } else if (y == null) {
            return x;
        }
        return (short) (x * y);
    }

    public static Integer multiply(Integer x, Integer y) {
        if (x == null) {
            return y;
        } else if (y == null) {
            return x;
        }
        return x * y;
    }

    public static Long multiply(Long x, Long y) {
        if (x == null) {
            return y;
        } else if (y == null) {
            return x;
        }
        return x * y;
    }

    public static Float multiply(Float x, Float y) {
        if (x == null) {
            return y;
        } else if (y == null) {
            return x;
        }
        return x * y;
    }

    public static Double multiply(Double x, Double y) {
        if (x == null) {
            return y;
        } else if (y == null) {
            return x;
        }
        return x * y;
    }

    public static BigInteger multiply(BigInteger x, BigInteger y) {
        if (x == null) {
            return y;
        } else if (y == null) {
            return x;
        }
        return x.multiply(y);
    }

    public static BigDecimal multiply(BigDecimal x, BigDecimal y) {
        if (x == null) {
            return y;
        } else if (y == null) {
            return x;
        }
        return x.multiply(y);
    }

    public static Double divide(Byte x, Byte y) {
        if (x == null && y == null) {
            return null;
        } else if (y == null) {
            return (double) x;
        } else if (x == null) {
            x = 1;
        }
        return (double) x / y;
    }

    public static Double divide(Short x, Short y) {
        if (x == null && y == null) {
            return null;
        } else if (y == null) {
            return (double) x;
        } else if (x == null) {
            x = 1;
        }
        return (double) x / y;
    }

    public static Double divide(Integer x, Integer y) {
        if (x == null && y == null) {
            return null;
        } else if (y == null) {
            return (double) x;
        } else if (x == null) {
            x = 1;
        }
        return (double) x / y;
    }

    public static Double divide(Long x, Long y) {
        if (x == null && y == null) {
            return null;
        } else if (y == null) {
            return (double) x;
        } else if (x == null) {
            x = 1L;
        }
        return (double) x / y;
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

    public static BigDecimal divide(BigInteger x, BigInteger y) {
        if (x == null && y == null) {
            return null;
        } else if (y == null) {
            return new BigDecimal(x);
        } else if (x == null) {
            x = BigInteger.ONE;
        }

        return new BigDecimal(x).divide(new BigDecimal(y), MathContext.DECIMAL128);
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
