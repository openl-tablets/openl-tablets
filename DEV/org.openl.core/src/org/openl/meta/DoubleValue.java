package org.openl.meta;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.openl.binding.impl.Operators;
import org.openl.binding.impl.operator.Comparison;
import org.openl.exception.OpenLRuntimeException;
import org.openl.meta.DoubleValue.DoubleValueAdapter;
import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.meta.number.CastOperand;
import org.openl.meta.number.Formulas;
import org.openl.meta.number.NumberOperations;
import org.openl.rules.util.*;
import org.openl.util.ArrayTool;
import org.openl.util.math.MathUtils;

@XmlRootElement
@XmlJavaTypeAdapter(DoubleValueAdapter.class)
public class DoubleValue extends ExplanationNumberValue<DoubleValue> implements Comparable<DoubleValue> {

    private static final long serialVersionUID = -4594250562069599646L;

    public static class DoubleValueOne extends DoubleValue {

        private static final long serialVersionUID = 6347462002516785250L;

        private DoubleValueOne() {
            super(1.0);
        }

        public DoubleValue multiply(DoubleValue dv) {
            return dv;
        }
    }

    public static class DoubleValueAdapter extends XmlAdapter<Double, DoubleValue> {
        @Override
        public DoubleValue unmarshal(Double val) throws Exception {
            if (val == null) {
                return null;
            }
            return new DoubleValue(val);
        }

        @Override
        public Double marshal(DoubleValue val) throws Exception {
            if (val == null) {
                return null;
            }
            return val.doubleValue();
        }
    }

    public static class DoubleValueZero extends DoubleValue {

        private static final long serialVersionUID = 3329865368482848868L;

        private DoubleValueZero() {
            super(0.0);
        }

        public DoubleValue add(DoubleValue dv) {
            return dv;
        }

        public DoubleValue divide(DoubleValue dv) {
            return this;
        }

        public DoubleValue multiply(DoubleValue dv) {
            return this;
        }
    }

    public static final DoubleValue ZERO = new DoubleValueZero();
    public static final DoubleValue ONE = new DoubleValueOne();
    public static final DoubleValue MINUS_ONE = new DoubleValue(-1);

    private double value;

    /**
     * EPBDS-6107
     */
    public void setValue(double value) {
        this.value = value;
    }

    static DoubleValue instance(Double result, NumberOperations operation, DoubleValue... values) {
        return result == null ? null : new DoubleValue(result, operation, values);
    }

    private static DoubleValue instance(DoubleValue result, NumberOperations operation, DoubleValue... values) {
        return result == null ? null : new DoubleValue(result.doubleValue(), operation, values);
    }

    public static DoubleValue max(DoubleValue... values) {
        return instance(Statistics.max(values), NumberOperations.MAX, values);
    }

    public static DoubleValue min(DoubleValue... values) {
        return instance(Statistics.min(values), NumberOperations.MIN, values);
    }

    public static DoubleValue sum(DoubleValue... values) {
        return instance(Sum.sum(unwrap(values)), NumberOperations.SUM, values);
    }

    public static DoubleValue avg(DoubleValue... values) {
        return instance(Avg.avg(unwrap(values)), NumberOperations.AVG, values);
    }

    public static DoubleValue median(DoubleValue... values) {
        return instance(MathUtils.median(unwrap(values)), NumberOperations.MEDIAN, values);
    }

    public static DoubleValue product(DoubleValue... values) {
        return instance(Product.product(unwrap(values)), NumberOperations.PRODUCT, values);
    }

    /**
     * Compares two values
     *
     * @param value1
     * @param value2
     * @return true if value1 equal value2
     */
    public static boolean eq(DoubleValue value1, DoubleValue value2) {
        if (value1 == null || value2 == null) {
            return value1 == value2;
        }
        return Comparison.eq(value1.getValue(), value2.getValue());
    }

    /**
     * Compares two values
     *
     * @param value1
     * @param value2
     * @return true if value1 greater or equal value2
     */
    public static Boolean ge(DoubleValue value1, DoubleValue value2) {
        Double v1 = value1 == null ? null : value1.value;
        Double v2 = value2 == null ? null : value2.value;
        return Comparison.ge(v1, v2);
    }

