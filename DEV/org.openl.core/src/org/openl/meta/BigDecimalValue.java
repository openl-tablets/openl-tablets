package org.openl.meta;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.openl.binding.impl.Operators;
import org.openl.binding.impl.operator.Comparison;
import org.openl.exception.OpenLRuntimeException;
import org.openl.meta.BigDecimalValue.BigDecimalValueAdapter;
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
@XmlJavaTypeAdapter(BigDecimalValueAdapter.class)
public class BigDecimalValue extends ExplanationNumberValue<BigDecimalValue> {

    private static final long serialVersionUID = 1996508840075924034L;

    private static final BigDecimalValue ZERO = new BigDecimalValue("0");
    private static final BigDecimalValue ONE = new BigDecimalValue("1");
    private static final BigDecimalValue MINUS_ONE = new BigDecimalValue("-1");

    private final java.math.BigDecimal value;

    public static class BigDecimalValueAdapter extends XmlAdapter<BigDecimal,BigDecimalValue> {
        public BigDecimalValue unmarshal(BigDecimal val) throws Exception {
            return new BigDecimalValue(val);
        }
        public BigDecimal marshal(BigDecimalValue val) throws Exception {
            return val.getValue();
        }
    }

    static BigDecimalValue instance(BigDecimal result, NumberOperations operation, BigDecimalValue... values) {
        return result == null ? null : new BigDecimalValue(new BigDecimalValue(result), operation, values);
    }

    private static BigDecimalValue instance(BigDecimalValue result, NumberOperations operation, BigDecimalValue... values) {
        return result == null ? null : new BigDecimalValue(result, operation, values);
    }

    public static BigDecimalValue max(BigDecimalValue... values) {
        return instance(Statistics.max(values), NumberOperations.MAX, values);
    }

    public static BigDecimalValue min(BigDecimalValue... values) {
        return instance(Statistics.min(values), NumberOperations.MIN, values);
    }

    public static BigDecimalValue sum(BigDecimalValue... values) {
        return instance(Sum.sum(unwrap(values)), NumberOperations.SUM, values);
    }

    public static BigDecimalValue avg(BigDecimalValue... values) {
        return instance(Avg.avg(unwrap(values)), NumberOperations.AVG, values);
    }

    public static BigDecimalValue median(BigDecimalValue... values) {
        return instance(MathUtils.median(unwrap(values)), NumberOperations.MEDIAN, values);
    }

    public static BigDecimalValue product(BigDecimalValue... values) {
        return instance(MathUtils.product(unwrap(values)), NumberOperations.PRODUCT, values);
    }

