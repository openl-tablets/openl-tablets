package org.openl.rules.dt.type;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

import org.openl.meta.BigDecimalValue;
import org.openl.meta.BigIntegerValue;
import org.openl.meta.ByteValue;
import org.openl.meta.DoubleValue;
import org.openl.meta.FloatValue;
import org.openl.meta.IntValue;
import org.openl.meta.LongValue;
import org.openl.meta.ShortValue;

public interface ITypeAdaptor<T, C extends Comparable<C>> {

    public C convert(T param);

    C increment(C value);

    abstract static class NumberTypeAdaptor<N extends Number, C extends Comparable<C>> implements ITypeAdaptor<N, C> {
        @Override
        @SuppressWarnings("unchecked")
        public C convert(N param) {
            return (C) param;
        }

    }

    public static ITypeAdaptor<String, String> STRING = new ITypeAdaptor<String, String>() {

        @Override
        public String convert(String param) {
            return param;
        }

        @Override
        public String increment(String value) {
            if (value == null) {
                throw new IllegalArgumentException("value can't be null!");
            }
            int d = 1;
            StringBuilder sb = new StringBuilder();
            int i = value.length() - 1;
            while (i >= 0) {
                if (d > 0) {
                    if (value.charAt(i) != Character.MAX_CODE_POINT) {
                        sb.append((char) (value.charAt(i) + d));
                        d = 0;
                    } else {
                        sb.append(Character.MIN_CODE_POINT);
                    }
                } else {
                    sb.append(value.charAt(i));
                }
                i--;
            }
            sb.reverse();
            return sb.toString();
        }

        @Override
        public Class<String> getTargetType() {
            return String.class;
        }

    };

    public static ITypeAdaptor<Byte, Byte> BYTE = new NumberTypeAdaptor<Byte, Byte>() {
        @Override
        public Byte increment(Byte value) {
            if (value == null) {
                throw new IllegalArgumentException("value can't be null!");
            }
            if (value.equals(Byte.MAX_VALUE)) {
                return null;
            }
            return (byte) (value + 1);
        }

        @Override
        public Class<Byte> getTargetType() {
            return Byte.class;
        }

    };

    public static ITypeAdaptor<ByteValue, Byte> BYTE_VALUE = new NumberTypeAdaptor<ByteValue, Byte>() {
        @Override
        public Byte convert(ByteValue param) {
            if (param == null) {
                return null;
            }

            return param.getValue();
        }

        @Override
        public Byte increment(Byte value) {
            if (value == null) {
                throw new IllegalArgumentException("value can't be null!");
            }
            if (value.equals(Byte.MAX_VALUE)) {
                return null;
            }
            return (byte) (value + 1);
        }

        @Override
        public Class<Byte> getTargetType() {
            return Byte.class;
        }

    };

    public static ITypeAdaptor<Short, Short> SHORT = new NumberTypeAdaptor<Short, Short>() {
        @Override
        public Short increment(Short value) {
            if (value == null) {
                throw new IllegalArgumentException("value can't be null!");
            }
            if (value.equals(Short.MAX_VALUE)) {
                return null;
            }

            return (short) (value + 1);
        }

        @Override
        public Class<Short> getTargetType() {
            return Short.class;
        }
    };

    public static ITypeAdaptor<ShortValue, Short> SHORT_VALUE = new NumberTypeAdaptor<ShortValue, Short>() {
        @Override
        public Short convert(ShortValue param) {
            if (param == null) {
                return null;
            }
            return param.shortValue();
        }

        @Override
        public Short increment(Short value) {
            if (value == null) {
                throw new IllegalArgumentException("value can't be null!");
            }
            if (value.equals(Short.MAX_VALUE)) {
                return null;
            }

            return (short) (value + 1);
        }

        @Override
        public Class<Short> getTargetType() {
            return Short.class;
        }
    };

    public static ITypeAdaptor<Long, Long> LONG = new NumberTypeAdaptor<Long, Long>() {

        @Override
        public Long increment(Long value) {
            if (value == null) {
                throw new IllegalArgumentException("value can't be null!");
            }
            if (value.equals(Long.MAX_VALUE)) {
                return null;
            }
            return value + 1;
        }

        @Override
        public Class<Long> getTargetType() {
            return Long.class;
        }

    };

    public static ITypeAdaptor<LongValue, Long> LONG_VALUE = new NumberTypeAdaptor<LongValue, Long>() {

        @Override
        public Long convert(LongValue param) {
            if (param == null) {
                return null;
            }
            return param.longValue();
        }

        @Override
        public Long increment(Long value) {
            if (value == null) {
                throw new IllegalArgumentException("value can't be null!");
            }
            if (value.equals(Long.MAX_VALUE)) {
                return null;
            }

            return value + 1;
        }

        @Override
        public Class<Long> getTargetType() {
            return Long.class;
        }

    };

    public static ITypeAdaptor<Double, Double> DOUBLE = new NumberTypeAdaptor<Double, Double>() {

        @Override
        public Double increment(Double value) {
            if (value.isNaN()) {
                return Double.NaN;
            }
            if (value.isInfinite()) {
                return value;
            }
            return value + Math.ulp(value);
        }

        @Override
        public Class<Double> getTargetType() {
            return Double.class;
        }

    };

