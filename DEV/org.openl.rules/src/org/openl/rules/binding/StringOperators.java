package org.openl.rules.binding;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.openl.binding.impl.Operators;
import org.openl.conf.OperatorsNamespace;
import org.openl.meta.BigDecimalValue;
import org.openl.meta.BigIntegerValue;
import org.openl.meta.ByteValue;
import org.openl.meta.DoubleValue;
import org.openl.meta.FloatValue;
import org.openl.meta.IntValue;
import org.openl.meta.LongValue;
import org.openl.meta.ShortValue;
import org.openl.meta.number.Formulas;

@OperatorsNamespace
@Deprecated
public class StringOperators extends Operators {

    private static final int STRING_TO_TYPE_DISTANCE = 40;
    private static final int TYPE_TO_STRING_DISTANCE = 41;

    public static Byte add(Byte x, String y) {
        return (byte) (x + Byte.valueOf(y));
    }

    public static Byte add(String x, Byte y) {
        return (byte) (y + Byte.valueOf(x));
    }

    public static Short add(Short x, String y) {
        return (short) (x + Short.valueOf(y));
    }

    public static Short add(String x, Short y) {
        return (short) (y + Short.valueOf(x));
    }

    public static Integer add(Integer x, String y) {
        return x + Integer.valueOf(y);
    }

    public static Integer add(String x, Integer y) {
        return y + Integer.valueOf(x);
    }

    public static Long add(Long x, String y) {
        return x + Long.valueOf(y);
    }

    public static Long add(String x, Long y) {
        return y + Long.valueOf(x);
    }

    public static Float add(Float x, String y) {
        return x + Float.valueOf(y);
    }

    public static Float add(String x, Float y) {
        return y + Float.valueOf(x);
    }

    public static Double add(Double x, String y) {
        return x + Double.valueOf(y);
    }

    public static Double add(String x, Double y) {
        return y + Double.valueOf(x);
    }

    public static BigInteger add(String x, BigInteger y) {
        return new BigInteger(x).add(y);
    }

    public static BigInteger add(BigInteger x, String y) {
        return new BigInteger(y).add(x);
    }

    public static BigDecimal add(String x, BigDecimal y) {
        return new BigDecimal(x).add(y);
    }

    public static BigDecimal add(BigDecimal x, String y) {
        return new BigDecimal(y).add(x);
    }

    // Add Value Types
    public static DoubleValue add(DoubleValue value1, String value2) {
        if (value2 == null) {
            return value1;
        }

        if (value1 == null) {
            return new DoubleValue(Double.valueOf(value2));
        }

        double v = Double.valueOf(value2);

        return new org.openl.meta.DoubleValue(value1,
            new DoubleValue(v),
            Operators.add(value1.getValue(), v),
            Formulas.ADD);
    }

    public static DoubleValue add(String value1, DoubleValue value2) {
        if (value1 == null) {
            return value2;
        }

        if (value2 == null) {
            return new DoubleValue(Double.valueOf(value1));
        }

        double v = Double.valueOf(value1);

        return new org.openl.meta.DoubleValue(new DoubleValue(v),
            value2,
            Operators.add(v, value2.getValue()),
            Formulas.ADD);
    }

    public static IntValue add(IntValue value1, String value2) {
        if (value2 == null) {
            return value1;
        }

        if (value1 == null) {
            return new IntValue(Integer.valueOf(value2));
        }

        int v = Integer.valueOf(value2);

        return new org.openl.meta.IntValue(value1, new IntValue(v), Operators.add(value1.getValue(), v), Formulas.ADD);
    }

    public static IntValue add(String value1, IntValue value2) {
        if (value1 == null) {
            return value2;
        }

        if (value2 == null) {
            return new IntValue(Integer.valueOf(value1));
        }

        int v = Integer.valueOf(value1);

        return new org.openl.meta.IntValue(new IntValue(v), value2, Operators.add(v, value2.getValue()), Formulas.ADD);
    }