    /**
     * Compares two values
     * @param value1
     * @param value2
     * @return true if  value1 equal value2
     */
    public static boolean eq(org.openl.meta.BigDecimalValue value1, org.openl.meta.BigDecimalValue value2) {
        if (value1 == null || value2 == null){
            return value1 == value2;
        }
        return Comparison.eq(value1.getValue(), value2.getValue());
    }
    /**
     * Compares two values
     * @param value1
     * @param value2
     * @return true if  value1 greater or equal value2
     */
    public static Boolean ge(org.openl.meta.BigDecimalValue value1, org.openl.meta.BigDecimalValue value2) {
        BigDecimal v1 = value1 == null ? null : value1.value;
        BigDecimal v2 = value2 == null ? null : value2.value;
        return Comparison.ge(v1, v2);
    }
    /**
     * Compares two values
     * @param value1
     * @param value2
     * @return true if  value1 greater value2
     */
    public static Boolean gt(org.openl.meta.BigDecimalValue value1, org.openl.meta.BigDecimalValue value2) {
        BigDecimal v1 = value1 == null ? null : value1.value;
        BigDecimal v2 = value2 == null ? null : value2.value;
        return Comparison.gt(v1, v2);
    }
    /**
     * Compares two values
     * @param value1
     * @param value2
     * @return true if  value1 less or equal value2
     */
    public static Boolean le(org.openl.meta.BigDecimalValue value1, org.openl.meta.BigDecimalValue value2) {
        BigDecimal v1 = value1 == null ? null : value1.value;
        BigDecimal v2 = value2 == null ? null : value2.value;
        return Comparison.le(v1, v2);
    }
    /**
     * Compares two values
     * @param value1
     * @param value2
     * @return true if  value1 less value2
     */
    public static Boolean lt(org.openl.meta.BigDecimalValue value1, org.openl.meta.BigDecimalValue value2) {
        BigDecimal v1 = value1 == null ? null : value1.value;
        BigDecimal v2 = value2 == null ? null : value2.value;
        return Comparison.lt(v1, v2);
    }
    /**
     * Compares two values
     * @param value1
     * @param value2
     * @return true if  value1 not equal value2
     */
    public static boolean ne(org.openl.meta.BigDecimalValue value1, org.openl.meta.BigDecimalValue value2) {
        if (value1 == null || value2 == null){
            return value1 != value2;
        }

        return Comparison.ne(value1.getValue(), value2.getValue());
    }
        /**
     * 
     * @param value of variable which should be copied
     * @param name of new variable
     * @return the new org.openl.meta.BigDecimalValue variable with name <b>name</b> and value <b>value</b>
     */
    public static org.openl.meta.BigDecimalValue copy(org.openl.meta.BigDecimalValue value, String name) {
        if (value.getName() == null) {
            value.setName(name);

            return value;
        } else if (!value.getName().equals(name)) {
            org.openl.meta.BigDecimalValue result = new org.openl.meta.BigDecimalValue (value, NumberOperations.COPY, 
                new org.openl.meta.BigDecimalValue[] { value });
            result.setName(name);

            return result;
        }
        return value;
    }

    //REM
    /**
     * Divides left hand operand by right hand operand and returns remainder
     * @param value1 org.openl.meta.BigDecimalValue 
     * @param value2 org.openl.meta.BigDecimalValue 
     * @return remainder from division value1 by value2
     */
    public static org.openl.meta.BigDecimalValue rem(org.openl.meta.BigDecimalValue value1, org.openl.meta.BigDecimalValue value2) {
        // Commented to support operations with nulls. See also MathUtils.mod()
        // validate(value1, value2, Formulas.REM.toString());
        if (value1 == null || value2 == null) {
            return ZERO;
        }

        return new org.openl.meta.BigDecimalValue(value1, value2, Operators.rem(value1.getValue(), value2.getValue()),
            Formulas.REM);
    }

    //ADD
    public static String add(BigDecimalValue value1, String value2) {
        return value1 + value2;
    }
    
    public static String add(String value1, BigDecimalValue value2) {
        return value1 + value2;
    }
    
     /**
     * Adds left hand operand to right hand operand
     * @param value1 org.openl.meta.BigDecimalValue
     * @param value2 org.openl.meta.BigDecimalValue
     * @return the result of addition operation
     */
    public static org.openl.meta.BigDecimalValue add(org.openl.meta.BigDecimalValue value1, org.openl.meta.BigDecimalValue value2) {
        // temporary commented to support operations with nulls
        //
        //        validate(value1, value2, Formulas.ADD.toString());
        //conditions big types
        if (value1 == null) {
            return value2;
        }

        if (value2 == null) {
            return value1;
        }

        return new org.openl.meta.BigDecimalValue(value1, value2, Operators.add(value1.getValue(), value2.getValue()),
            Formulas.ADD);
    }

    // MULTIPLY
     /**
     * Multiplies left hand operand to right hand operand
     * @param value1 org.openl.meta.BigDecimalValue
     * @param value2 org.openl.meta.BigDecimalValue
     * @return the result of multiplication  operation
     */
    public static org.openl.meta.BigDecimalValue multiply(org.openl.meta.BigDecimalValue value1, org.openl.meta.BigDecimalValue value2) {
        // temporary commented to support operations with nulls
        //
        //        validate(value1, value2, Formulas.MULTIPLY.toString());
        if (value1 == null) {
            return value2;
        }

        if (value2 == null) {
            return value1;
        }

        return new org.openl.meta.BigDecimalValue(value1, value2, Operators.multiply(value1.getValue(), value2.getValue()),
            Formulas.MULTIPLY);
    }