    /**
     * Compares two values
     *
     * @param value1
     * @param value2
     * @return true if value1 greater value2
     */
    public static Boolean gt(DoubleValue value1, DoubleValue value2) {
        Double v1 = value1 == null ? null : value1.value;
        Double v2 = value2 == null ? null : value2.value;
        return Comparison.gt(v1, v2);
    }

    /**
     * Compares two values
     *
     * @param value1
     * @param value2
     * @return true if value1 less or equal value2
     */
    public static Boolean le(DoubleValue value1, DoubleValue value2) {
        Double v1 = value1 == null ? null : value1.value;
        Double v2 = value2 == null ? null : value2.value;
        return Comparison.le(v1, v2);
    }

    /**
     * Compares two values
     *
     * @param value1
     * @param value2
     * @return true if value1 less value2
     */
    public static Boolean lt(DoubleValue value1, DoubleValue value2) {
        Double v1 = value1 == null ? null : value1.value;
        Double v2 = value2 == null ? null : value2.value;
        return Comparison.lt(v1, v2);
    }

    /**
     * Compares two values
     *
     * @param value1
     * @param value2
     * @return true if value1 not equal value2
     */
    public static boolean ne(DoubleValue value1, DoubleValue value2) {
        if (value1 == null || value2 == null) {
            return value1 != value2;
        }

        return Comparison.ne(value1.getValue(), value2.getValue());
    }

    /**
     *
     * @param value of variable which should be copied
     * @param name of new variable
     * @return the new org.openl.meta.DoubleValue variable with name <b>name</b> and value <b>value</b>
     */
    public static DoubleValue copy(DoubleValue value, String name) {
        if (value.getName() == null) {
            value.setName(name);

            return value;
        } else if (!value.getName().equals(name)) {
            DoubleValue result = new DoubleValue(value.doubleValue(),
                NumberOperations.COPY,
                value);
            result.setName(name);

            return result;
        }
        return value;
    }

    // REM
    /**
     * Divides left hand operand by right hand operand and returns remainder
     *
     * @param value1 org.openl.meta.DoubleValue
     * @param value2 org.openl.meta.DoubleValue
     * @return remainder from division value1 by value2
     */
    public static DoubleValue rem(DoubleValue value1, DoubleValue value2) {
        // Commented to support operations with nulls. See also MathUtils.mod()
        // validate(value1, value2, Formulas.REM.toString());
        if (value1 == null || value2 == null) {
            return ZERO;
        }

        return new DoubleValue(value1,
            value2,
            Operators.rem(value1.getValue(), value2.getValue()),
            Formulas.REM);
    }

    /**
     * Adds left hand operand to right hand operand
     *
     * @param value1 org.openl.meta.DoubleValue
     * @param value2 org.openl.meta.DoubleValue
     * @return the result of addition operation
     */
    public static DoubleValue add(DoubleValue value1, DoubleValue value2) {
        if (value1 == null) {
            return value2;
        }

        if (value2 == null) {
            return value1;
        }

        return new DoubleValue(value1,
            value2,
            Operators.add(value1.getValue(), value2.getValue()),
            Formulas.ADD);
    }

    // MULTIPLY
    /**
     * Multiplies left hand operand to right hand operand
     *
     * @param value1 org.openl.meta.DoubleValue
     * @param value2 org.openl.meta.DoubleValue
     * @return the result of multiplication operation
     */
    public static DoubleValue multiply(DoubleValue value1, DoubleValue value2) {
        if (value1 == null || value2 == null) {
            return null;
        }

        return new DoubleValue(value1,
            value2,
            Operators.multiply(value1.getValue(), value2.getValue()),
            Formulas.MULTIPLY);
    }

    // SUBTRACT
    /**
     * Subtracts left hand operand to right hand operand
     *
     * @param value1 org.openl.meta.DoubleValue
     * @param value2 org.openl.meta.DoubleValue
     * @return the result of subtraction operation
     */
    public static DoubleValue subtract(DoubleValue value1, DoubleValue value2) {
        if (value1 == null && value2 == null) {
            return null;
        }

        if (value1 == null) {
            return negative(value2);
        }

        if (value2 == null) {
            return value1;
        }

        return new DoubleValue(value1,
            value2,
            Operators.subtract(value1.getValue(), value2.getValue()),
            Formulas.SUBTRACT);
    }

