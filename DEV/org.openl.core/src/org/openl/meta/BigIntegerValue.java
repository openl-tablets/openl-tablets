package org.openl.meta;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.openl.binding.impl.Operators;
import org.openl.exception.OpenLRuntimeException;
import org.openl.meta.BigIntegerValue.BigIntegerValueAdapter;
import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.meta.number.CastOperand;
import org.openl.meta.number.Formulas;
import org.openl.meta.number.NumberOperations;
import org.openl.rules.util.Statistics;
import org.openl.util.ArrayTool;
import org.openl.util.CollectionUtils;
import org.openl.util.math.MathUtils;

@XmlRootElement
@XmlJavaTypeAdapter(BigIntegerValueAdapter.class)
public class BigIntegerValue extends ExplanationNumberValue<BigIntegerValue> {

    private static final long serialVersionUID = -3936317402079096501L;

    public static final BigIntegerValue ZERO = new BigIntegerValue("0");
    public static final BigIntegerValue ONE = new BigIntegerValue("1");
    public static final BigIntegerValue MINUS_ONE = new BigIntegerValue("-1");

    private final java.math.BigInteger value;

    public static class BigIntegerValueAdapter extends XmlAdapter<BigInteger, BigIntegerValue> {
        public BigIntegerValue unmarshal(BigInteger val) throws Exception {
            return new BigIntegerValue(val);
        }

        public BigInteger marshal(BigIntegerValue val) throws Exception {
            return val.getValue();
        }
    }

    private static BigDecimalValue[] toBigDecimalValueValues(org.openl.meta.BigIntegerValue[] values) {
        if (values == null) {
            return null;
        }
        BigDecimalValue[] doubleValues = new BigDecimalValue[values.length];
        int i = 0;
        for (BigIntegerValue value : values) {
            doubleValues[i] = BigDecimalValue.autocast(value, new BigDecimalValue("0"));
            i++;
        }
        return doubleValues;
    }

    public static BigIntegerValue max(BigIntegerValue... values) {
        BigIntegerValue result = Statistics.max(values);
        return result == null ? null : new BigIntegerValue(result, NumberOperations.MAX, values);
    }

    public static BigIntegerValue min(BigIntegerValue... values) {
        BigIntegerValue result = Statistics.min(values);
        return result == null ? null : new BigIntegerValue(result, NumberOperations.MIN, values);
    }

    /**
     * average
     * 
     * @param values array of org.openl.meta.BigIntegerValue values
     * @return the average value from the array
     */
    public static org.openl.meta.BigDecimalValue avg(org.openl.meta.BigIntegerValue[] values) {
        if (CollectionUtils.isEmpty(values)) {
            return null;
        }
        java.math.BigInteger[] unwrappedArray = unwrap(values);
        java.math.BigDecimal avg = MathUtils.avg(unwrappedArray);
        return avg != null ? new org.openl.meta.BigDecimalValue(new org.openl.meta.BigDecimalValue(avg),
            NumberOperations.AVG,
            toBigDecimalValueValues(values)) : null;
    }

    /**
     * sum
     * 
     * @param values array of org.openl.meta.BigIntegerValue values
     * @return the sum value from the array
     */
    public static org.openl.meta.BigIntegerValue sum(org.openl.meta.BigIntegerValue[] values) {
        if (CollectionUtils.isEmpty(values)) {
            return null;
        }
        java.math.BigInteger[] unwrappedArray = unwrap(values);
        java.math.BigInteger sum = MathUtils.sum(unwrappedArray);
        return sum != null ? new org.openl.meta.BigIntegerValue(new org.openl.meta.BigIntegerValue(sum),
            NumberOperations.SUM,
            values) : null;
    }