    //SUBTRACT
    /**
     * Subtracts left hand operand to right hand operand
     * @param value1 org.openl.meta.BigDecimalValue
     * @param value2 org.openl.meta.BigDecimalValue
     * @return the result of subtraction  operation
     */
    public static org.openl.meta.BigDecimalValue subtract(org.openl.meta.BigDecimalValue value1, org.openl.meta.BigDecimalValue value2) {
        // temporary commented to support operations with nulls
        //
        //        validate(value1, value2, Formulas.SUBTRACT.toString());
        if (value1 == null && value2 == null) {
            return null;
        }

        if (value1 == null) {
            return negative(value2);
        }

        if (value2 == null) {
            return value1;
        }

        return new org.openl.meta.BigDecimalValue(value1, value2, Operators.subtract(value1.getValue(), value2.getValue()), 
            Formulas.SUBTRACT);
    }

    // DIVIDE
    /**
     * Divides left hand operand by right hand operand
     * @param value1 org.openl.meta.BigDecimalValue
     * @param value2 org.openl.meta.BigDecimalValue
     * @return the result of division  operation
     */
    public static org.openl.meta.BigDecimalValue divide(org.openl.meta.BigDecimalValue value1, org.openl.meta.BigDecimalValue value2) {
        // temporary commented to support operations with nulls
        //
        //        validate(value1, value2, Formulas.DIVIDE.toString());
        if (value1 == null && value2 == null) {
            return null;
        }

        if (value1 == null) {
            if (value2 != null && value2.doubleValue() != 0) {
                return new org.openl.meta.BigDecimalValue(value1, value2, divide(ONE, value2).getValue(), Formulas.DIVIDE);
            }
        }

        if (value2 == null) {
            return new org.openl.meta.BigDecimalValue(value1, value2, value1.getValue(), Formulas.DIVIDE);
        }

        if (value2.doubleValue() == 0) {
            throw new OpenLRuntimeException("Division by zero");
        }

        return new org.openl.meta.BigDecimalValue(value1, value2, Operators.divide(value1.getValue(), value2.getValue()),
            Formulas.DIVIDE);
    }