    // DIVIDE
    /**
     * Divides left hand operand by right hand operand
     *
     * @param value1 org.openl.meta.DoubleValue
     * @param value2 org.openl.meta.DoubleValue
     * @return the result of division operation
     */
    public static DoubleValue divide(DoubleValue value1,
            DoubleValue value2) {
        if (value1 == null || value2 == null) {
            return null;
        }

        return new DoubleValue(value1,
            value2,
            Operators.divide(value1.getValue(), value2.getValue()),
            Formulas.DIVIDE);
    }

    // QUAOTIENT
    /**
     * Divides left hand operand by right hand operand
     *
     * @param number org.openl.meta.DoubleValue
     * @param divisor org.openl.meta.DoubleValue
     * @return LongValue the result of division operation
     */
    public static LongValue quotient(DoubleValue number, DoubleValue divisor) {
        if (number != null && divisor != null) {
            LongValue result = new LongValue(MathUtils.quotient(number.getValue(), divisor.getValue()));
            return new LongValue(result, NumberOperations.QUOTIENT, null);
        }
        return null;
    }

    /**
     *
     * @param number
     * @param divisor
     * @return the remainder after a number is divided by a divisor. The result is a numeric value and has the same sign
     *         as the devisor.
     */
    public static DoubleValue mod(DoubleValue number, DoubleValue divisor) {
        if (number != null && divisor != null) {
            double result = MathUtils.mod(number.getValue(), divisor.getValue());
            return new DoubleValue(result, NumberOperations.MOD, number, divisor);
        }
        return null;
    }

    /**
     * Sorts the array <b>values</b> in ascending order and returns the value from array <b>values</b> at position
     * <b>position</b>
     *
     * @param values array of org.openl.meta.DoubleValue values
     * @param position int value
     * @return the value from array <b>values</b> at position <b>position</b>
     */
    public static DoubleValue small(DoubleValue[] values, int position) {
        return instance(MathUtils.small(unwrap(values), position), NumberOperations.SMALL, values);
    }

    /**
     * Sorts the array <b>values</b> in descending order and returns the value from array <b>values</b> at position
     * <b>position</b>
     *
     * @param values array of org.openl.meta.DoubleValue values
     * @param position int value
     * @return the value from array <b>values</b> at position <b>position</b>
     */
    public static DoubleValue big(DoubleValue[] values, int position) {
        return instance(MathUtils.big(unwrap(values), position), NumberOperations.BIG, values);
    }

    /**
     *
     * @param value1
     * @param value2
     * @return the result of value1 raised to the power of value2
     */
    public static DoubleValue pow(DoubleValue value1, DoubleValue value2) {
        // Commented to support operations with nulls
        // "null" means that data does not exist
        //
        // validate(value1, value2, NumberOperations.POW);
        if (value1 == null) {
            return value2 == null ? null : new DoubleValue(0);
        } else if (value2 == null) {
            return value1;
        }

        return new DoubleValue(Operators.pow(value1.getValue(), value2.getValue()),
            NumberOperations.POW,
            value1,
            value2);
    }

    /**
     *
     * @param value
     * @return the absolute value (module) of the value <b>value </b>
     */
    public static DoubleValue abs(DoubleValue value) {
        // Commented to support operations with nulls.
        // validate(value, NumberOperations.ABS);
        if (value == null) {
            return null;
        }
        // evaluate result
        double result = Math.abs(value.getValue());
        // create instance with information about last operation
        return new DoubleValue(result, NumberOperations.ABS, value);
    }

    /**
     *
     * @param value
     * @return the negative value of the <b>value</b>
     */
    public static DoubleValue negative(DoubleValue value) {
        if (value == null) {
            return null;
        }
        return multiply(value, MINUS_ONE);
    }

    /**
     *
     * @param value
     * @return the <b>value</b> increased by 1
     */
    public static DoubleValue inc(DoubleValue value) {
        return add(value, ONE);
    }

    /**
     *
     * @param value
     * @return the <b>value</b>
     */
    public static DoubleValue positive(DoubleValue value) {
        return value;
    }

    /**
     *
     * @param value
     * @return the <b>value </b> decreased by 1
     */
    public static DoubleValue dec(DoubleValue value) {
        return subtract(value, ONE);
    }

    // Autocasts