    public static ITypeAdaptor<DoubleValue, Double> DOUBLE_VALUE = new NumberTypeAdaptor<DoubleValue, Double>() {

        @Override
        public Double convert(DoubleValue param) {
            if (param == null) {
                return null;
            }
            return param.doubleValue();
        }

        @Override
        public Double increment(Double value) {
            if (value.isNaN()) {
                return Double.NaN;
            }
            if (value.isInfinite()) {
                return value;
            }
            return value + Math.ulp(value);
        }

        @Override
        public Class<Double> getTargetType() {
            return Double.class;
        }

    };

    public static ITypeAdaptor<Float, Float> FLOAT = new NumberTypeAdaptor<Float, Float>() {

        @Override
        public Float increment(Float value) {
            if (value.isNaN()) {
                return Float.NaN;
            }
            if (value.isInfinite()) {
                return value;
            }
            return value + Math.ulp(value);
        }

        @Override
        public Class<Float> getTargetType() {
            return Float.class;
        }

    };

    public static ITypeAdaptor<BigInteger, BigInteger> BIGINTEGER = new NumberTypeAdaptor<BigInteger, BigInteger>() {
        @Override
        public BigInteger increment(BigInteger value) {
            return value.add(BigInteger.ONE);
        }

        @Override
        public Class<BigInteger> getTargetType() {
            return BigInteger.class;
        }
    };

    public static ITypeAdaptor<BigIntegerValue, BigInteger> BIGINTEGER_VALUE = new NumberTypeAdaptor<BigIntegerValue, BigInteger>() {
        @Override
        public BigInteger convert(BigIntegerValue param) {
            if (param == null) {
                return null;
            }
            return param.getValue();
        }

        @Override
        public BigInteger increment(BigInteger value) {
            return value.add(BigInteger.ONE);
        }

        @Override
        public Class<BigInteger> getTargetType() {
            return BigInteger.class;
        }
    };

    public static ITypeAdaptor<BigDecimal, BigDecimal> BIGDECIMAL = new NumberTypeAdaptor<BigDecimal, BigDecimal>() {
        @Override
        public BigDecimal increment(BigDecimal value) {
            return value.add(value.ulp());
        }

        @Override
        public Class<BigDecimal> getTargetType() {
            return BigDecimal.class;
        }

    };

    public static ITypeAdaptor<BigDecimalValue, BigDecimal> BIGDECIMAL_VALUE = new NumberTypeAdaptor<BigDecimalValue, BigDecimal>() {
        @Override
        public BigDecimal convert(BigDecimalValue param) {
            if (param == null) {
                return null;
            }
            return param.getValue();
        }

        @Override
        public BigDecimal increment(BigDecimal value) {
            return value.add(value.ulp());
        }

        @Override
        public Class<BigDecimal> getTargetType() {
            return BigDecimal.class;
        }
    };

    public static ITypeAdaptor<FloatValue, Float> FLOAT_VALUE = new NumberTypeAdaptor<FloatValue, Float>() {

        @Override
        public Float convert(FloatValue param) {
            if (param == null) {
                return null;
            }
            return param.floatValue();
        }

        @Override
        public Float increment(Float value) {
            if (value.isNaN()) {
                return Float.NaN;
            }
            if (value.isInfinite()) {
                return value;
            }
            return value + Math.ulp(value);
        }

        @Override
        public Class<Float> getTargetType() {
            return Float.class;
        }

    };

    public static ITypeAdaptor<Integer, Integer> INT = new NumberTypeAdaptor<Integer, Integer>() {

        @Override
        public Integer increment(Integer value) {
            if (value == null) {
                throw new IllegalArgumentException("value can't be null!");
            }
            if (value.equals(Integer.MAX_VALUE)) {
                return null;
            }
            return value + 1;
        }

        @Override
        public Class<Integer> getTargetType() {
            return Integer.class;
        }

    };

    public static ITypeAdaptor<IntValue, Integer> INT_VALUE = new NumberTypeAdaptor<IntValue, Integer>() {

        @Override
        public Integer convert(IntValue param) {
            if (param == null) {
                return null;
            }
            return param.intValue();
        }

        @Override
        public Integer increment(Integer value) {
            if (value == null) {
                throw new IllegalArgumentException("value can't be null!");
            }
            if (value.equals(Integer.MAX_VALUE)) {
                return null;
            }
            return value + 1;
        }

        @Override
        public Class<Integer> getTargetType() {
            return Integer.class;
        }

    };

    public static ITypeAdaptor<Date, Integer> DATE = new ITypeAdaptor<Date, Integer>() {

        static final long MS_IN_A_DAY = 1000 * 3600 * 24l;

        @Override
        public Integer convert(Date date) {

            return (int) (date.getTime() / MS_IN_A_DAY);
        }

        @Override
        public Integer increment(Integer value) {
            return value + 1;
        }

        @Override
        public Class<Integer> getTargetType() {
            return Integer.class;
        }

    };

    public Class<C> getTargetType();

}
