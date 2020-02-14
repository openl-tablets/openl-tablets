package org.openl.meta;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.openl.binding.impl.Operators;
import org.openl.exception.OpenLRuntimeException;
import org.openl.meta.IntValue.IntValueAdapter;
import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.meta.number.CastOperand;
import org.openl.meta.number.Formulas;
import org.openl.meta.number.NumberOperations;
import org.openl.rules.util.Avg;
import org.openl.rules.util.Statistics;
import org.openl.rules.util.Sum;
import org.openl.util.ArrayTool;
import org.openl.util.math.MathUtils;

@XmlRootElement
@XmlJavaTypeAdapter(IntValueAdapter.class)
public class IntValue extends ExplanationNumberValue<IntValue> implements Comparable<IntValue> {

    private static final long serialVersionUID = -3821702883606493390L;

    public static final IntValue ZERO = new IntValue(0);
    public static final IntValue ONE = new IntValue(1);
    public static final IntValue MINUS_ONE = new IntValue(-1);

    public static class IntValueAdapter extends XmlAdapter<Integer, IntValue> {
        @Override
        public IntValue unmarshal(Integer val) {
            if (val == null) {
                return null;
            }
            return new IntValue(val);
        }

        @Override
        public Integer marshal(IntValue val) {
            if (val == null) {
                return null;
            }
            return val.getValue();
        }
    }

    private final int value;

    private static DoubleValue[] toDoubleValues(org.openl.meta.IntValue[] values) {
        if (values == null) {
            return null;
        }
        DoubleValue[] doubleValues = new DoubleValue[values.length];
        int i = 0;
        for (IntValue value : values) {
            doubleValues[i] = autocast(value, DoubleValue.ZERO);
            i++;
        }
        return doubleValues;
    }

    private static IntValue instance(Integer result, NumberOperations operation, IntValue... values) {
        return result == null ? null : new IntValue(new IntValue(result), operation, values);
    }

    private static IntValue instance(IntValue result, NumberOperations operation, IntValue... values) {
        return result == null ? null : new IntValue(result, operation, values);
    }

    public static IntValue max(IntValue... values) {
        return instance(Statistics.max(values), NumberOperations.MAX, values);
    }

    public static IntValue min(IntValue... values) {
        return instance(Statistics.min(values), NumberOperations.MIN, values);
    }

    public static IntValue sum(IntValue... values) {
        return instance(Sum.sum(unwrap(values)), NumberOperations.SUM, values);
    }

    public static DoubleValue avg(IntValue... values) {
        return DoubleValue.instance(Avg.avg(unwrap(values)), NumberOperations.AVG, toDoubleValues(values));
    }

    public static DoubleValue median(IntValue... values) {
        return DoubleValue.instance(MathUtils.median(unwrap(values)), NumberOperations.MEDIAN, toDoubleValues(values));
    }

    /**
     *
     * @param value of variable which should be copied
     * @param name of new variable
     * @return the new org.openl.meta.IntValue variable with name <b>name</b> and value <b>value</b>
     */
    public static org.openl.meta.IntValue copy(org.openl.meta.IntValue value, String name) {
        if (value.getName() == null) {
            value.setName(name);

            return value;
        } else if (!value.getName().equals(name)) {
            org.openl.meta.IntValue result = new org.openl.meta.IntValue(value, NumberOperations.COPY, value);
            result.setName(name);

            return result;
        }
        return value;
    }

    // REM
    /**
     * Divides left hand operand by right hand operand and returns remainder
     *
     * @param value1 org.openl.meta.IntValue
     * @param value2 org.openl.meta.IntValue
     * @return remainder from division value1 by value2
     */
    public static org.openl.meta.IntValue rem(org.openl.meta.IntValue value1, org.openl.meta.IntValue value2) {
        // Commented to support operations with nulls. See also MathUtils.mod()
        // validate(value1, value2, Formulas.REM.toString());
        if (value1 == null || value2 == null) {
            return ZERO;
        }

        return new org.openl.meta.IntValue(value1,
            value2,
            Operators.rem(value1.getValue(), value2.getValue()),
            Formulas.REM);
    }

