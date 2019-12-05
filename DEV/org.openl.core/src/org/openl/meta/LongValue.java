package org.openl.meta;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.openl.binding.impl.Operators;
import org.openl.exception.OpenLRuntimeException;
import org.openl.meta.LongValue.LongValueAdapter;
import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.meta.number.CastOperand;
import org.openl.meta.number.Formulas;
import org.openl.meta.number.NumberOperations;
import org.openl.rules.util.Avg;
import org.openl.rules.util.Product;
import org.openl.rules.util.Statistics;
import org.openl.rules.util.Sum;
import org.openl.util.ArrayTool;
import org.openl.util.math.MathUtils;

@XmlRootElement
@XmlJavaTypeAdapter(LongValueAdapter.class)
public class LongValue extends ExplanationNumberValue<LongValue> implements Comparable<LongValue> {

    private static final long serialVersionUID = -437788531108803012L;

    public static final LongValue ZERO = new LongValue(0);
    public static final LongValue ONE = new LongValue(1);
    public static final LongValue MINUS_ONE = new LongValue(-1);

    public static class LongValueAdapter extends XmlAdapter<Long, LongValue> {
        @Override
        public LongValue unmarshal(Long val) throws Exception {
            return new LongValue(val);
        }

        @Override
        public Long marshal(LongValue val) throws Exception {
            return val.getValue();
        }
    }

    private final long value;

    private static DoubleValue[] toDoubleValues(org.openl.meta.LongValue[] values) {
        if (values == null) {
            return null;
        }
        DoubleValue[] doubleValues = new DoubleValue[values.length];
        int i = 0;
        for (LongValue value : values) {
            doubleValues[i] = autocast(value, DoubleValue.ZERO);
            i++;
        }
        return doubleValues;
    }

    static LongValue instance(Long result, NumberOperations operation, LongValue... values) {
        return result == null ? null : new LongValue(new LongValue(result), operation, values);
    }

    private static LongValue instance(LongValue result, NumberOperations operation, LongValue... values) {
        return result == null ? null : new LongValue(result, operation, values);
    }

    public static LongValue max(LongValue... values) {
        return instance(Statistics.max(values), NumberOperations.MAX, values);
    }

    public static LongValue min(LongValue... values) {
        return instance(Statistics.min(values), NumberOperations.MIN, values);
    }

    public static LongValue sum(LongValue... values) {
        return instance(Sum.sum(unwrap(values)), NumberOperations.SUM, values);
    }

    public static DoubleValue avg(LongValue... values) {
        return DoubleValue.instance(Avg.avg(unwrap(values)), NumberOperations.AVG, toDoubleValues(values));
    }

    public static DoubleValue median(LongValue... values) {
        return DoubleValue.instance(MathUtils.median(unwrap(values)), NumberOperations.MEDIAN, toDoubleValues(values));
    }

    public static LongValue product(LongValue... values) {
        return instance(Product.product(unwrap(values)), NumberOperations.PRODUCT);
    }

    /**
     *
     * @param value of variable which should be copied
     * @param name of new variable
     * @return the new org.openl.meta.LongValue variable with name <b>name</b> and value <b>value</b>
     */
    public static org.openl.meta.LongValue copy(org.openl.meta.LongValue value, String name) {
        if (value.getName() == null) {
            value.setName(name);

            return value;
        } else if (!value.getName().equals(name)) {
            org.openl.meta.LongValue result = new org.openl.meta.LongValue(value,
                NumberOperations.COPY,
                new org.openl.meta.LongValue[] { value });
            result.setName(name);

            return result;
        }
        return value;
    }

    // REM
    /**
     * Divides left hand operand by right hand operand and returns remainder
     *
     * @param value1 org.openl.meta.LongValue
     * @param value2 org.openl.meta.LongValue
     * @return remainder from division value1 by value2
     */
    public static org.openl.meta.LongValue rem(org.openl.meta.LongValue value1, org.openl.meta.LongValue value2) {
        // Commented to support operations with nulls. See also MathUtils.mod()
        // validate(value1, value2, Formulas.REM.toString());
        if (value1 == null || value2 == null) {
            return ZERO;
        }

        return new org.openl.meta.LongValue(value1,
            value2,
            Operators.rem(value1.getValue(), value2.getValue()),
            Formulas.REM);
    }

    // ADD
    public static String add(LongValue value1, String value2) {
        return value1 + value2;
    }

    public static String add(String value1, LongValue value2) {
        return value1 + value2;
    }