    /**
     * median
     * 
     * @param values array of org.openl.meta.BigIntegerValue values
     * @return the median value from the array
     */
    public static org.openl.meta.BigDecimalValue median(org.openl.meta.BigIntegerValue[] values) {
        if (CollectionUtils.isEmpty(values)) {
            return null;
        }
        java.math.BigInteger[] unwrappedArray = unwrap(values);
        java.math.BigDecimal median = MathUtils.median(unwrappedArray);
        BigDecimalValue[] values1 = new BigDecimalValue[values.length];
        int i = 0;
        for (BigIntegerValue v : values) {
            values1[i++] = new BigDecimalValue(new BigDecimal(v.getValue()));
        }
        return median != null ? new org.openl.meta.BigDecimalValue(new org.openl.meta.BigDecimalValue(median),
            NumberOperations.MEDIAN,
            values1) : null;
    }

    /**
     * 
     * @param value of variable which should be copied
     * @param name of new variable
     * @return the new org.openl.meta.BigIntegerValue variable with name
     *         <b>name</b> and value <b>value</b>
     */
    public static org.openl.meta.BigIntegerValue copy(org.openl.meta.BigIntegerValue value, String name) {
        if (value.getName() == null) {
            value.setName(name);

            return value;
        } else if (!value.getName().equals(name)) {
            org.openl.meta.BigIntegerValue result = new org.openl.meta.BigIntegerValue(value,
                NumberOperations.COPY,
                new org.openl.meta.BigIntegerValue[] { value });
            result.setName(name);

            return result;
        }
        return value;
    }

    // REM
    /**
     * Divides left hand operand by right hand operand and returns remainder
     * 
     * @param value1 org.openl.meta.BigIntegerValue
     * @param value2 org.openl.meta.BigIntegerValue
     * @return remainder from division value1 by value2
     */
    public static org.openl.meta.BigIntegerValue rem(org.openl.meta.BigIntegerValue value1,
            org.openl.meta.BigIntegerValue value2) {
        // Commented to support operations with nulls. See also MathUtils.mod()
        // validate(value1, value2, Formulas.REM.toString());
        if (value1 == null || value2 == null) {
            return ZERO;
        }

        return new org.openl.meta.BigIntegerValue(value1,
            value2,
            Operators.rem(value1.getValue(), value2.getValue()),
            Formulas.REM);
    }

    // ADD
    public static String add(BigIntegerValue value1, String value2) {
        return value1 + value2;
    }

    public static String add(String value1, BigIntegerValue value2) {
        return value1 + value2;
    }

    /**
     * Adds left hand operand to right hand operand
     * 
     * @param value1 org.openl.meta.BigIntegerValue
     * @param value2 org.openl.meta.BigIntegerValue
     * @return the result of addition operation
     */
    public static org.openl.meta.BigIntegerValue add(org.openl.meta.BigIntegerValue value1,
            org.openl.meta.BigIntegerValue value2) {
        // temporary commented to support operations with nulls
        //
        // validate(value1, value2, Formulas.ADD.toString());
        // conditions big types
        if (value1 == null) {
            return value2;
        }

        if (value2 == null) {
            return value1;
        }

        return new org.openl.meta.BigIntegerValue(value1,
            value2,
            Operators.add(value1.getValue(), value2.getValue()),
            Formulas.ADD);
    }

    // MULTIPLY
    /**
     * Multiplies left hand operand to right hand operand
     * 
     * @param value1 org.openl.meta.BigIntegerValue
     * @param value2 org.openl.meta.BigIntegerValue
     * @return the result of multiplication operation
     */
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

    // SUBTRACT
    /**
     * Subtracts left hand operand to right hand operand
     * 
     * @param value1 org.openl.meta.BigIntegerValue
     * @param value2 org.openl.meta.BigIntegerValue
     * @return the result of subtraction operation
     */
    public static org.openl.meta.BigIntegerValue subtract(org.openl.meta.BigIntegerValue value1,
            org.openl.meta.BigIntegerValue value2) {
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

        return new org.openl.meta.BigIntegerValue(value1,
            value2,
            Operators.subtract(value1.getValue(), value2.getValue()),
            Formulas.SUBTRACT);
    }