    /**
     * Adds left hand operand to right hand operand
     *
     * @param value1 org.openl.meta.IntValue
     * @param value2 org.openl.meta.IntValue
     * @return the result of addition operation
     */
    public static org.openl.meta.IntValue add(org.openl.meta.IntValue value1, org.openl.meta.IntValue value2) {
        if (value1 == null) {
            return value2;
        }

        if (value2 == null) {
            return value1;
        }

        return new org.openl.meta.IntValue(value1,
            value2,
            Operators.add(value1.getValue(), value2.getValue()),
            Formulas.ADD);
    }

    // MULTIPLY
    /**
     * Multiplies left hand operand to right hand operand
     *
     * @param value1 org.openl.meta.IntValue
     * @param value2 org.openl.meta.IntValue
     * @return the result of multiplication operation
     */
    public static org.openl.meta.IntValue multiply(org.openl.meta.IntValue value1, org.openl.meta.IntValue value2) {
        if (value1 == null || value2 == null) {
            return null;
        }

        return new org.openl.meta.IntValue(value1,
            value2,
            Operators.multiply(value1.getValue(), value2.getValue()),
            Formulas.MULTIPLY);
    }

    // SUBTRACT
    /**
     * Subtracts left hand operand to right hand operand
     *
     * @param value1 org.openl.meta.IntValue
     * @param value2 org.openl.meta.IntValue
     * @return the result of subtraction operation
     */
    public static org.openl.meta.IntValue subtract(org.openl.meta.IntValue value1, org.openl.meta.IntValue value2) {
        if (value1 == null) {
            return negative(value2);
        }

        if (value2 == null) {
            return value1;
        }

        return new org.openl.meta.IntValue(value1,
            value2,
            Operators.subtract(value1.getValue(), value2.getValue()),
            Formulas.SUBTRACT);
    }