    public static BigDecimalValue add(BigDecimalValue value1, String value2) {
        if (value2 == null) {
            return value1;
        }

        if (value1 == null) {
            return new BigDecimalValue(new BigDecimal(value2));
        }

        BigDecimal v = new BigDecimal(value2);

        return new org.openl.meta.BigDecimalValue(value1,
            new BigDecimalValue(v),
            Operators.add(value1.getValue(), v),
            Formulas.ADD);
    }

    public static BigDecimalValue add(String value1, BigDecimalValue value2) {
        if (value1 == null) {
            return value2;
        }

        if (value2 == null) {
            return new BigDecimalValue(new BigDecimal(value1));
        }

        BigDecimal v = new BigDecimal(value1);

        return new org.openl.meta.BigDecimalValue(new BigDecimalValue(v),
            value2,
            Operators.add(v, value2.getValue()),
            Formulas.ADD);
    }

    public static BigIntegerValue add(BigIntegerValue value1, String value2) {
        if (value2 == null) {
            return value1;
        }

        if (value1 == null) {
            return new BigIntegerValue(new BigInteger(value2));
        }

        BigInteger v = new BigInteger(value2);

        return new org.openl.meta.BigIntegerValue(value1,
            new BigIntegerValue(v),
            Operators.add(value1.getValue(), v),
            Formulas.ADD);
    }

    public static BigIntegerValue add(String value1, BigIntegerValue value2) {
        if (value1 == null) {
            return value2;
        }

        if (value2 == null) {
            return new BigIntegerValue(new BigInteger(value1));
        }

        BigInteger v = new BigInteger(value1);

        return new org.openl.meta.BigIntegerValue(new BigIntegerValue(v),
            value2,
            Operators.add(v, value2.getValue()),
            Formulas.ADD);
    }

    public static ByteValue add(ByteValue value1, String value2) {
        if (value2 == null) {
            return value1;
        }

        if (value1 == null) {
            return new ByteValue(Byte.valueOf(value2));
        }

        byte v = Byte.valueOf(value2);

        return new org.openl.meta.ByteValue(value1,
            new ByteValue(v),
            Operators.add(value1.getValue(), v),
            Formulas.ADD);
    }

    public static ByteValue add(String value1, ByteValue value2) {
        if (value1 == null) {
            return value2;
        }

        if (value2 == null) {
            return new ByteValue(Byte.valueOf(value1));
        }

        byte v = Byte.valueOf(value1);

        return new org.openl.meta.ByteValue(new ByteValue(v),
            value2,
            Operators.add(v, value2.getValue()),
            Formulas.ADD);
    }

    public static FloatValue add(FloatValue value1, String value2) {
        if (value2 == null) {
            return value1;
        }

        if (value1 == null) {
            return new FloatValue(Float.valueOf(value2));
        }

        float v = Float.valueOf(value2);

        return new org.openl.meta.FloatValue(value1,
            new FloatValue(v),
            Operators.add(value1.getValue(), v),
            Formulas.ADD);
    }

    public static FloatValue add(String value1, FloatValue value2) {
        if (value1 == null) {
            return value2;
        }

        if (value2 == null) {
            return new FloatValue(Float.valueOf(value1));
        }

        float v = Float.valueOf(value1);

        return new org.openl.meta.FloatValue(new FloatValue(v),
            value2,
            Operators.add(v, value2.getValue()),
            Formulas.ADD);
    }

    public static LongValue add(LongValue value1, String value2) {
        if (value2 == null) {
            return value1;
        }

        if (value1 == null) {
            return new LongValue(Long.valueOf(value2));
        }

        long v = Long.valueOf(value2);

        return new org.openl.meta.LongValue(value1,
            new LongValue(v),
            Operators.add(value1.getValue(), v),
            Formulas.ADD);
    }

    public static LongValue add(String value1, LongValue value2) {
        if (value1 == null) {
            return value2;
        }

        if (value2 == null) {
            return new LongValue(Long.valueOf(value1));
        }

        long v = Long.valueOf(value1);

        return new org.openl.meta.LongValue(new LongValue(v),
            value2,
            Operators.add(v, value2.getValue()),
            Formulas.ADD);
    }