    // QUAOTIENT
    /**
     * Divides left hand operand by right hand operand
     * @param number org.openl.meta.BigDecimalValue
     * @param divisor org.openl.meta.BigDecimalValue
     * @return LongValue the result of division  operation
     */
    public static LongValue quotient(org.openl.meta.BigDecimalValue number, org.openl.meta.BigDecimalValue divisor) {
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
     * @return the remainder after a number is divided by a divisor. The result is a numeric value and has the same sign as the devisor.
     */
    public static org.openl.meta.BigDecimalValue mod(org.openl.meta.BigDecimalValue number, org.openl.meta.BigDecimalValue divisor) {
        if (number != null && divisor != null) {
            org.openl.meta.BigDecimalValue result = new org.openl.meta.BigDecimalValue(MathUtils.mod(number.getValue(), divisor.getValue()));
            return new org.openl.meta.BigDecimalValue(result, NumberOperations.MOD, new org.openl.meta.BigDecimalValue[]{number, divisor} );
        }
        return null;
    }

    /**
     * Sorts the array <b>values</b> in ascending order and returns the value from array <b>values</b> at position <b>position</b>
     * @param values array of org.openl.meta.BigDecimalValue values 
     * @param position int value
     * @return the value from array <b>values</b> at position <b>position</b>
     */
    public static BigDecimalValue small(BigDecimalValue[] values, int position) {
        return instance(MathUtils.small(unwrap(values), position), NumberOperations.SMALL, values);
    }

    /**
     * Sorts the array <b>values</b> in descending order and returns the value from array <b>values</b> at position <b>position</b>
     * @param values array of org.openl.meta.BigDecimalValue values 
     * @param position int value
     * @return the value from array <b>values</b> at position <b>position</b>
     */
    public static BigDecimalValue big(BigDecimalValue[] values, int position) {
        return instance(MathUtils.big(unwrap(values), position), NumberOperations.BIG, values);
    }

    /**
     * 
     * @param value1
     * @param value2
     * @return the result of value1 raised to the power of value2
     */
    public static org.openl.meta.BigDecimalValue pow(org.openl.meta.BigDecimalValue value1, org.openl.meta.BigDecimalValue value2) {
        // Commented to support operations with nulls
        // "null" means that data does not exist
        //
        // validate(value1, value2, NumberOperations.POW);
        if (value1 == null) {
            return value2 == null ? null : new org.openl.meta.BigDecimalValue("0");
        } else if (value2 == null) {
            return value1;
        }

        return new org.openl.meta.BigDecimalValue(new org.openl.meta.BigDecimalValue(Operators.pow(value1.getValue(), value2.getValue())), 
            NumberOperations.POW, new org.openl.meta.BigDecimalValue[] { value1, value2 });
    }

    /**
     * 
     * @param value
     * @return the absolute value (module) of the value <b>value </b>
     */
    public static org.openl.meta.BigDecimalValue abs(org.openl.meta.BigDecimalValue value) {
        // Commented to support operations with nulls.
        // validate(value, NumberOperations.ABS);
        if (value == null) {
            return null;
        }
        // evaluate result
        org.openl.meta.BigDecimalValue result = new org.openl.meta.BigDecimalValue(Operators.abs(value.getValue()));
        // create instance with information about last operation
        return new org.openl.meta.BigDecimalValue(result, NumberOperations.ABS, new org.openl.meta.BigDecimalValue[] { value });
    }

    /**
     * 
     * @param value
     * @return the negative value of the <b>value</b>
     */
    public static org.openl.meta.BigDecimalValue negative(org.openl.meta.BigDecimalValue value) {
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
    public static org.openl.meta.BigDecimalValue inc(org.openl.meta.BigDecimalValue value) {
        return add(value, ONE);
    }

    /**
     * 
     * @param value
     * @return the <b>value</b>
     */
    public static org.openl.meta.BigDecimalValue positive(org.openl.meta.BigDecimalValue value) {
        return value;
    }

    /**
     * 
     * @param value
     * @return the <b>value </b> decreased by 1
     */
    public static org.openl.meta.BigDecimalValue dec(org.openl.meta.BigDecimalValue value) {
        return subtract(value, ONE);
    }

    // Autocasts

    /**
     * Is used to overload implicit cast operators from byte to org.openl.meta.BigDecimalValue
     * @param x
     * @param y is needed to avoid ambiguity in Java method resolution
     * @return the casted value to org.openl.meta.BigDecimalValue
     */
    public static org.openl.meta.BigDecimalValue autocast(byte x, org.openl.meta.BigDecimalValue y) {
        return new org.openl.meta.BigDecimalValue(String.valueOf(x));
    }
    /**
     * Is used to overload implicit cast operators from short to org.openl.meta.BigDecimalValue
     * @param x
     * @param y is needed to avoid ambiguity in Java method resolution
     * @return the casted value to org.openl.meta.BigDecimalValue
     */
    public static org.openl.meta.BigDecimalValue autocast(short x, org.openl.meta.BigDecimalValue y) {
        return new org.openl.meta.BigDecimalValue(String.valueOf(x));
    }
    /**
     * Is used to overload implicit cast operators from int to org.openl.meta.BigDecimalValue
     * @param x
     * @param y is needed to avoid ambiguity in Java method resolution
     * @return the casted value to org.openl.meta.BigDecimalValue
     */
    public static org.openl.meta.BigDecimalValue autocast(int x, org.openl.meta.BigDecimalValue y) {
        return new org.openl.meta.BigDecimalValue(String.valueOf(x));
    }
    /**
     * Is used to overload implicit cast operators from char to org.openl.meta.BigDecimalValue
     * @param x
     * @param y is needed to avoid ambiguity in Java method resolution
     * @return the casted value to org.openl.meta.BigDecimalValue
     */
    public static org.openl.meta.BigDecimalValue autocast(char x, org.openl.meta.BigDecimalValue y) {
        return new org.openl.meta.BigDecimalValue(String.valueOf((int)x));
    }
    /**
     * Is used to overload implicit cast operators from long to org.openl.meta.BigDecimalValue
     * @param x
     * @param y is needed to avoid ambiguity in Java method resolution
     * @return the casted value to org.openl.meta.BigDecimalValue
     */
    public static org.openl.meta.BigDecimalValue autocast(long x, org.openl.meta.BigDecimalValue y) {
        return new org.openl.meta.BigDecimalValue(String.valueOf(x));
    }
    /**
     * Is used to overload implicit cast operators from float to org.openl.meta.BigDecimalValue
     * @param x
     * @param y is needed to avoid ambiguity in Java method resolution
     * @return the casted value to org.openl.meta.BigDecimalValue
     */
    public static org.openl.meta.BigDecimalValue autocast(float x, org.openl.meta.BigDecimalValue y) {
        return new org.openl.meta.BigDecimalValue(String.valueOf(x));
    }
    /**
     * Is used to overload implicit cast operators from double to org.openl.meta.BigDecimalValue
     * @param x
     * @param y is needed to avoid ambiguity in Java method resolution
     * @return the casted value to org.openl.meta.BigDecimalValue
     */
    public static org.openl.meta.BigDecimalValue autocast(double x, org.openl.meta.BigDecimalValue y) {
        return new org.openl.meta.BigDecimalValue(String.valueOf(x));
    }

    // Constructors
    public BigDecimalValue(java.math.BigDecimal value) {
        this.value = value;
    }

    /**Formula constructor**/
    public BigDecimalValue(org.openl.meta.BigDecimalValue lv1, org.openl.meta.BigDecimalValue lv2, java.math.BigDecimal value, Formulas operand) {
        super(lv1, lv2, operand);
        this.value = value;
    }

    /**Cast constructor**/
    public BigDecimalValue(String valueString, ExplanationNumberValue<?> beforeCastValue, boolean autocast) {
        super(beforeCastValue, new CastOperand("BigDecimalValue", autocast));
        this.value = new java.math.BigDecimal(valueString);
    }

    /**
    *Copy the current value with new name <b>name</b>
    */
    @Override
    public org.openl.meta.BigDecimalValue copy(String name) {
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
    public java.math.BigDecimal getValue() {
        return value;
    }

    //Equals
    @Override
     /**
     * Indicates whether some other object is "equal to" this org.openl.meta.BigDecimalValue variable. 
     */
    public boolean equals(Object obj) {
        if (obj instanceof org.openl.meta.BigDecimalValue) {
            org.openl.meta.BigDecimalValue secondObj = (org.openl.meta.BigDecimalValue) obj;
            return Comparison.eq(getValue(), secondObj.getValue());
        }

        return false;
    }

    // sort
    /**
    * Sorts the array <b>values</b>
    * @param values an array for sorting
    * @return the sorted array
    */
    public static org.openl.meta.BigDecimalValue[] sort (org.openl.meta.BigDecimalValue[] values ) {
        org.openl.meta.BigDecimalValue[] sortedArray = null;
        if (values != null) {
            sortedArray = new org.openl.meta.BigDecimalValue[values.length];
           org.openl.meta.BigDecimalValue[] notNullArray = ArrayTool.removeNulls(values);

            Arrays.sort(notNullArray);

            /* Filling sortedArray by sorted and null values */
            System.arraycopy(notNullArray, 0, sortedArray, 0, notNullArray.length);
        }
        return sortedArray;
    }

    // ******* Autocasts *************

    public static BigDecimalValue autocast(BigInteger x, BigDecimalValue y) {
        if (x == null) {
            return null;
        }

        return new BigDecimalValue(new BigDecimal(x));
    }

    public static BigDecimalValue autocast(BigDecimal x, BigDecimalValue y) {
        if (x == null) {
            return null;
        }

        return new BigDecimalValue(x);
    }

    public static BigDecimalValue autocast(BigIntegerValue x, BigDecimalValue y) {
        if (x == null) {
            return null;
        }

        return new BigDecimalValue(new BigDecimal(x.getValue()));
    }

    // ******* Casts *************

    public static byte cast(BigDecimalValue x, byte y) {
        return x.byteValue();
    }

    public static short cast(BigDecimalValue x, short y) {
        return x.shortValue();
    }

    public static char cast(BigDecimalValue x, char y) {
        return (char) x.intValue();
    }

    public static int cast(BigDecimalValue x, int y) {
        return x.intValue();
    }

    public static long cast(BigDecimalValue x, long y) {
        return x.longValue();
    }

    public static float cast(BigDecimalValue x, float y) {
        return x.floatValue();
    }

    public static double cast(BigDecimalValue x, double y) {
        return x.doubleValue();
    }

    public static BigDecimal cast(BigDecimalValue x, BigDecimal y) {
        if (x == null) {
            return null;
        }
        return x.getValue();
    }

    public static BigInteger cast(BigDecimalValue x, BigInteger y) {
        if (x == null) {
            return null;
        }
        return x.getValue().toBigInteger();
    }
    
    public static ByteValue cast(BigDecimalValue x, ByteValue y) {
        if (x == null) {
            return null;
        }
        return new ByteValue(x.byteValue(), x, false);
    }

    public static ShortValue cast(BigDecimalValue x, ShortValue y) {
        if (x == null) {
            return null;
        }
        return new ShortValue(x.shortValue(), x, false);
    }

    public static IntValue cast(BigDecimalValue x, IntValue y) {
        if (x == null) {
            return null;
        }
        return new IntValue(x.intValue(), x, false);
    }

    public static LongValue cast(BigDecimalValue x, LongValue y) {
        if (x == null) {
            return null;
        }
        return new LongValue(x.longValue(), x, false);
    }

    public static FloatValue cast(BigDecimalValue x, FloatValue y) {
        if (x == null) {
            return null;
        }
        return new FloatValue(x.floatValue(), x, false);
    }

    public static DoubleValue cast(BigDecimalValue x, DoubleValue y) {
        if (x == null) {
            return null;
        }
        return new DoubleValue(x.doubleValue(), x, false);
    }

    public static BigIntegerValue cast(BigDecimalValue x, BigIntegerValue y) {
        if (x == null) {
            return null;
        }
        return new BigIntegerValue(String.valueOf(x.longValue()), x, false);
    }

    public static BigDecimalValue round(BigDecimalValue value) {
        return round(value, 0);
    }

    public static BigDecimalValue round(BigDecimalValue value, int scale) {
        return round(value, scale, BigDecimal.ROUND_HALF_UP);
    }

    public static BigDecimalValue round(BigDecimalValue value, int scale, int roundingMethod) {
        if (value == null) {
            return null;
        }

        return new BigDecimalValue(new BigDecimalValue(value.getValue().setScale(scale, roundingMethod)),
            NumberOperations.ROUND,
            new BigDecimalValue[] { value });
    }

    public BigDecimalValue(String valueString) {
        value = new BigDecimal(valueString);
    }

    /** Function constructor **/
    public BigDecimalValue(BigDecimalValue result, NumberOperations function, BigDecimalValue[] params) {
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

    public int compareTo(BigDecimalValue o) {
        return value.compareTo(o.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    private static BigDecimal[] unwrap(BigDecimalValue[] values) {
        if (values == null) {
            return null;
        }
        values = ArrayTool.removeNulls(values);

        BigDecimal[] unwrappedArray = new BigDecimal[values.length];
        for (int i = 0; i < values.length; i++) {
            unwrappedArray[i] = values[i].getValue();
        }
        return unwrappedArray;
    }

}