    // DIVIDE
    /**
     * Divides left hand operand by right hand operand
     *
     * @param value1 org.openl.meta.IntValue
     * @param value2 org.openl.meta.IntValue
     * @return the result of division operation
     */
    public static org.openl.meta.DoubleValue divide(org.openl.meta.IntValue value1, org.openl.meta.IntValue value2) {
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
     * @param number org.openl.meta.IntValue
     * @param divisor org.openl.meta.IntValue
     * @return LongValue the result of division operation
     */
    public static LongValue quotient(org.openl.meta.IntValue number, org.openl.meta.IntValue divisor) {
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
    public static org.openl.meta.IntValue mod(org.openl.meta.IntValue number, org.openl.meta.IntValue divisor) {
        if (number != null && divisor != null) {
            org.openl.meta.IntValue result = new org.openl.meta.IntValue(
                MathUtils.mod(number.getValue(), divisor.getValue()));
            return new org.openl.meta.IntValue(result, NumberOperations.MOD, number, divisor);
        }
        return null;
    }

    /**
     * Sorts the array <b>values</b> in ascending order and returns the value from array <b>values</b> at position
     * <b>position</b>
     *
     * @param values array of org.openl.meta.IntValue values
     * @param position int value
     * @return the value from array <b>values</b> at position <b>position</b>
     */
    public static IntValue small(IntValue[] values, int position) {
        return instance(MathUtils.small(unwrap(values), position), NumberOperations.SMALL, values);
    }

    /**
     * Sorts the array <b>values</b> in descending order and returns the value from array <b>values</b> at position
     * <b>position</b>
     *
     * @param values array of org.openl.meta.IntValue values
     * @param position int value
     * @return the value from array <b>values</b> at position <b>position</b>
     */
    public static IntValue big(IntValue[] values, int position) {
        return instance(MathUtils.big(unwrap(values), position), NumberOperations.BIG, values);
    }

    /**
     *
     * @param value1
     * @param value2
     * @return the result of value1 raised to the power of value2
     */
    public static org.openl.meta.IntValue pow(org.openl.meta.IntValue value1, org.openl.meta.IntValue value2) {
        // Commented to support operations with nulls
        // "null" means that data does not exist
        //
        // validate(value1, value2, NumberOperations.POW);
        if (value1 == null) {
            return value2 == null ? null : new org.openl.meta.IntValue(0);
        } else if (value2 == null) {
            return value1;
        }

        return new org.openl.meta.IntValue(new org.openl.meta.IntValue(
            Operators.pow(value1.getValue(), value2.getValue())), NumberOperations.POW, value1, value2);
    }

    /**
     *
     * @param value
     * @return the absolute value (module) of the value <b>value </b>
     */
    public static org.openl.meta.IntValue abs(org.openl.meta.IntValue value) {
        // Commented to support operations with nulls.
        // validate(value, NumberOperations.ABS);
        if (value == null) {
            return null;
        }
        // evaluate result
        org.openl.meta.IntValue result = new org.openl.meta.IntValue(Math.abs(value.getValue()));
        // create instance with information about last operation
        return new org.openl.meta.IntValue(result, NumberOperations.ABS, value);
    }

    /**
     *
     * @param value
     * @return the negative value of the <b>value</b>
     */
    public static org.openl.meta.IntValue negative(org.openl.meta.IntValue value) {
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
    public static org.openl.meta.IntValue inc(org.openl.meta.IntValue value) {
        return add(value, ONE);
    }

    /**
     *
     * @param value
     * @return the <b>value</b>
     */
    public static org.openl.meta.IntValue positive(org.openl.meta.IntValue value) {
        return value;
    }

    /**
     *
     * @param value
     * @return the <b>value </b> decreased by 1
     */
    public static org.openl.meta.IntValue dec(org.openl.meta.IntValue value) {
        return subtract(value, ONE);
    }

    // Autocasts

    /**
     * Is used to overload implicit cast operators from byte to org.openl.meta.IntValue
     *
     * @param x
     * @param y is needed to avoid ambiguity in Java method resolution
     * @return the casted value to org.openl.meta.IntValue
     */
    public static org.openl.meta.IntValue autocast(byte x, org.openl.meta.IntValue y) {
        return new org.openl.meta.IntValue(x);
    }

    /**
     * Is used to overload implicit cast operators from short to org.openl.meta.IntValue
     *
     * @param x
     * @param y is needed to avoid ambiguity in Java method resolution
     * @return the casted value to org.openl.meta.IntValue
     */
    public static org.openl.meta.IntValue autocast(short x, org.openl.meta.IntValue y) {
        return new org.openl.meta.IntValue(x);
    }

    /**
     * Is used to overload implicit cast operators from int to org.openl.meta.IntValue
     *
     * @param x
     * @param y is needed to avoid ambiguity in Java method resolution
     * @return the casted value to org.openl.meta.IntValue
     */
    public static org.openl.meta.IntValue autocast(int x, org.openl.meta.IntValue y) {
        return new org.openl.meta.IntValue(x);
    }

    /**
     * Is used to overload implicit cast operators from char to org.openl.meta.IntValue
     *
     * @param x
     * @param y is needed to avoid ambiguity in Java method resolution
     * @return the casted value to org.openl.meta.IntValue
     */
    public static org.openl.meta.IntValue autocast(char x, org.openl.meta.IntValue y) {
        return new org.openl.meta.IntValue(x);
    }

    // Constructors
    public IntValue(int value) {
        this.value = value;
    }

    /** Formula constructor **/
    public IntValue(org.openl.meta.IntValue lv1, org.openl.meta.IntValue lv2, int value, Formulas operand) {
        super(lv1, lv2, operand);
        this.value = value;
    }

    /** Cast constructor **/
    public IntValue(int value, ExplanationNumberValue<?> beforeCastValue, boolean autocast) {
        super(beforeCastValue, new CastOperand("IntValue", autocast));
        this.value = value;
    }

    /**
     * Copy the current value with new name <b>name</b>
     */
    @Override
    public org.openl.meta.IntValue copy(String name) {
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
    public int getValue() {
        return value;
    }

    // Equals
    @Override
    /**
     * Indicates whether some other object is "equal to" this org.openl.meta.IntValue variable.
     */
    public boolean equals(Object obj) {
        return obj instanceof IntValue && value == ((IntValue) obj).value;
    }

    // sort
    /**
     * Sorts the array <b>values</b>
     *
     * @param values an array for sorting
     * @return the sorted array
     */
    public static org.openl.meta.IntValue[] sort(org.openl.meta.IntValue[] values) {
        org.openl.meta.IntValue[] sortedArray = null;
        if (values != null) {
            sortedArray = new org.openl.meta.IntValue[values.length];
            org.openl.meta.IntValue[] notNullArray = ArrayTool.removeNulls(values);

            Arrays.sort(notNullArray);

            /* Filling sortedArray by sorted and null values */
            System.arraycopy(notNullArray, 0, sortedArray, 0, notNullArray.length);
        }
        return sortedArray;
    }

    // ******* Autocasts*************

    public static LongValue autocast(IntValue x, LongValue y) {
        if (x == null) {
            return null;
        }

        return new LongValue(x.getValue(), x, true);
    }

    public static FloatValue autocast(IntValue x, FloatValue y) {
        if (x == null) {
            return null;
        }

        return new FloatValue(x.getValue(), x, true);
    }

    public static DoubleValue autocast(IntValue x, DoubleValue y) {
        if (x == null) {
            return null;
        }

        return new DoubleValue(x.getValue(), x, true);
    }

    public static BigIntegerValue autocast(IntValue x, BigIntegerValue y) {
        if (x == null) {
            return null;
        }
        return new BigIntegerValue(String.valueOf(x.getValue()), x, true);
    }

    public static BigDecimalValue autocast(IntValue x, BigDecimalValue y) {
        if (x == null) {
            return null;
        }
        return new BigDecimalValue(String.valueOf(x.getValue()), x, true);
    }

    // ******* Casts*************

    public static IntValue cast(long x, IntValue y) {
        return new IntValue((int) x);
    }

    public static IntValue cast(float x, IntValue y) {
        return new IntValue((int) x);
    }

    public static IntValue cast(double x, IntValue y) {
        return new IntValue((int) x);
    }

    public static IntValue cast(BigInteger x, IntValue y) {
        return new IntValue(x.intValue());
    }

    public static IntValue cast(BigDecimal x, IntValue y) {
        return new IntValue(x.intValue());
    }

    public static byte cast(IntValue x, byte y) {
        return x.byteValue();
    }

    public static short cast(IntValue x, short y) {
        return x.shortValue();
    }

    public static char cast(IntValue x, char y) {
        return (char) x.intValue();
    }

    public static int cast(IntValue x, int y) {
        return x.intValue();
    }

    public static long cast(IntValue x, long y) {
        return x.longValue();
    }

    public static float cast(IntValue x, float y) {
        return x.floatValue();
    }

    public static double cast(IntValue x, double y) {
        return x.doubleValue();
    }

    public static ByteValue cast(IntValue x, ByteValue y) {
        if (x == null) {
            return null;
        }
        return new ByteValue(x.byteValue(), x, false);
    }

    public static ShortValue cast(IntValue x, ShortValue y) {
        if (x == null) {
            return null;
        }
        return new ShortValue(x.shortValue(), x, false);
    }

    public static BigInteger cast(IntValue x, BigInteger y) {
        return BigInteger.valueOf(x.intValue());
    }

    public static BigDecimal cast(IntValue x, BigDecimal y) {
        return BigDecimal.valueOf(x.intValue());
    }

    public IntValue(String valueString) {
        value = Integer.parseInt(valueString);
    }

    /** Function constructor **/
    public IntValue(IntValue result, NumberOperations function, IntValue... params) {
        super(function, params);
        this.value = result.intValue();
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
        return value;
    }

    @Override
    public long longValue() {
        return value;
    }

    @Override
    public int compareTo(IntValue o) {
        return Integer.compare(value, o.value);
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(value);
    }

    private static Integer[] unwrap(IntValue[] values) {
        if (values == null) {
            return null;
        }
        values = ArrayTool.removeNulls(values);

        Integer[] intArray = new Integer[values.length];
        for (int i = 0; i < values.length; i++) {
            intArray[i] = values[i].getValue();
        }
        return intArray;
    }
}
