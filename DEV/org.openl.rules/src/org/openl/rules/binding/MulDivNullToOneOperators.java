package org.openl.rules.binding;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

import org.openl.binding.impl.Operators;
import org.openl.conf.OperatorsNamespace;
import org.openl.exception.OpenLRuntimeException;
import org.openl.meta.BigDecimalValue;
import org.openl.meta.DoubleValue;
import org.openl.meta.number.Formulas;

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
            return Double.valueOf(x);
        } else if (x == null) {
            x = 1;
        }
        return ((double) x / y);
    }

    public static Double divide(Short x, Short y) {
        if (x == null && y == null) {
            return null;
        } else if (y == null) {
            return Double.valueOf(x);
        } else if (x == null) {
            x = 1;
        }
        return ((double) x / y);
    }

    public static Double divide(Integer x, Integer y) {
        if (x == null && y == null) {
            return null;
        } else if (y == null) {
            return Double.valueOf(x);
        } else if (x == null) {
            x = 1;
        }
        return ((double) x / y);
    }

    public static Double divide(Long x, Long y) {
        if (x == null && y == null) {
            return null;
        } else if (y == null) {
            return Double.valueOf(x);
        } else if (x == null) {
            x = 1l;
        }
        return ((double) x / y);
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

    public static org.openl.meta.ByteValue multiply(org.openl.meta.ByteValue value1, org.openl.meta.ByteValue value2) {
        // temporary commented to support operations with nulls
        //
        // validate(value1, value2, Formulas.MULTIPLY.toString());
        if (value1 == null) {
            return value2;
        }

        if (value2 == null) {
            return value1;
        }

        return new org.openl.meta.ByteValue(value1,
            value2,
            Operators.multiply(value1.getValue(), value2.getValue()),
            Formulas.MULTIPLY);
    }

    public static org.openl.meta.DoubleValue divide(org.openl.meta.ByteValue value1, org.openl.meta.ByteValue value2) {
        // temporary commented to support operations with nulls
        //
        // validate(value1, value2, Formulas.DIVIDE.toString());
        if (value1 == null && value2 == null) {
            return null;
        }

        if (value1 == null && value2.doubleValue() != 0) {
            return new org.openl.meta.DoubleValue(null,
                new DoubleValue(value2.doubleValue()),
                divide((byte) 1, value2.getValue()),
                Formulas.DIVIDE);
        }

        if (value2 == null) {
            return new org.openl.meta.DoubleValue(new DoubleValue(value1.doubleValue()),
                null,
                value1.getValue(),
                Formulas.DIVIDE);
        }

        if (value2.doubleValue() == 0) {
            throw new OpenLRuntimeException("Division by zero");
        }

        return new org.openl.meta.DoubleValue(new DoubleValue(value1.doubleValue()),
            new DoubleValue(value2.doubleValue()),
            Operators.divide(value1.getValue(), value2.getValue()),
            Formulas.DIVIDE);
    }

    public static org.openl.meta.ShortValue multiply(org.openl.meta.ShortValue value1,
            org.openl.meta.ShortValue value2) {
        // temporary commented to support operations with nulls
        //
        // validate(value1, value2, Formulas.MULTIPLY.toString());
        if (value1 == null) {
            return value2;
        }

        if (value2 == null) {
            return value1;
        }

        return new org.openl.meta.ShortValue(value1,
            value2,
            Operators.multiply(value1.getValue(), value2.getValue()),
            Formulas.MULTIPLY);
    }

    public static org.openl.meta.DoubleValue divide(org.openl.meta.ShortValue value1,
            org.openl.meta.ShortValue value2) {
        // temporary commented to support operations with nulls
        //
        // validate(value1, value2, Formulas.DIVIDE.toString());
        if (value1 == null && value2 == null) {
            return null;
        }

        if (value1 == null) {
            if (value2 != null && value2.doubleValue() != 0) {
                return new org.openl.meta.DoubleValue(null,
                    new DoubleValue(value2.doubleValue()),
                    divide((short) 1, value2.getValue()),
                    Formulas.DIVIDE);
            }
        }

        if (value2 == null) {
            return new org.openl.meta.DoubleValue(new DoubleValue(value1.doubleValue()),
                null,
                value1.getValue(),
                Formulas.DIVIDE);
        }

        if (value2.doubleValue() == 0) {
            throw new OpenLRuntimeException("Division by zero");
        }

        return new org.openl.meta.DoubleValue(new DoubleValue(value1.doubleValue()),
            new DoubleValue(value2.doubleValue()),
            Operators.divide(value1.getValue(), value2.getValue()),
            Formulas.DIVIDE);
    }

    public static org.openl.meta.IntValue multiply(org.openl.meta.IntValue value1, org.openl.meta.IntValue value2) {
        // temporary commented to support operations with nulls
        //
        // validate(value1, value2, Formulas.MULTIPLY.toString());
        if (value1 == null) {
            return value2;
        }

        if (value2 == null) {
            return value1;
        }

        return new org.openl.meta.IntValue(value1,
            value2,
            Operators.multiply(value1.getValue(), value2.getValue()),
            Formulas.MULTIPLY);
    }

    public static org.openl.meta.DoubleValue divide(org.openl.meta.IntValue value1, org.openl.meta.IntValue value2) {
        // temporary commented to support operations with nulls
        //
        // validate(value1, value2, Formulas.DIVIDE.toString());
        if (value1 == null && value2 == null) {
            return null;
        }

        if (value1 == null) {
            if (value2 != null && value2.doubleValue() != 0) {
                return new org.openl.meta.DoubleValue(null,
                    new DoubleValue(value2.doubleValue()),
                    divide(1, value2.getValue()),
                    Formulas.DIVIDE);
            }
        }

        if (value2 == null) {
            return new org.openl.meta.DoubleValue(new DoubleValue(value1.doubleValue()),
                null,
                value1.getValue(),
                Formulas.DIVIDE);
        }

        if (value2.doubleValue() == 0) {
            throw new OpenLRuntimeException("Division by zero");
        }

        return new org.openl.meta.DoubleValue(new DoubleValue(value1.doubleValue()),
            new DoubleValue(value2.doubleValue()),
            Operators.divide(value1.getValue(), value2.getValue()),
            Formulas.DIVIDE);

    }

    public static org.openl.meta.LongValue multiply(org.openl.meta.LongValue value1, org.openl.meta.LongValue value2) {
        // temporary commented to support operations with nulls
        //
        // validate(value1, value2, Formulas.MULTIPLY.toString());
        if (value1 == null) {
            return value2;
        }

        if (value2 == null) {
            return value1;
        }

        return new org.openl.meta.LongValue(value1,
            value2,
            Operators.multiply(value1.getValue(), value2.getValue()),
            Formulas.MULTIPLY);
    }

    public static org.openl.meta.DoubleValue divide(org.openl.meta.LongValue value1, org.openl.meta.LongValue value2) {
        // temporary commented to support operations with nulls
        //
        // validate(value1, value2, Formulas.DIVIDE.toString());
        if (value1 == null && value2 == null) {
            return null;
        }

        if (value1 == null) {
            if (value2 != null && value2.doubleValue() != 0) {
                return new org.openl.meta.DoubleValue(null,
                    new DoubleValue(value2.doubleValue()),
                    divide(1l, value2.getValue()),
                    Formulas.DIVIDE);
            }
        }

        if (value2 == null) {
            return new org.openl.meta.DoubleValue(new DoubleValue(value1.doubleValue()),
                null,
                value1.getValue(),
                Formulas.DIVIDE);
        }

        if (value2.doubleValue() == 0) {
            throw new OpenLRuntimeException("Division by zero");
        }

        return new org.openl.meta.DoubleValue(new DoubleValue(value1.doubleValue()),
            new DoubleValue(value2.doubleValue()),
            Operators.divide(value1.getValue(), value2.getValue()),
            Formulas.DIVIDE);

    }

    public static org.openl.meta.FloatValue multiply(org.openl.meta.FloatValue value1,
            org.openl.meta.FloatValue value2) {
        // temporary commented to support operations with nulls
        //
        // validate(value1, value2, Formulas.MULTIPLY.toString());
        if (value1 == null) {
            return value2;
        }

        if (value2 == null) {
            return value1;
        }

        return new org.openl.meta.FloatValue(value1,
            value2,
            Operators.multiply(value1.getValue(), value2.getValue()),
            Formulas.MULTIPLY);
    }

    public static org.openl.meta.FloatValue divide(org.openl.meta.FloatValue value1, org.openl.meta.FloatValue value2) {
        if (value1 == null && value2 == null) {
            return null;
        }

        if (value1 == null && value2.doubleValue() != 0) {
            return new org.openl.meta.FloatValue(value1,
                value2,
                Operators.divide(1.0f, value2.getValue()),
                Formulas.DIVIDE);
        }

        if (value2 == null) {
            return new org.openl.meta.FloatValue(value1, value2, value1.getValue(), Formulas.DIVIDE);
        }

        if (value2.doubleValue() == 0) {
            throw new OpenLRuntimeException("Division by zero");
        }

        return new org.openl.meta.FloatValue(value1,
            value2,
            Operators.divide(value1.getValue(), value2.getValue()),
            Formulas.DIVIDE);
    }

    public static org.openl.meta.BigIntegerValue multiply(org.openl.meta.BigIntegerValue value1,
            org.openl.meta.BigIntegerValue value2) {
        // temporary commented to support operations with nulls
        //
        // validate(value1, value2, Formulas.MULTIPLY.toString());
        if (value1 == null) {
            return value2;
        }

        if (value2 == null) {
            return value1;
        }

        return new org.openl.meta.BigIntegerValue(value1,
            value2,
            Operators.multiply(value1.getValue(), value2.getValue()),
            Formulas.MULTIPLY);
    }

    public static org.openl.meta.BigDecimalValue divide(org.openl.meta.BigIntegerValue value1,
            org.openl.meta.BigIntegerValue value2) {
        // temporary commented to support operations with nulls
        //
        // validate(value1, value2, Formulas.DIVIDE.toString());
        if (value1 == null && value2 == null) {
            return null;
        }

        if (value1 == null && value2.doubleValue() != 0) {
            return new org.openl.meta.BigDecimalValue(null,
                new BigDecimalValue(new BigDecimal(value2.getValue())),
                divide(BigInteger.ONE, value2.getValue()),
                Formulas.DIVIDE);
        }

        if (value2 == null) {
            return new org.openl.meta.BigDecimalValue(new BigDecimalValue(new BigDecimal(value1.getValue())),
                null,
                new BigDecimal(value1.getValue()),
                Formulas.DIVIDE);
        }

        if (value2.doubleValue() == 0) {
            throw new OpenLRuntimeException("Division by zero");
        }

        return new org.openl.meta.BigDecimalValue(new BigDecimalValue(new BigDecimal(value1.getValue())),
            new BigDecimalValue(new BigDecimal(value2.getValue())),
            Operators.divide(value1.getValue(), value2.getValue()),
            Formulas.DIVIDE);
    }

    public static org.openl.meta.DoubleValue multiply(org.openl.meta.DoubleValue value1,
            org.openl.meta.DoubleValue value2) {
        // temporary commented to support operations with nulls
        //
        // validate(value1, value2, Formulas.MULTIPLY.toString());
        if (value1 == null) {
            return value2;
        }

        if (value2 == null) {
            return value1;
        }

        return new org.openl.meta.DoubleValue(value1,
            value2,
            Operators.multiply(value1.getValue(), value2.getValue()),
            Formulas.MULTIPLY);
    }

    public static org.openl.meta.DoubleValue divide(org.openl.meta.DoubleValue value1,
            org.openl.meta.DoubleValue value2) {
        if (value1 == null && value2 == null) {
            return null;
        }

        if (value1 == null && value2.doubleValue() != 0) {
            return new org.openl.meta.DoubleValue(value1,
                value2,
                Operators.divide(1.0, value2.getValue()),
                Formulas.DIVIDE);
        }

        if (value2 == null) {
            return new org.openl.meta.DoubleValue(value1, value2, value1.getValue(), Formulas.DIVIDE);
        }

        return new org.openl.meta.DoubleValue(value1,
            value2,
            Operators.divide(value1.getValue(), value2.getValue()),
            Formulas.DIVIDE);
    }

    public static org.openl.meta.BigDecimalValue multiply(org.openl.meta.BigDecimalValue value1,
            org.openl.meta.BigDecimalValue value2) {
        // temporary commented to support operations with nulls
        //
        // validate(value1, value2, Formulas.MULTIPLY.toString());
        if (value1 == null) {
            return value2;
        }

        if (value2 == null) {
            return value1;
        }

        return new org.openl.meta.BigDecimalValue(value1,
            value2,
            Operators.multiply(value1.getValue(), value2.getValue()),
            Formulas.MULTIPLY);
    }

    public static org.openl.meta.BigDecimalValue divide(org.openl.meta.BigDecimalValue value1,
            org.openl.meta.BigDecimalValue value2) {
        if (value1 == null && value2 == null) {
            return null;
        }

        if (value1 == null && value2.doubleValue() != 0) {
            return new org.openl.meta.BigDecimalValue(value1,
                value2,
                Operators.divide(BigDecimal.ONE, value2.getValue()),
                Formulas.DIVIDE);
        }

        if (value2 == null) {
            return new org.openl.meta.BigDecimalValue(value1, value2, value1.getValue(), Formulas.DIVIDE);
        }

        if (value2.doubleValue() == 0) {
            throw new OpenLRuntimeException("Division by zero");
        }

        return new org.openl.meta.BigDecimalValue(value1,
            value2,
            Operators.divide(value1.getValue(), value2.getValue()),
            Formulas.DIVIDE);
    }
}
