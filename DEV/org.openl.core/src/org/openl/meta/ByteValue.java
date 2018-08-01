package org.openl.meta;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.openl.binding.impl.Operators;
import org.openl.exception.OpenLRuntimeException;
import org.openl.meta.ByteValue.ByteValueAdapter;
import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.meta.number.CastOperand;
import org.openl.meta.number.Formulas;
import org.openl.meta.number.NumberOperations;
import org.openl.util.ArrayTool;
import org.openl.util.CollectionUtils;
import org.openl.util.math.MathUtils;

@XmlRootElement
@XmlJavaTypeAdapter(ByteValueAdapter.class)
public class ByteValue extends ExplanationNumberValue<ByteValue> {

    private static final long serialVersionUID = -3137978912171407672L;

    public static final ByteValue ZERO = new ByteValue((byte) 0);
    public static final ByteValue ONE = new ByteValue((byte) 1);
    public static final ByteValue MINUS_ONE = new ByteValue((byte) -1);

    public static class ByteValueAdapter extends XmlAdapter<Byte, ByteValue> {
        public ByteValue unmarshal(Byte val) throws Exception {
            return new ByteValue(val);
        }

        public Byte marshal(ByteValue val) throws Exception {
            return val.getValue();
        }
    }

    private final byte value;

    private static DoubleValue[] toDoubleValues(org.openl.meta.ByteValue[] values) {
        if (values == null) {
            return null;
        }
        DoubleValue[] doubleValues = new DoubleValue[values.length];
        int i = 0;
        for (ByteValue value : values) {
            doubleValues[i] = autocast(value, DoubleValue.ZERO);
            i++;
        }
        return doubleValues;
    }

    /**
     * average
     * 
     * @param values array of org.openl.meta.ByteValue values
     * @return the average value from the array
     */
    public static org.openl.meta.DoubleValue avg(org.openl.meta.ByteValue[] values) {
        if (CollectionUtils.isEmpty(values)) {
            return null;
        }
        Byte[] unwrappedArray = unwrap(values);
        Double avg = MathUtils.avg(unwrappedArray);

        return avg != null ? new org.openl.meta.DoubleValue(new org.openl.meta.DoubleValue(avg),
            NumberOperations.AVG,
            toDoubleValues(values)) : null;
    }

    /**
     * sum
     * 
     * @param values array of org.openl.meta.ByteValue values
     * @return the sum value from the array
     */
    public static org.openl.meta.ByteValue sum(org.openl.meta.ByteValue[] values) {
        if (CollectionUtils.isEmpty(values)) {
            return null;
        }
        Byte[] unwrappedArray = unwrap(values);
        Byte sum = MathUtils.sum(unwrappedArray);
        return sum != null ? new org.openl.meta.ByteValue(new org.openl.meta.ByteValue(sum),
            NumberOperations.SUM,
            values) : null;
    }

    /**
     * median
     * 
     * @param values array of org.openl.meta.ByteValue values
     * @return the median value from the array
     */
    public static org.openl.meta.DoubleValue median(org.openl.meta.ByteValue[] values) {
        if (CollectionUtils.isEmpty(values)) {
            return null;
        }
        Byte[] unwrappedArray = unwrap(values);
        Double median = MathUtils.median(unwrappedArray);
        return median != null ? new org.openl.meta.DoubleValue(new org.openl.meta.DoubleValue(median),
            NumberOperations.MEDIAN,
            toDoubleValues(values)) : null;
    }

    /**
     * Compares value1 and value2 and returns the max value
     * 
     * @param value1
     * @param value2
     * @return max value
     */
    public static org.openl.meta.ByteValue max(org.openl.meta.ByteValue value1, org.openl.meta.ByteValue value2) {
        // Commented to support operations with nulls
        // "null" means that data does not exist
        // validate(value1, value2, NumberOperations.MAX.toString());
        if (value1 == null)
            return value2;
        if (value2 == null)
            return value1;

        return new org.openl.meta.ByteValue(MathUtils.max(value1.getValue(), value2.getValue()) ? value1 : value2,
            NumberOperations.MAX,
            new org.openl.meta.ByteValue[] { value1, value2 });
    }