    // DIVIDE
    /**
     * Divides left hand operand by right hand operand
     * 
     * @param value1 org.openl.meta.BigIntegerValue
     * @param value2 org.openl.meta.BigIntegerValue
     * @return the result of division operation
     */
    public static org.openl.meta.BigDecimalValue divide(org.openl.meta.BigIntegerValue value1,
            org.openl.meta.BigIntegerValue value2) {
        // temporary commented to support operations with nulls
        //
        // validate(value1, value2, Formulas.DIVIDE.toString());
        if (value1 == null && value2 == null) {
            return null;
        }

        if (value1 == null) {
            if (value2 != null && value2.doubleValue() != 0) {
                return new org.openl.meta.BigDecimalValue(null,
                    new BigDecimalValue(new BigDecimal(value2.getValue())),
                    divide(ONE, value2).getValue(),
                    Formulas.DIVIDE);
            }
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

    // QUAOTIENT
    /**
     * Divides left hand operand by right hand operand
     * 
     * @param number org.openl.meta.BigIntegerValue
     * @param divisor org.openl.meta.BigIntegerValue
     * @return LongValue the result of division operation
     */
    public static LongValue quotient(org.openl.meta.BigIntegerValue number, org.openl.meta.BigIntegerValue divisor) {
        if (number != null && divisor != null) {
            LongValue result = new LongValue(MathUtils.quotient(number.getValue(), divisor.getValue()));
            return new LongValue(result, NumberOperations.QUOTIENT, null);
        }
        return null;
    }

    // generated product function for big types
    /**
     * Multiplies the numbers from the provided array and returns the product as
     * a number.
     * 
     * @param values an array of IntValue which will be converted to DoubleValue
     * @return the product as a number
     */
    public static org.openl.meta.BigIntegerValue product(org.openl.meta.BigIntegerValue[] values) {
        if (CollectionUtils.isEmpty(values)) {
            return null;
        }
        java.math.BigInteger[] unwrappedArray = unwrap(values);
        java.math.BigInteger product = MathUtils.product(unwrappedArray);
        // we loose the parameters, but not the result of computation.
        return product != null ? new org.openl.meta.BigIntegerValue(new org.openl.meta.BigIntegerValue(product),
            NumberOperations.PRODUCT,
            null) : null;
    }

    /**
     * 
     * @param number
     * @param divisor
     * @return the remainder after a number is divided by a divisor. The result
     *         is a numeric value and has the same sign as the devisor.
     */
    public static org.openl.meta.BigIntegerValue mod(org.openl.meta.BigIntegerValue number,
            org.openl.meta.BigIntegerValue divisor) {
        if (number != null && divisor != null) {
            org.openl.meta.BigIntegerValue result = new org.openl.meta.BigIntegerValue(
                MathUtils.mod(number.getValue(), divisor.getValue()));
            return new org.openl.meta.BigIntegerValue(result,
                NumberOperations.MOD,
                new org.openl.meta.BigIntegerValue[] { number, divisor });
        }
        return null;
    }

    /**
     * Sorts the array <b>values</b> in ascending order and returns the value
     * from array <b>values</b> at position <b>position</b>
     * 
     * @param values array of org.openl.meta.BigIntegerValue values
     * @param position int value
     * @return the value from array <b>values</b> at position <b>position</b>
     */
    public static org.openl.meta.BigIntegerValue small(org.openl.meta.BigIntegerValue[] values, int position) {
        if (values == null) {
            return null;
        }
        java.math.BigInteger[] unwrappedArray = unwrap(values);
        java.math.BigInteger small = MathUtils.small(unwrappedArray, position);
        return new org.openl.meta.BigIntegerValue(
            (org.openl.meta.BigIntegerValue) getAppropriateValue(values, new org.openl.meta.BigIntegerValue(small)),
            NumberOperations.SMALL,
            values);
    }

    /**
     * Sorts the array <b>values</b> in descending order and returns the value
     * from array <b>values</b> at position <b>position</b>
     * 
     * @param values array of org.openl.meta.BigIntegerValue values
     * @param position int value
     * @return the value from array <b>values</b> at position <b>position</b>
     */
    public static org.openl.meta.BigIntegerValue big(org.openl.meta.BigIntegerValue[] values, int position) {
        if (values == null) {
            return null;
        }
        java.math.BigInteger[] unwrappedArray = unwrap(values);
        java.math.BigInteger big = MathUtils.big(unwrappedArray, position);
        return new org.openl.meta.BigIntegerValue(
            (org.openl.meta.BigIntegerValue) getAppropriateValue(values, new org.openl.meta.BigIntegerValue(big)),
            NumberOperations.BIG,
            values);
    }

    /**
     * 
     * @param value1
     * @param value2
     * @return the result of value1 raised to the power of value2
     */
    public static org.openl.meta.BigIntegerValue pow(org.openl.meta.BigIntegerValue value1,
            org.openl.meta.BigIntegerValue value2) {
        // Commented to support operations with nulls
        // "null" means that data does not exist
        //
        // validate(value1, value2, NumberOperations.POW);
        if (value1 == null) {
            return value2 == null ? null : new org.openl.meta.BigIntegerValue("0");
        } else if (value2 == null) {
            return value1;
        }

        return new org.openl.meta.BigIntegerValue(
            new org.openl.meta.BigIntegerValue(Operators.pow(value1.getValue(), value2.getValue())),
            NumberOperations.POW,
            new org.openl.meta.BigIntegerValue[] { value1, value2 });
    }

    /**
     * 
     * @param value
     * @return the absolute value (module) of the value <b>value </b>
     */
    public static org.openl.meta.BigIntegerValue abs(org.openl.meta.BigIntegerValue value) {
        // Commented to support operations with nulls.
        // validate(value, NumberOperations.ABS);
        if (value == null) {
            return null;
        }
        // evaluate result
        org.openl.meta.BigIntegerValue result = new org.openl.meta.BigIntegerValue(Operators.abs(value.getValue()));
        // create instance with information about last operation
        return new org.openl.meta.BigIntegerValue(result,
            NumberOperations.ABS,
            new org.openl.meta.BigIntegerValue[] { value });
    }

    /**
     * 
     * @param value
     * @return the negative value of the <b>value</b>
     */
    public static org.openl.meta.BigIntegerValue negative(org.openl.meta.BigIntegerValue value) {
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
    public static org.openl.meta.BigIntegerValue inc(org.openl.meta.BigIntegerValue value) {
        return add(value, ONE);
    }

    /**
     * 
     * @param value
     * @return the <b>value</b>
     */
    public static org.openl.meta.BigIntegerValue positive(org.openl.meta.BigIntegerValue value) {
        return value;
    }

    /**
     * 
     * @param value
     * @return the <b>value </b> decreased by 1
     */
    public static org.openl.meta.BigIntegerValue dec(org.openl.meta.BigIntegerValue value) {
        return subtract(value, ONE);
    }

    // Autocasts

    /**
     * Is used to overload implicit cast operators from byte to
     * org.openl.meta.BigIntegerValue
     * 
     * @param x
     * @param y is needed to avoid ambiguity in Java method resolution
     * @return the casted value to org.openl.meta.BigIntegerValue
     */
    public static org.openl.meta.BigIntegerValue autocast(byte x, org.openl.meta.BigIntegerValue y) {
        return new org.openl.meta.BigIntegerValue(String.valueOf(x));
    }

    /**
     * Is used to overload implicit cast operators from short to
     * org.openl.meta.BigIntegerValue
     * 
     * @param x
     * @param y is needed to avoid ambiguity in Java method resolution
     * @return the casted value to org.openl.meta.BigIntegerValue
     */
    public static org.openl.meta.BigIntegerValue autocast(short x, org.openl.meta.BigIntegerValue y) {
        return new org.openl.meta.BigIntegerValue(String.valueOf(x));
    }

    /**
     * Is used to overload implicit cast operators from int to
     * org.openl.meta.BigIntegerValue
     * 
     * @param x
     * @param y is needed to avoid ambiguity in Java method resolution
     * @return the casted value to org.openl.meta.BigIntegerValue
     */
    public static org.openl.meta.BigIntegerValue autocast(int x, org.openl.meta.BigIntegerValue y) {
        return new org.openl.meta.BigIntegerValue(String.valueOf(x));
    }

    /**
     * Is used to overload implicit cast operators from char to
     * org.openl.meta.BigIntegerValue
     * 
     * @param x
     * @param y is needed to avoid ambiguity in Java method resolution
     * @return the casted value to org.openl.meta.BigIntegerValue
     */
    public static org.openl.meta.BigIntegerValue autocast(char x, org.openl.meta.BigIntegerValue y) {
        return new org.openl.meta.BigIntegerValue(String.valueOf((int) x));
    }

    /**
     * Is used to overload implicit cast operators from long to
     * org.openl.meta.BigIntegerValue
     * 
     * @param x
     * @param y is needed to avoid ambiguity in Java method resolution
     * @return the casted value to org.openl.meta.BigIntegerValue
     */
    public static org.openl.meta.BigIntegerValue autocast(long x, org.openl.meta.BigIntegerValue y) {
        return new org.openl.meta.BigIntegerValue(String.valueOf(x));
    }

    // Constructors
    public BigIntegerValue(java.math.BigInteger value) {
        this.value = value;
    }

    /** Formula constructor **/
    public BigIntegerValue(org.openl.meta.BigIntegerValue lv1,
            org.openl.meta.BigIntegerValue lv2,
            java.math.BigInteger value,
            Formulas operand) {
        super(lv1, lv2, operand);
        this.value = value;
    }

    /** Cast constructor **/
    public BigIntegerValue(String valueString, ExplanationNumberValue<?> beforeCastValue, boolean autocast) {
        super(beforeCastValue, new CastOperand("BigIntegerValue", autocast));
        this.value = new java.math.BigInteger(valueString);
    }

    /**
     * Copy the current value with new name <b>name</b>
     */
    @Override
    public org.openl.meta.BigIntegerValue copy(String name) {
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
    public java.math.BigInteger getValue() {
        return value;
    }

    // Equals
    @Override
    /**
     * Indicates whether some other object is "equal to" this
     * org.openl.meta.BigIntegerValue variable.
     */
    public boolean equals(Object obj) {
        return obj instanceof BigIntegerValue && value.equals(((BigIntegerValue) obj).value);
    }

    // sort
    /**
     * Sorts the array <b>values</b>
     * 
     * @param values an array for sorting
     * @return the sorted array
     */
    public static org.openl.meta.BigIntegerValue[] sort(org.openl.meta.BigIntegerValue[] values) {
        org.openl.meta.BigIntegerValue[] sortedArray = null;
        if (values != null) {
            sortedArray = new org.openl.meta.BigIntegerValue[values.length];
            org.openl.meta.BigIntegerValue[] notNullArray = ArrayTool.removeNulls(values);

            Arrays.sort(notNullArray);

            /* Filling sortedArray by sorted and null values */
            System.arraycopy(notNullArray, 0, sortedArray, 0, notNullArray.length);
        }
        return sortedArray;
    }

    // ******* Autocasts 8*************

    public static BigIntegerValue autocast(BigInteger x, BigIntegerValue y) {
        if (x == null) {
            return null;
        }

        return new BigIntegerValue(x);
    }

    // ******* Casts 8*************

    public static BigIntegerValue cast(float x, BigIntegerValue y) {
        return new BigIntegerValue(String.valueOf((long) x));
    }

    public static BigIntegerValue cast(double x, BigIntegerValue y) {
        return new BigIntegerValue(String.valueOf((long) x));
    }

    public static BigIntegerValue cast(FloatValue x, BigIntegerValue y) {
        if (x == null) {
            return null;
        }
        return new BigIntegerValue(String.valueOf(x.longValue()));
    }

    public static BigIntegerValue cast(DoubleValue x, BigIntegerValue y) {
        if (x == null) {
            return null;
        }
        return new BigIntegerValue(String.valueOf(x.longValue()));
    }

    public static BigIntegerValue cast(BigDecimal x, BigIntegerValue y) {
        if (x == null) {
            return null;
        }
        return new BigIntegerValue(x.toBigInteger());
    }

    public static byte cast(BigIntegerValue x, byte y) {
        return x.byteValue();
    }

    public static short cast(BigIntegerValue x, short y) {
        return x.shortValue();
    }

    public static char cast(BigIntegerValue x, char y) {
        return (char) x.intValue();
    }

    public static int cast(BigIntegerValue x, int y) {
        return x.intValue();
    }

    public static long cast(BigIntegerValue x, long y) {
        return x.longValue();
    }

    public static float cast(BigIntegerValue x, float y) {
        return x.floatValue();
    }

    public static double cast(BigIntegerValue x, double y) {
        return x.doubleValue();
    }

    public static BigInteger cast(BigIntegerValue x, BigInteger y) {
        if (x == null) {
            return null;
        }
        return x.getValue();
    }

    public static BigDecimal cast(BigIntegerValue x, BigDecimal y) {
        if (x == null) {
            return null;
        }

        return new BigDecimal(x.getValue());
    }

    public static ByteValue cast(BigIntegerValue x, ByteValue y) {
        if (x == null) {
            return null;
        }
        return new ByteValue(x.byteValue(), x, false);
    }

    public static ShortValue cast(BigIntegerValue x, ShortValue y) {
        if (x == null) {
            return null;
        }
        return new ShortValue(x.shortValue(), x, false);
    }

    public static IntValue cast(BigIntegerValue x, IntValue y) {
        if (x == null) {
            return null;
        }
        return new IntValue(x.intValue(), x, false);
    }

    public static LongValue cast(BigIntegerValue x, LongValue y) {
        if (x == null) {
            return null;
        }
        return new LongValue(x.longValue(), x, false);
    }

    public static FloatValue cast(BigIntegerValue x, FloatValue y) {
        if (x == null) {
            return null;
        }
        return new FloatValue(x.floatValue(), x, false);
    }

    public static DoubleValue cast(BigIntegerValue x, DoubleValue y) {
        if (x == null) {
            return null;
        }
        return new DoubleValue(x.doubleValue(), x, false);
    }

    public BigIntegerValue(String valueString) {
        value = new BigInteger(valueString);
    }

    /** Function constructor **/
    public BigIntegerValue(BigIntegerValue result, NumberOperations function, BigIntegerValue[] params) {
        super(function, params);
        this.value = result.getValue();
    }

    @Override
    public double doubleValue() {
        return value.doubleValue();
    }

    @Override
    public float floatValue() {
        return value.floatValue();
    }

    @Override
    public int intValue() {
        return value.intValue();
    }

    @Override
    public long longValue() {
        return value.longValue();
    }

    public int compareTo(BigIntegerValue o) {
        return value.compareTo(o.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    private static BigInteger[] unwrap(BigIntegerValue[] values) {
        values = ArrayTool.removeNulls(values);

        BigInteger[] unwrapArray = new BigInteger[values.length];
        for (int i = 0; i < values.length; i++) {
            unwrapArray[i] = values[i].value;
        }
        return unwrapArray;
    }

}