    public static ShortValue add(ShortValue value1, String value2) {
        if (value2 == null) {
            return value1;
        }

        if (value1 == null) {
            return new ShortValue(Short.valueOf(value2));
        }

        short v = Short.valueOf(value2);

        return new org.openl.meta.ShortValue(value1,
            new ShortValue(v),
            Operators.add(value1.getValue(), v),
            Formulas.ADD);
    }

    public static ShortValue add(String value1, ShortValue value2) {
        if (value1 == null) {
            return value2;
        }

        if (value2 == null) {
            return new ShortValue(Short.valueOf(value1));
        }

        short v = Short.valueOf(value1);

        return new org.openl.meta.ShortValue(new ShortValue(v),
            value2,
            Operators.add(v, value2.getValue()),
            Formulas.ADD);
    }

    // AutoCasts to String
    public static String autocast(byte x, String y) {
        return Byte.toString(x);
    }

    public static Integer distance(byte x, String y) {
        return TYPE_TO_STRING_DISTANCE;
    }

    public static byte autocast(String x, byte y) {
        return Byte.valueOf(x);
    }

    public static Integer distance(String x, byte y) {
        return STRING_TO_TYPE_DISTANCE;
    }

    public static String autocast(short x, String y) {
        return Short.toString(x);
    }

    public static Integer distance(short x, String y) {
        return TYPE_TO_STRING_DISTANCE;
    }

    public static short autocast(String x, short y) {
        return Short.valueOf(x);
    }

    public static Integer distance(String x, short y) {
        return STRING_TO_TYPE_DISTANCE;
    }

    public static String autocast(int x, String y) {
        return Integer.toString(x);
    }

    public static Integer distance(int x, String y) {
        return TYPE_TO_STRING_DISTANCE;
    }

    public static int autocast(String x, int y) {
        return Integer.valueOf(x);
    }

    public static Integer distance(String x, int y) {
        return STRING_TO_TYPE_DISTANCE;
    }

    public static String autocast(long x, String y) {
        return Long.toString(x);
    }

    public static Integer distance(long x, String y) {
        return TYPE_TO_STRING_DISTANCE;
    }

    public static long autocast(String x, long y) {
        return Long.valueOf(x);
    }

    public static Integer distance(String x, long y) {
        return STRING_TO_TYPE_DISTANCE;
    }

    public static String autocast(float x, String y) {
        return Float.toString(x);
    }

    public static Integer distance(float x, String y) {
        return TYPE_TO_STRING_DISTANCE;
    }

    public static float autocast(String x, float y) {
        return Float.valueOf(x);
    }

    public static Integer distance(String x, float y) {
        return STRING_TO_TYPE_DISTANCE;
    }

    public static String autocast(double x, String y) {
        return Double.toString(x);
    }

    public static Integer distance(double x, String y) {
        return TYPE_TO_STRING_DISTANCE;
    }

    public static double autocast(String x, double y) {
        return Double.valueOf(x);
    }

    public static Integer distance(String x, double y) {
        return STRING_TO_TYPE_DISTANCE;
    }

    // AutoCasts to String
    public static String autocast(Byte x, String y) {
        return Byte.toString(x);
    }

    public static Integer distance(Byte x, String y) {
        return TYPE_TO_STRING_DISTANCE;
    }

    public static Byte autocast(String x, Byte y) {
        return x == null ? null :Byte.valueOf(x);
    }

    public static Integer distance(String x, Byte y) {
        return STRING_TO_TYPE_DISTANCE;
    }

    public static String autocast(Short x, String y) {
        return Short.toString(x);
    }

    public static Integer distance(Short x, String y) {
        return TYPE_TO_STRING_DISTANCE;
    }

    public static short autocast(String x, Short y) {
        return x == null ? null : Short.valueOf(x);
    }

    public static Integer distance(String x, Short y) {
        return STRING_TO_TYPE_DISTANCE;
    }