    /**
     * Compares value1 and value2 and returns the min value
     * 
     * @param value1
     * @param value2
     * @return min value
     */
    public static org.openl.meta.ByteValue min(org.openl.meta.ByteValue value1, org.openl.meta.ByteValue value2) {
        // Commented to support operations with nulls
        // "null" means that data does not exist
        // validate(value1, value2, NumberOperations.MIN.toString());
        if (value1 == null)
            return value2;
        if (value2 == null)
            return value1;

        return new org.openl.meta.ByteValue(MathUtils.min(value1.getValue(), value2.getValue()) ? value1 : value2,
            NumberOperations.MIN,
            new org.openl.meta.ByteValue[] { value1, value2 });
    }

    /**
     * 
     * @param values an array org.openl.meta.ByteValue, must not be null
     * @return org.openl.meta.ByteValue the max element from array
     */
    public static org.openl.meta.ByteValue max(org.openl.meta.ByteValue[] values) {
        org.openl.meta.ByteValue result = (org.openl.meta.ByteValue) MathUtils.max(values);
        if (result == null) {
            return null;
        }

        return new org.openl.meta.ByteValue((org.openl.meta.ByteValue) getAppropriateValue(values, result),
            NumberOperations.MAX_IN_ARRAY,
            values);
    }

    /**
     * 
     * @param values an array org.openl.meta.ByteValue, must not be null
     * @return org.openl.meta.ByteValue the min element from array
     */
    public static org.openl.meta.ByteValue min(org.openl.meta.ByteValue[] values) {
        org.openl.meta.ByteValue result = (org.openl.meta.ByteValue) MathUtils.min(values);
        if (result == null) {
            return null;
        }

        return new org.openl.meta.ByteValue((org.openl.meta.ByteValue) getAppropriateValue(values, result),
            NumberOperations.MIN_IN_ARRAY,
            values);
    }

    /**
     * 
     * @param value of variable which should be copied
     * @param name of new variable
     * @return the new org.openl.meta.ByteValue variable with name <b>name</b>
     *         and value <b>value</b>
     */
    public static org.openl.meta.ByteValue copy(org.openl.meta.ByteValue value, String name) {
        if (value.getName() == null) {
            value.setName(name);

            return value;
        } else if (!value.getName().equals(name)) {
            org.openl.meta.ByteValue result = new org.openl.meta.ByteValue(value,
                NumberOperations.COPY,
                new org.openl.meta.ByteValue[] { value });
            result.setName(name);

            return result;
        }
        return value;
    }

    // REM
    /**
     * Divides left hand operand by right hand operand and returns remainder
     * 
     * @param value1 org.openl.meta.ByteValue
     * @param value2 org.openl.meta.ByteValue
     * @return remainder from division value1 by value2
     */
    public static org.openl.meta.ByteValue rem(org.openl.meta.ByteValue value1, org.openl.meta.ByteValue value2) {
        // Commented to support operations with nulls. See also MathUtils.mod()
        // validate(value1, value2, Formulas.REM.toString());
        if (value1 == null || value2 == null) {
            return ZERO;
        }

        return new org.openl.meta.ByteValue(value1,
            value2,
            Operators.rem(value1.getValue(), value2.getValue()),
            Formulas.REM);
    }

    // ADD
    public static String add(ByteValue value1, String value2) {
        return value1 + value2;
    }

    public static String add(String value1, ByteValue value2) {
        return value1 + value2;
    }

    /**
     * Adds left hand operand to right hand operand
     * 
     * @param value1 org.openl.meta.ByteValue
     * @param value2 org.openl.meta.ByteValue
     * @return the result of addition operation
     */
    public static org.openl.meta.ByteValue add(org.openl.meta.ByteValue value1, org.openl.meta.ByteValue value2) {
        // temporary commented to support operations with nulls
        //
        // validate(value1, value2, Formulas.ADD.toString());
        // conditions for classes that are wrappers over primitives
        if (value1 == null) {
            return value2;
        }

        if (value2 == null) {
            return value1;
        }

        return new org.openl.meta.ByteValue(value1,
            value2,
            Operators.add(value1.getValue(), value2.getValue()),
            Formulas.ADD);
    }

