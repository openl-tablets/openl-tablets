package org.openl.rules.binding;

import java.math.BigInteger;

import org.openl.conf.OperatorsNamespace;
import org.openl.exception.OpenLRuntimeException;
import org.openl.meta.*;
import org.openl.meta.number.Formulas;

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
            x = 1l;
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

    public static ByteValue divide(ByteValue value1, ByteValue value2) {
        if (value1 == null && value2 == null) {
            return null;
        }

        if (value1 == null && value2.doubleValue() != 0) {
            return new ByteValue(value1, value2, divide(ByteValue.ONE, value2).getValue(), Formulas.DIVIDE);
        }

        if (value2 == null) {
            return new ByteValue(value1, value2, value1.getValue(), Formulas.DIVIDE);
        }

        if (value2.doubleValue() == 0) {
            throw new OpenLRuntimeException(DIVISION_BY_ZERO);
        }

        return new ByteValue(value1, value2, divide(value1.getValue(), value2.getValue()), Formulas.DIVIDE);
    }

    public static ShortValue divide(ShortValue value1, ShortValue value2) {
        if (value1 == null && value2 == null) {
            return null;
        }

        if (value1 == null && value2.doubleValue() != 0) {
            return new ShortValue(value1, value2, divide(ShortValue.ONE, value2).getValue(), Formulas.DIVIDE);
        }

        if (value2 == null) {
            return new ShortValue(value1, value2, value1.getValue(), Formulas.DIVIDE);
        }

        if (value2.doubleValue() == 0) {
            throw new OpenLRuntimeException(DIVISION_BY_ZERO);
        }

        return new ShortValue(value1, value2, divide(value1.getValue(), value2.getValue()), Formulas.DIVIDE);
    }

    public static IntValue divide(IntValue value1, IntValue value2) {
        if (value1 == null && value2 == null) {
            return null;
        }

        if (value1 == null && value2.doubleValue() != 0) {
            return new IntValue(value1, value2, divide(IntValue.ONE, value2).getValue(), Formulas.DIVIDE);
        }

        if (value2 == null) {
            return new IntValue(value1, value2, value1.getValue(), Formulas.DIVIDE);
        }

        if (value2.doubleValue() == 0) {
            throw new OpenLRuntimeException(DIVISION_BY_ZERO);
        }

        return new IntValue(value1, value2, divide(value1.getValue(), value2.getValue()), Formulas.DIVIDE);
    }

    public static LongValue divide(LongValue value1, LongValue value2) {
        if (value1 == null && value2 == null) {
            return null;
        }

        if (value1 == null && value2.doubleValue() != 0) {
            return new LongValue(value1, value2, divide(LongValue.ONE, value2).getValue(), Formulas.DIVIDE);
        }

        if (value2 == null) {
            return new LongValue(value1, value2, value1.getValue(), Formulas.DIVIDE);
        }

        if (value2.doubleValue() == 0) {
            throw new OpenLRuntimeException(DIVISION_BY_ZERO);
        }

        return new LongValue(value1, value2, divide(value1.getValue(), value2.getValue()), Formulas.DIVIDE);
    }

    public static BigIntegerValue divide(BigIntegerValue value1, BigIntegerValue value2) {
        if (value1 == null && value2 == null) {
            return null;
        }

        if (value1 == null && value2.doubleValue() != 0) {
            return new BigIntegerValue(value1, value2, divide(BigIntegerValue.ONE, value2).getValue(), Formulas.DIVIDE);
        }

        if (value2 == null) {
            return new BigIntegerValue(value1, value2, value1.getValue(), Formulas.DIVIDE);
        }

        if (value2.doubleValue() == 0) {
            throw new OpenLRuntimeException(DIVISION_BY_ZERO);
        }

        return new BigIntegerValue(value1, value2, divide(value1.getValue(), value2.getValue()), Formulas.DIVIDE);
    }

}