    public static String autocast(Integer x, String y) {
        return Integer.toString(x);
    }

    public static Integer distance(Integer x, String y) {
        return TYPE_TO_STRING_DISTANCE;
    }

    public static Integer autocast(String x, Integer y) {
        return x == null ? null : Integer.valueOf(x);
    }

    public static Integer distance(String x, Integer y) {
        return STRING_TO_TYPE_DISTANCE;
    }

    public static String autocast(Long x, String y) {
        return Long.toString(x);
    }

    public static Integer distance(Long x, String y) {
        return TYPE_TO_STRING_DISTANCE;
    }

    public static Long autocast(String x, Long y) {
        return x == null ? null : Long.valueOf(x);
    }

    public static Integer distance(String x, Long y) {
        return STRING_TO_TYPE_DISTANCE;
    }

    public static String autocast(Float x, String y) {
        return Float.toString(x);
    }

    public static Integer distance(Float x, String y) {
        return TYPE_TO_STRING_DISTANCE;
    }

    public static Float autocast(String x, Float y) {
        return x == null ? null : Float.valueOf(x);
    }

    public static Integer distance(String x, Float y) {
        return STRING_TO_TYPE_DISTANCE;
    }

    public static String autocast(Double x, String y) {
        return Double.toString(x);
    }

    public static Integer distance(Double x, String y) {
        return TYPE_TO_STRING_DISTANCE;
    }

    public static Double autocast(String x, Double y) {
        return x == null ? null : Double.valueOf(x);
    }

    public static Integer distance(String x, Double y) {
        return STRING_TO_TYPE_DISTANCE;
    }

    public static String autocast(BigDecimal x, String y) {
        return x.toString();
    }

    public static Integer distance(BigDecimal x, String y) {
        return TYPE_TO_STRING_DISTANCE;
    }

    public static BigDecimal autocast(String x, BigDecimal y) {
        return new BigDecimal(x);
    }

    public static Integer distance(String x, BigDecimal y) {
        return STRING_TO_TYPE_DISTANCE;
    }

    public static String autocast(BigInteger x, String y) {
        return x.toString();
    }

    public static Integer distance(BigInteger x, String y) {
        return TYPE_TO_STRING_DISTANCE;
    }

    public static BigInteger autocast(String x, BigInteger y) {
        return new BigInteger(x);
    }

    public static Integer distance(String x, BigInteger y) {
        return STRING_TO_TYPE_DISTANCE;
    }

    // Value types casts
    public static String autocast(DoubleValue x, String y) {
        if (x == null) {
            return null;
        }

        return x.toString();
    }

    public static Integer distance(DoubleValue x, String y) {
        return TYPE_TO_STRING_DISTANCE;
    }

    public static DoubleValue autocast(String x, DoubleValue y) {
        if (x == null || "".equals(x)) {
            return null;
        }
        return new DoubleValue(Double.valueOf(x));
    }

    public static Integer distance(String x, DoubleValue y) {
        return STRING_TO_TYPE_DISTANCE;
    }

    public static String autocast(BigDecimalValue x, String y) {
        if (x == null) {
            return null;
        }
        return x.toString();
    }

    public static Integer distance(BigDecimalValue x, String y) {
        return TYPE_TO_STRING_DISTANCE;
    }

    public static BigDecimalValue autocast(String x, BigDecimalValue y) {
        if (x == null) {
            return null;
        }
        return new BigDecimalValue(new BigDecimal(x));
    }

    public static Integer distance(String x, BigDecimalValue y) {
        return STRING_TO_TYPE_DISTANCE;
    }

    public static String autocast(BigIntegerValue x, String y) {
        if (x == null) {
            return null;
        }
        return x.toString();
    }

    public static Integer distance(BigIntegerValue x, String y) {
        return TYPE_TO_STRING_DISTANCE;
    }

    public static BigIntegerValue autocast(String x, BigIntegerValue y) {
        if (x == null) {
            return null;
        }
        return new BigIntegerValue(new BigInteger(x));
    }