    // MULTIPLY
    /**
     * Multiplies left hand operand to right hand operand
     * 
     * @param value1 org.openl.meta.ByteValue
     * @param value2 org.openl.meta.ByteValue
     * @return the result of multiplication operation
     */
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

    // SUBTRACT
    /**
     * Subtracts left hand operand to right hand operand
     * 
     * @param value1 org.openl.meta.ByteValue
     * @param value2 org.openl.meta.ByteValue
     * @return the result of subtraction operation
     */
    public static org.openl.meta.ByteValue subtract(org.openl.meta.ByteValue value1, org.openl.meta.ByteValue value2) {
        // temporary commented to support operations with nulls
        //
        // validate(value1, value2, Formulas.SUBTRACT.toString());
        if (value1 == null && value2 == null) {
            return null;
        }

        if (value1 == null) {
            return negative(value2);
        }

        if (value2 == null) {
            return value1;
        }

        return new org.openl.meta.ByteValue(value1,
            value2,
            Operators.subtract(value1.getValue(), value2.getValue()),
            Formulas.SUBTRACT);
    }

    // DIVIDE
    /**
     * Divides left hand operand by right hand operand
     * 
     * @param value1 org.openl.meta.ByteValue
     * @param value2 org.openl.meta.ByteValue
     * @return the result of division operation
     */
    public static org.openl.meta.DoubleValue divide(org.openl.meta.ByteValue value1, org.openl.meta.ByteValue value2) {
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
                    divide(ONE, value2).getValue(),
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

    // QUAOTIENT
    /**
     * Divides left hand operand by right hand operand
     * 
     * @param number org.openl.meta.ByteValue
     * @param divisor org.openl.meta.ByteValue
     * @return LongValue the result of division operation
     */
    public static LongValue quotient(org.openl.meta.ByteValue number, org.openl.meta.ByteValue divisor) {
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
     * @return the remainder after a number is divided by a divisor. The result
     *         is a numeric value and has the same sign as the devisor.
     */
    public static org.openl.meta.ByteValue mod(org.openl.meta.ByteValue number, org.openl.meta.ByteValue divisor) {
        if (number != null && divisor != null) {
            org.openl.meta.ByteValue result = new org.openl.meta.ByteValue(
                MathUtils.mod(number.getValue(), divisor.getValue()));
            return new org.openl.meta.ByteValue(result,
                NumberOperations.MOD,
                new org.openl.meta.ByteValue[] { number, divisor });
        }
        return null;
    }

    /**
     * Sorts the array <b>values</b> in ascending order and returns the value
     * from array <b>values</b> at position <b>position</b>
     * 
     * @param values array of org.openl.meta.ByteValue values
     * @param position int value
     * @return the value from array <b>values</b> at position <b>position</b>
     */
    public static org.openl.meta.ByteValue small(org.openl.meta.ByteValue[] values, int position) {
        if (values == null) {
            return null;
        }
        Byte[] unwrappedArray = unwrap(values);
        Byte small = MathUtils.small(unwrappedArray, position);
        return new org.openl.meta.ByteValue(
            (org.openl.meta.ByteValue) getAppropriateValue(values, new org.openl.meta.ByteValue(small)),
            NumberOperations.SMALL,
            values);
    }

    /**
     * Sorts the array <b>values</b> in descending order and returns the value
     * from array <b>values</b> at position <b>position</b>
     * 
     * @param values array of org.openl.meta.ByteValue values
     * @param position int value
     * @return the value from array <b>values</b> at position <b>position</b>
     */
    public static org.openl.meta.ByteValue big(org.openl.meta.ByteValue[] values, int position) {
        if (values == null) {
            return null;
        }
        Byte[] unwrappedArray = unwrap(values);
        Byte big = MathUtils.big(unwrappedArray, position);
        return new org.openl.meta.ByteValue(
            (org.openl.meta.ByteValue) getAppropriateValue(values, new org.openl.meta.ByteValue(big)),
            NumberOperations.BIG,
            values);
    }

    /**
     * 
     * @param value1
     * @param value2
     * @return the result of value1 raised to the power of value2
     */
    public static org.openl.meta.ByteValue pow(org.openl.meta.ByteValue value1, org.openl.meta.ByteValue value2) {
        // Commented to support operations with nulls
        // "null" means that data does not exist
        //
        // validate(value1, value2, NumberOperations.POW);
        if (value1 == null) {
            return value2 == null ? null : new org.openl.meta.ByteValue((byte) 0);
        } else if (value2 == null) {
            return value1;
        }

        return new org.openl.meta.ByteValue(
            new org.openl.meta.ByteValue(Operators.pow(value1.getValue(), value2.getValue())),
            NumberOperations.POW,
            new org.openl.meta.ByteValue[] { value1, value2 });
    }

    /**
     * 
     * @param value
     * @return the absolute value (module) of the value <b>value </b>
     */
    public static org.openl.meta.ByteValue abs(org.openl.meta.ByteValue value) {
        // Commented to support operations with nulls.
        // validate(value, NumberOperations.ABS);
        if (value == null) {
            return null;
        }
        // evaluate result
        org.openl.meta.ByteValue result = new org.openl.meta.ByteValue(Operators.abs(value.getValue()));
        // create instance with information about last operation
        return new org.openl.meta.ByteValue(result, NumberOperations.ABS, new org.openl.meta.ByteValue[] { value });
    }

    /**
     * 
     * @param value
     * @return the negative value of the <b>value</b>
     */
    public static org.openl.meta.ByteValue negative(org.openl.meta.ByteValue value) {
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
    public static org.openl.meta.ByteValue inc(org.openl.meta.ByteValue value) {
        return add(value, ONE);
    }

    /**
     * 
     * @param value
     * @return the <b>value</b>
     */
    public static org.openl.meta.ByteValue positive(org.openl.meta.ByteValue value) {
        return value;
    }

    /**
     * 
     * @param value
     * @return the <b>value </b> decreased by 1
     */
    public static org.openl.meta.ByteValue dec(org.openl.meta.ByteValue value) {
        return subtract(value, ONE);
    }

    // Autocasts

    /**
     * Is used to overload implicit cast operators from byte to
     * org.openl.meta.ByteValue
     * 
     * @param x
     * @param y is needed to avoid ambiguity in Java method resolution
     * @return the casted value to org.openl.meta.ByteValue
     */
    public static org.openl.meta.ByteValue autocast(byte x, org.openl.meta.ByteValue y) {
        return new org.openl.meta.ByteValue((byte) x);
    }

    // Constructors
    public ByteValue(byte value) {
        this.value = value;
    }

    /** Formula constructor **/
    public ByteValue(org.openl.meta.ByteValue lv1, org.openl.meta.ByteValue lv2, byte value, Formulas operand) {
        super(lv1, lv2, operand);
        this.value = value;
    }

    /** Cast constructor **/
    public ByteValue(byte value, ExplanationNumberValue<?> beforeCastValue, boolean autocast) {
        super(beforeCastValue, new CastOperand("ByteValue", autocast));
        this.value = value;
    }

    /**
     * Copy the current value with new name <b>name</b>
     */
    @Override
    public org.openl.meta.ByteValue copy(String name) {
        return copy(this, name);
    }

    /**
     * Prints the value of the current variable
     */
    public String printValue() {
        return String.valueOf(value);
    }

    /**
     * Returns the value of the current variable
     */
    public byte getValue() {
        return value;
    }

    // Equals
    @Override
    /**
     * Indicates whether some other object is "equal to" this
     * org.openl.meta.ByteValue variable.
     */
    public boolean equals(Object obj) {
        return obj instanceof ByteValue && value == ((ByteValue) obj).value;
    }

    // sort
    /**
     * Sorts the array <b>values</b>
     * 
     * @param values an array for sorting
     * @return the sorted array
     */
    public static org.openl.meta.ByteValue[] sort(org.openl.meta.ByteValue[] values) {
        org.openl.meta.ByteValue[] sortedArray = null;
        if (values != null) {
            sortedArray = new org.openl.meta.ByteValue[values.length];
            org.openl.meta.ByteValue[] notNullArray = ArrayTool.removeNulls(values);

            Arrays.sort(notNullArray);

            /* Filling sortedArray by sorted and null values */
            System.arraycopy(notNullArray, 0, sortedArray, 0, notNullArray.length);
        }
        return sortedArray;
    }

    // ******* Autocasts *************

    public static ShortValue autocast(ByteValue x, ShortValue y) {
        if (x == null) {
            return null;
        }

        return new ShortValue(x.getValue(), x, true);
    }

    public static IntValue autocast(ByteValue x, IntValue y) {
        if (x == null) {
            return null;
        }

        return new IntValue(x.getValue(), x, true);
    }

    public static LongValue autocast(ByteValue x, LongValue y) {
        if (x == null) {
            return null;
        }

        return new LongValue(x.getValue(), x, true);
    }

    public static FloatValue autocast(ByteValue x, FloatValue y) {
        if (x == null) {
            return null;
        }

        return new FloatValue(x.getValue(), x, true);
    }

    public static DoubleValue autocast(ByteValue x, DoubleValue y) {
        if (x == null) {
            return null;
        }

        return new DoubleValue(x.getValue(), x, true);
    }

    public static BigIntegerValue autocast(ByteValue x, BigIntegerValue y) {
        if (x == null) {
            return null;
        }
        return new BigIntegerValue(String.valueOf(x.getValue()), x, true);
    }

    public static BigDecimalValue autocast(ByteValue x, BigDecimalValue y) {
        if (x == null) {
            return null;
        }
        return new BigDecimalValue(String.valueOf(x.getValue()), x, true);
    }

    // ******* Casts *************

    public static ByteValue cast(short x, ByteValue y) {
        return new ByteValue((byte) x);
    }

    public static ByteValue cast(char x, ByteValue y) {
        return new ByteValue((byte) x);
    }

    public static ByteValue cast(int x, ByteValue y) {
        return new ByteValue((byte) x);
    }

    public static ByteValue cast(long x, ByteValue y) {
        return new ByteValue((byte) x);
    }

    public static ByteValue cast(float x, ByteValue y) {
        return new ByteValue((byte) x);
    }

    public static ByteValue cast(double x, ByteValue y) {
        return new ByteValue((byte) x);
    }

    public static ByteValue cast(BigInteger x, ByteValue y) {
        return new ByteValue(x.byteValue());
    }

    public static ByteValue cast(BigDecimal x, ByteValue y) {
        return new ByteValue(x.byteValue());
    }

    public static byte cast(ByteValue x, byte y) {
        return x.byteValue();
    }

    public static short cast(ByteValue x, short y) {
        return x.shortValue();
    }

    public static char cast(ByteValue x, char y) {
        return (char) x.byteValue();
    }

    public static int cast(ByteValue x, int y) {
        return x.intValue();
    }

    public static long cast(ByteValue x, long y) {
        return x.longValue();
    }

    public static float cast(ByteValue x, float y) {
        return x.floatValue();
    }

    public static double cast(ByteValue x, double y) {
        return x.doubleValue();
    }

    public static BigInteger cast(ByteValue x, BigInteger y) {
        return BigInteger.valueOf(x.byteValue());
    }

    public static BigDecimal cast(ByteValue x, BigDecimal y) {
        return BigDecimal.valueOf(x.byteValue());
    }

    public ByteValue(String valueString) {
        value = Byte.parseByte(valueString);
    }

    /** Function constructor **/
    public ByteValue(ByteValue result, NumberOperations function, ByteValue[] params) {
        super(function, params);
        this.value = result.byteValue();
    }

    @Override
    public double doubleValue() {
        return (double) value;
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

    public int compareTo(ByteValue o) {
        return value - o.value;
    }

    @Override
    public int hashCode() {
        return ((Byte) value).hashCode();
    }

    private static Byte[] unwrap(ByteValue[] values) {
        values = ArrayTool.removeNulls(values);

        Byte[] unwrappedArray = new Byte[values.length];
        for (int i = 0; i < values.length; i++) {
            if (values[i] != null) {
                unwrappedArray[i] = values[i].getValue();
            }
        }
        return unwrappedArray;
    }
}