    /**
     * Is used to overload implicit cast operators from byte to org.openl.meta.DoubleValue
     *
     * @param x
     * @param y is needed to avoid ambiguity in Java method resolution
     * @return the casted value to org.openl.meta.DoubleValue
     */
    public static DoubleValue autocast(byte x, DoubleValue y) {
        return new DoubleValue(x);
    }

    /**
     * Is used to overload implicit cast operators from short to org.openl.meta.DoubleValue
     *
     * @param x
     * @param y is needed to avoid ambiguity in Java method resolution
     * @return the casted value to org.openl.meta.DoubleValue
     */
    public static DoubleValue autocast(short x, DoubleValue y) {
        return new DoubleValue(x);
    }

    /**
     * Is used to overload implicit cast operators from int to org.openl.meta.DoubleValue
     *
     * @param x
     * @param y is needed to avoid ambiguity in Java method resolution
     * @return the casted value to org.openl.meta.DoubleValue
     */
    public static DoubleValue autocast(int x, DoubleValue y) {
        return new DoubleValue(x);
    }

    /**
     * Is used to overload implicit cast operators from int to org.openl.meta.DoubleValue
     *
     * @param x
     * @param y is needed to avoid ambiguity in Java method resolution
     * @return the casted value to org.openl.meta.DoubleValue
     */
    public static DoubleValue autocast(char x, DoubleValue y) {
        return new DoubleValue(x);
    }

    /**
     * Is used to overload implicit cast operators from long to org.openl.meta.DoubleValue
     *
     * @param x
     * @param y is needed to avoid ambiguity in Java method resolution
     * @return the casted value to org.openl.meta.DoubleValue
     */
    public static DoubleValue autocast(long x, DoubleValue y) {
        return new DoubleValue(x);
    }

    /**
     * Is used to overload implicit cast operators from float to org.openl.meta.DoubleValue
     *
     * @param x
     * @param y is needed to avoid ambiguity in Java method resolution
     * @return the casted value to org.openl.meta.DoubleValue
     */
    public static DoubleValue autocast(float x, DoubleValue y) {
        return new DoubleValue(new BigDecimal(String.valueOf(x)).doubleValue());
    }

    /**
     * Is used to overload implicit cast operators from double to org.openl.meta.DoubleValue
     *
     * @param x
     * @param y is needed to avoid ambiguity in Java method resolution
     * @return the casted value to org.openl.meta.DoubleValue
     */
    public static DoubleValue autocast(double x, DoubleValue y) {
        return new DoubleValue(x);
    }

    // Constructors
    public DoubleValue(double value) {
        this.value = value;
    }

    /** Formula constructor **/
    public DoubleValue(DoubleValue lv1, DoubleValue lv2, double value, Formulas operand) {
        super(lv1, lv2, operand);
        this.value = value;
    }

    /** Cast constructor **/
    public DoubleValue(double value, ExplanationNumberValue<?> beforeCastValue, boolean autocast) {
        super(beforeCastValue, new CastOperand("DoubleValue", autocast));
        this.value = value;
    }

    /**
     * Copy the current value with new name <b>name</b>
     */
    @Override
    public DoubleValue copy(String name) {
        return copy(this, name);
    }

    /**
     * Prints the value of the current variable
     */
    @Override
    public String printValue() {
        return String.valueOf(value);
    }

    /**
     * Returns the value of the current variable
     */
    public double getValue() {
        return value;
    }

    // Equals
    @Override
    /**
     * Indicates whether some other object is "equal to" this org.openl.meta.DoubleValue variable.
     */
    public boolean equals(Object obj) {
        return obj instanceof DoubleValue && value == ((DoubleValue) obj).value;
    }

    public static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    // sort
    /**
     * Sorts the array <b>values</b>
     *
     * @param values an array for sorting
     * @return the sorted array
     */
    public static DoubleValue[] sort(DoubleValue[] values) {
        DoubleValue[] sortedArray = null;
        if (values != null) {
            sortedArray = new DoubleValue[values.length];
            DoubleValue[] notNullArray = ArrayTool.removeNulls(values);

            Arrays.sort(notNullArray);

            /* Filling sortedArray by sorted and null values */
            System.arraycopy(notNullArray, 0, sortedArray, 0, notNullArray.length);
        }
        return sortedArray;
    }