    /**
     * Adds left hand operand to right hand operand
     *
     * @param value1 org.openl.meta.LongValue
     * @param value2 org.openl.meta.LongValue
     * @return the result of addition operation
     */
    public static org.openl.meta.LongValue add(org.openl.meta.LongValue value1, org.openl.meta.LongValue value2) {
        if (value1 == null) {
            return value2;
        }

        if (value2 == null) {
            return value1;
        }

        return new org.openl.meta.LongValue(value1,
            value2,
            Operators.add(value1.getValue(), value2.getValue()),
            Formulas.ADD);
    }

    // MULTIPLY
    /**
     * Multiplies left hand operand to right hand operand
     *
     * @param value1 org.openl.meta.LongValue
     * @param value2 org.openl.meta.LongValue
     * @return the result of multiplication operation
     */
    public static org.openl.meta.LongValue multiply(org.openl.meta.LongValue value1, org.openl.meta.LongValue value2) {
        if (value1 == null || value2 == null) {
            return null;
        }

        return new org.openl.meta.LongValue(value1,
            value2,
            Operators.multiply(value1.getValue(), value2.getValue()),
            Formulas.MULTIPLY);
    }

    // SUBTRACT
    /**
     * Subtracts left hand operand to right hand operand
     *
     * @param value1 org.openl.meta.LongValue
     * @param value2 org.openl.meta.LongValue
     * @return the result of subtraction operation
     */
    public static org.openl.meta.LongValue subtract(org.openl.meta.LongValue value1, org.openl.meta.LongValue value2) {
        if (value1 == null && value2 == null) {
            return null;
        }

        if (value1 == null) {
            return negative(value2);
        }

        if (value2 == null) {
            return value1;
        }

        return new org.openl.meta.LongValue(value1,
            value2,
            Operators.subtract(value1.getValue(), value2.getValue()),
            Formulas.SUBTRACT);
    }

    // DIVIDE
    /**
     * Divides left hand operand by right hand operand
     *
     * @param value1 org.openl.meta.LongValue
     * @param value2 org.openl.meta.LongValue
     * @return the result of division operation
     */
    public static org.openl.meta.DoubleValue divide(org.openl.meta.LongValue value1, org.openl.meta.LongValue value2) {
        if (value1 == null || value2 == null) {
            return null;
        }

        if (value2.doubleValue() == 0) {
            throw new OpenLRuntimeException("Division by zero");
        }

        return new org.openl.meta.DoubleValue(new DoubleValue(value1.doubleValue()),
            new DoubleValue(value2.doubleValue()),
            Operators.divide(value1.getValue(), value2.getValue()),
            Formulas.DIVIDE);

    }