    public static Integer distance(String x, BigIntegerValue y) {
        return STRING_TO_TYPE_DISTANCE;
    }

    public static String autocast(ByteValue x, String y) {
        if (x == null) {
            return null;
        }
        return x.toString();
    }

    public static Integer distance(ByteValue x, String y) {
        return TYPE_TO_STRING_DISTANCE;
    }

    public static ByteValue autocast(String x, ByteValue y) {
        if (x == null) {
            return null;
        }
        return new ByteValue(Byte.valueOf(x));
    }

    public static Integer distance(String x, ByteValue y) {
        return STRING_TO_TYPE_DISTANCE;
    }

    public static String autocast(FloatValue x, String y) {
        if (x == null) {
            return null;
        }
        return x.toString();
    }

    public static Integer distance(FloatValue x, String y) {
        return TYPE_TO_STRING_DISTANCE;
    }

    public static FloatValue autocast(String x, FloatValue y) {
        if (x == null) {
            return null;
        }
        return new FloatValue(Float.valueOf(x));
    }

    public static Integer distance(String x, FloatValue y) {
        return STRING_TO_TYPE_DISTANCE;
    }

    public static String autocast(IntValue x, String y) {
        if (x == null) {
            return null;
        }
        return x.toString();
    }

    public static Integer distance(IntValue x, String y) {
        return TYPE_TO_STRING_DISTANCE;
    }

    public static IntValue autocast(String x, IntValue y) {
        if (x == null) {
            return null;
        }
        return new IntValue(Integer.valueOf(x));
    }

    public static Integer distance(String x, IntValue y) {
        return STRING_TO_TYPE_DISTANCE;
    }

    public static String autocast(LongValue x, String y) {
        if (x == null) {
            return null;
        }
        return x.toString();
    }

    public static Integer distance(LongValue x, String y) {
        return TYPE_TO_STRING_DISTANCE;
    }

    public static LongValue autocast(String x, LongValue y) {
        if (x == null) {
            return null;
        }
        return new LongValue(Long.valueOf(x));
    }

    public static Integer distance(String x, LongValue y) {
        return STRING_TO_TYPE_DISTANCE;
    }

    public static String autocast(ShortValue x, String y) {
        if (x == null) {
            return null;
        }
        return x.toString();
    }

    public static Integer distance(ShortValue x, String y) {
        return TYPE_TO_STRING_DISTANCE;
    }

    public static ShortValue autocast(String x, ShortValue y) {
        if (x == null) {
            return null;
        }
        return new ShortValue(Short.valueOf(x));
    }

    public static Integer distance(String x, ShortValue y) {
        return STRING_TO_TYPE_DISTANCE;
    }

    public static boolean gt(String x, Integer y) {
        return x != null && y!= null && Integer.valueOf(x) > y;
    }

    public static boolean ge(String x, Integer y) {
        return x != null && y!= null && Integer.valueOf(x) >= y;
    }

    public static boolean lt(String x, Integer y) {
        return x != null && y!= null && Integer.valueOf(x) < y;
    }

    public static boolean le(String x, Integer y) {
        return x != null && y!= null && Integer.valueOf(x) <= y;
    }

    public static boolean eq(String x, Integer y) {
        return x == null && y == null || x!= null && y!= null && Integer.valueOf(x) == y;
    }

    public static boolean ne(String x, Integer y) {
        return !eq(x, y);
    }

    public static boolean gt(Integer x, String y) {
        return x != null && y!= null && Integer.valueOf(y) < x;
    }

    public static boolean ge(Integer x, String y) {
        return x != null && y!= null && Integer.valueOf(y) <= x;
    }

    public static boolean lt(Integer x, String y) {
        return x != null && y!= null && Integer.valueOf(y) > x;
    }

    public static boolean le(Integer x, String y) {
        return x != null && y!= null && Integer.valueOf(y) >= x;
    }


    public static boolean eq(Integer x, String y) {
        return eq(y, x);
    }

    public static boolean ne(Integer x, String y) {
        return ne(y, x);
    }
}