    // ******* Autocasts *************

    public static BigDecimalValue autocast(DoubleValue x, BigDecimalValue y) {
        if (x == null) {
            return null;
        }
        return new BigDecimalValue(String.valueOf(x.getValue()), x, true);
    }

    // ******* Casts *************

    public static DoubleValue cast(BigInteger x, DoubleValue y) {
        return new DoubleValue(x.doubleValue());
    }

    public static DoubleValue cast(BigDecimal x, DoubleValue y) {
        return new DoubleValue(x.doubleValue());
    }

    public static byte cast(DoubleValue x, byte y) {
        return x.byteValue();
    }

    public static short cast(DoubleValue x, short y) {
        return x.shortValue();
    }

    public static char cast(DoubleValue x, char y) {
        return (char) x.doubleValue();
    }

    public static int cast(DoubleValue x, int y) {
        return x.intValue();
    }

    public static long cast(DoubleValue x, long y) {
        return x.longValue();
    }

    public static float cast(DoubleValue x, float y) {
        return x.floatValue();
    }

    public static double cast(DoubleValue x, double y) {
        return x.doubleValue();
    }

    public static ByteValue cast(DoubleValue x, ByteValue y) {
        if (x == null) {
            return null;
        }
        return new ByteValue(x.byteValue(), x, false);
    }

    public static ShortValue cast(DoubleValue x, ShortValue y) {
        if (x == null) {
            return null;
        }
        return new ShortValue(x.shortValue(), x, false);
    }

    public static IntValue cast(DoubleValue x, IntValue y) {
        if (x == null) {
            return null;
        }
        return new IntValue(x.intValue(), x, false);
    }

    public static LongValue cast(DoubleValue x, LongValue y) {
        if (x == null) {
            return null;
        }
        return new LongValue(x.longValue(), x, false);
    }

    public static FloatValue cast(DoubleValue x, FloatValue y) {
        if (x == null) {
            return null;
        }
        return new FloatValue(x.floatValue(), x, false);
    }

    public static BigInteger cast(DoubleValue x, BigInteger y) {
        return BigDecimal.valueOf(x.doubleValue()).toBigInteger();
    }

    public static BigDecimal cast(DoubleValue x, BigDecimal y) {
        return BigDecimal.valueOf(x.doubleValue());
    }

    public static Integer round(DoubleValue value) {
        if (value == null) {
            return null;
        }

        return Round.round(value.value);
    }

    public static Integer round(DoubleValue value, RoundingMode roundingMode) {
        if (value == null) {
            return null;
        }
        return Round.round(value.value, roundingMode);
    }

    public static Double round(DoubleValue value, int scale) {
        if (value == null) {
            return null;
        }

        return Round.round(value.value, scale);
    }


    public static Double round(DoubleValue value, int scale, int roundingMode) {
        if (value == null) {
            return null;
        }

        return Round.round(value.value, scale, roundingMode);
    }

    public static Double round(DoubleValue value, int scale, RoundingMode roundingMode) {
        if (value == null) {
            return null;
        }

        return Round.round(value.value, scale, roundingMode);
    }

    public DoubleValue(String valueString) {
        super();
        this.value = Double.parseDouble(valueString);
    }

    /** Function constructor **/
    public DoubleValue(double result, NumberOperations function, DoubleValue... params) {
        super(function, params);
        this.value = result;
    }

    @Override
    public int compareTo(DoubleValue o) {
        return Double.compare(value, o.value);
    }

    @Override
    public double doubleValue() {
        return value;
    }

    @Override
    public float floatValue() {
        return (float) value;
    }

    @Override
    public int intValue() {
        return (int) value;
    }

    @Override
    public long longValue() {
        return (long) value;
    }

    @Deprecated
    public String getFormat() {
        // return format;
        return null;
    }

    @Deprecated
    public void setFormat(String format) {
        // this.format = format;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(value);
    }

    private static Double[] unwrap(DoubleValue[] values) {
        if (values == null) {
            return null;
        }
        values = ArrayTool.removeNulls(values);

        Double[] unwrappedArray = new Double[values.length];
        for (int i = 0; i < values.length; i++) {
            unwrappedArray[i] = values[i].getValue();
        }
        return unwrappedArray;
    }

}