    // QUAOTIENT
    /**
     * Divides left hand operand by right hand operand
     *
     * @param number org.openl.meta.LongValue
     * @param divisor org.openl.meta.LongValue
     * @return LongValue the result of division operation
     */
    public static LongValue quotient(org.openl.meta.LongValue number, org.openl.meta.LongValue divisor) {
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
    public static org.openl.meta.LongValue mod(org.openl.meta.LongValue number, org.openl.meta.LongValue divisor) {
        if (number != null && divisor != null) {
            org.openl.meta.LongValue result = new org.openl.meta.LongValue(
                MathUtils.mod(number.getValue(), divisor.getValue()));
            return new org.openl.meta.LongValue(result, NumberOperations.MOD, number, divisor);
        }
        return null;
    }

    /**
     * Sorts the array <b>values</b> in ascending order and returns the value from array <b>values</b> at position
     * <b>position</b>
     *
     * @param values array of org.openl.meta.LongValue values
     * @param position int value
     * @return the value from array <b>values</b> at position <b>position</b>
     */
    public static LongValue small(LongValue[] values, int position) {
        return instance(MathUtils.small(unwrap(values), position), NumberOperations.SMALL, values);
    }

    /**
     * Sorts the array <b>values</b> in descending order and returns the value from array <b>values</b> at position
     * <b>position</b>
     *
     * @param values array of org.openl.meta.LongValue values
     * @param position int value
     * @return the value from array <b>values</b> at position <b>position</b>
     */
    public static LongValue big(LongValue[] values, int position) {
        return instance(MathUtils.big(unwrap(values), position), NumberOperations.BIG, values);
    }

    /**
     *
     * @param value1
     * @param value2
     * @return the result of value1 raised to the power of value2
     */
    public static org.openl.meta.LongValue pow(org.openl.meta.LongValue value1, org.openl.meta.LongValue value2) {
        // Commented to support operations with nulls
        // "null" means that data does not exist
        //
        // validate(value1, value2, NumberOperations.POW);
        if (value1 == null) {
            return value2 == null ? null : new org.openl.meta.LongValue(0);
        } else if (value2 == null) {
            return value1;
        }

        return new org.openl.meta.LongValue(new org.openl.meta.LongValue(
            Operators.pow(value1.getValue(), value2.getValue())), NumberOperations.POW, value1, value2);
    }

    /**
     *
     * @param value
     * @return the absolute value (module) of the value <b>value </b>
     */
    public static org.openl.meta.LongValue abs(org.openl.meta.LongValue value) {
        // Commented to support operations with nulls.
        // validate(value, NumberOperations.ABS);
        if (value == null) {
            return null;
        }
        // evaluate result
        org.openl.meta.LongValue result = new org.openl.meta.LongValue(Math.abs(value.getValue()));
        // create instance with information about last operation
        return new org.openl.meta.LongValue(result, NumberOperations.ABS, value);
    }

    /**
     *
     * @param value
     * @return the negative value of the <b>value</b>
     */
    public static org.openl.meta.LongValue negative(org.openl.meta.LongValue value) {
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
    public static org.openl.meta.LongValue inc(org.openl.meta.LongValue value) {
        return add(value, ONE);
    }

    /**
     *
     * @param value
     * @return the <b>value</b>
     */
    public static org.openl.meta.LongValue positive(org.openl.meta.LongValue value) {
        return value;
    }

    /**
     *
     * @param value
     * @return the <b>value </b> decreased by 1
     */
    public static org.openl.meta.LongValue dec(org.openl.meta.LongValue value) {
        return subtract(value, ONE);
    }

    // Autocasts

    /**
     * Is used to overload implicit cast operators from byte to org.openl.meta.LongValue
     *
     * @param x
     * @param y is needed to avoid ambiguity in Java method resolution
     * @return the casted value to org.openl.meta.LongValue
     */
    public static org.openl.meta.LongValue autocast(byte x, org.openl.meta.LongValue y) {
        return new org.openl.meta.LongValue(x);
    }

    /**
     * Is used to overload implicit cast operators from short to org.openl.meta.LongValue
     *
     * @param x
     * @param y is needed to avoid ambiguity in Java method resolution
     * @return the casted value to org.openl.meta.LongValue
     */
    public static org.openl.meta.LongValue autocast(short x, org.openl.meta.LongValue y) {
        return new org.openl.meta.LongValue(x);
    }

    /**
     * Is used to overload implicit cast operators from int to org.openl.meta.LongValue
     *
     * @param x
     * @param y is needed to avoid ambiguity in Java method resolution
     * @return the casted value to org.openl.meta.LongValue
     */
    public static org.openl.meta.LongValue autocast(int x, org.openl.meta.LongValue y) {
        return new org.openl.meta.LongValue(x);
    }

    /**
     * Is used to overload implicit cast operators from long to org.openl.meta.LongValue
     *
     * @param x
     * @param y is needed to avoid ambiguity in Java method resolution
     * @return the casted value to org.openl.meta.LongValue
     */
    public static org.openl.meta.LongValue autocast(long x, org.openl.meta.LongValue y) {
        return new org.openl.meta.LongValue(x);
    }

    /**
     * Is used to overload implicit cast operators from char to org.openl.meta.LongValue
     *
     * @param x
     * @param y is needed to avoid ambiguity in Java method resolution
     * @return the casted value to org.openl.meta.LongValue
     */
    public static org.openl.meta.LongValue autocast(char x, org.openl.meta.LongValue y) {
        return new org.openl.meta.LongValue(x);
    }

    // Constructors
    public LongValue(long value) {
        this.value = value;
    }

    /** Formula constructor **/
    public LongValue(org.openl.meta.LongValue lv1, org.openl.meta.LongValue lv2, long value, Formulas operand) {
        super(lv1, lv2, operand);
        this.value = value;
    }

    /** Cast constructor **/
    public LongValue(long value, ExplanationNumberValue<?> beforeCastValue, boolean autocast) {
        super(beforeCastValue, new CastOperand("LongValue", autocast));
        this.value = value;
    }

    /**
     * Copy the current value with new name <b>name</b>
     */
    @Override
    public org.openl.meta.LongValue copy(String name) {
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
    public long getValue() {
        return value;
    }

    // Equals
    @Override
    /**
     * Indicates whether some other object is "equal to" this org.openl.meta.LongValue variable.
     */
    public boolean equals(Object obj) {
        return obj instanceof LongValue && value == ((LongValue) obj).value;
    }

    // sort
    /**
     * Sorts the array <b>values</b>
     *
     * @param values an array for sorting
     * @return the sorted array
     */
    public static org.openl.meta.LongValue[] sort(org.openl.meta.LongValue[] values) {
        org.openl.meta.LongValue[] sortedArray = null;
        if (values != null) {
            sortedArray = new org.openl.meta.LongValue[values.length];
            org.openl.meta.LongValue[] notNullArray = ArrayTool.removeNulls(values);

            Arrays.sort(notNullArray);

            /* Filling sortedArray by sorted and null values */
            System.arraycopy(notNullArray, 0, sortedArray, 0, notNullArray.length);
        }
        return sortedArray;
    }

    // ******* Autocasts*************

    public static FloatValue autocast(LongValue x, FloatValue y) {
        if (x == null) {
            return null;
        }

        return new FloatValue(x.getValue(), x, true);
    }

    public static DoubleValue autocast(LongValue x, DoubleValue y) {
        if (x == null) {
            return null;
        }

        return new DoubleValue(x.getValue(), x, true);
    }

    public static BigIntegerValue autocast(LongValue x, BigIntegerValue y) {
        if (x == null) {
            return null;
        }
        return new BigIntegerValue(String.valueOf(x.getValue()), x, true);
    }

    public static BigDecimalValue autocast(LongValue x, BigDecimalValue y) {
        if (x == null) {
            return null;
        }
        return new BigDecimalValue(String.valueOf(x.getValue()), x, true);
    }

    // ******* Casts *************

    public static LongValue cast(float x, LongValue y) {
        return new LongValue((long) x);
    }

    public static LongValue cast(double x, LongValue y) {
        return new LongValue((long) x);
    }

    public static LongValue cast(BigInteger x, LongValue y) {
        return new LongValue(x.longValue());
    }

    public static LongValue cast(BigDecimal x, LongValue y) {
        return new LongValue(x.longValue());
    }

    public static byte cast(LongValue x, byte y) {
        return x.byteValue();
    }

    public static short cast(LongValue x, short y) {
        return x.shortValue();
    }

    public static char cast(LongValue x, char y) {
        return (char) x.longValue();
    }

    public static int cast(LongValue x, int y) {
        return x.intValue();
    }

    public static long cast(LongValue x, long y) {
        return x.longValue();
    }

    public static float cast(LongValue x, float y) {
        return x.floatValue();
    }

    public static double cast(LongValue x, double y) {
        return x.doubleValue();
    }

    public static ByteValue cast(LongValue x, ByteValue y) {
        if (x == null) {
            return null;
        }
        return new ByteValue(x.byteValue(), x, false);
    }

    public static ShortValue cast(LongValue x, ShortValue y) {
        if (x == null) {
            return null;
        }
        return new ShortValue(x.shortValue(), x, false);
    }

    public static IntValue cast(LongValue x, IntValue y) {
        if (x == null) {
            return null;
        }
        return new IntValue(x.intValue(), x, false);
    }

    public static BigInteger cast(LongValue x, BigInteger y) {
        return BigInteger.valueOf(x.longValue());
    }

    public static BigDecimal cast(LongValue x, BigDecimal y) {
        return BigDecimal.valueOf(x.longValue());
    }

    public LongValue(String valueString) {
        value = Long.parseLong(valueString);
    }

    /** Function constructor **/
    public LongValue(LongValue result, NumberOperations function, LongValue... params) {
        super(function, params);
        this.value = result.longValue();
    }

    @Override
    public double doubleValue() {
        return value;
    }

    @Override
    public float floatValue() {
        return value;
    }

    @Override
    public int intValue() {
        return (int) value;
    }

    @Override
    public long longValue() {
        return value;
    }

    @Override
    public int compareTo(LongValue o) {
        return Long.compare(value, o.value);
    }

    @Override
    public int hashCode() {
        return Long.hashCode(value);
    }

    private static Long[] unwrap(LongValue[] values) {
        if (values == null) {
            return null;
        }
        values = ArrayTool.removeNulls(values);

        Long[] longArray = new Long[values.length];
        for (int i = 0; i < values.length; i++) {
            longArray[i] = values[i].getValue();
        }
        return longArray;

    }

}
