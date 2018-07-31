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
import org.openl.meta.DoubleValue.DoubleValueAdapter;
import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.meta.number.CastOperand;
import org.openl.meta.number.Formulas;
import org.openl.meta.number.NumberOperations;
import org.openl.rules.util.Round;
import org.openl.util.ArrayTool;
import org.openl.util.CollectionUtils;
import org.openl.util.math.MathUtils;

@XmlRootElement
@XmlJavaTypeAdapter(DoubleValueAdapter.class)
public class DoubleValue extends ExplanationNumberValue<DoubleValue> {

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

    public static class DoubleValueAdapter extends XmlAdapter<Double,DoubleValue> {
        public DoubleValue unmarshal(Double val) throws Exception {
            return new DoubleValue(val);
        }
        public Double marshal(DoubleValue val) throws Exception {
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
    private int hashCode;

    /**
     * EPBDS-6107
     */
    public void setValue(double value) {
        this.value = value;
        this.hashCode = ((Double) value).hashCode();
    }

    /**
     * Compares two values
     * @param value1
     * @param value2
     * @return true if  value1 equal value2
     */
    public static boolean eq(org.openl.meta.DoubleValue value1, org.openl.meta.DoubleValue value2) {
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
    public static Boolean ge(org.openl.meta.DoubleValue value1, org.openl.meta.DoubleValue value2) {
        Double v1 = value1 == null ? null : value1.value;
        Double v2 = value2 == null ? null : value2.value;
        return Comparison.ge(v1, v2);
    }
    /**
     * Compares two values
     * @param value1
     * @param value2
     * @return true if  value1 greater value2
     */
    public static Boolean gt(org.openl.meta.DoubleValue value1, org.openl.meta.DoubleValue value2) {
        Double v1 = value1 == null ? null : value1.value;
        Double v2 = value2 == null ? null : value2.value;
        return Comparison.gt(v1, v2);
    }
    /**
     * Compares two values
     * @param value1
     * @param value2
     * @return true if  value1 less or equal value2
     */
    public static Boolean le(org.openl.meta.DoubleValue value1, org.openl.meta.DoubleValue value2) {
        Double v1 = value1 == null ? null : value1.value;
        Double v2 = value2 == null ? null : value2.value;
        return Comparison.le(v1, v2);
    }
    /**
     * Compares two values
     * @param value1
     * @param value2
     * @return true if  value1 less value2
     */
    public static Boolean lt(org.openl.meta.DoubleValue value1, org.openl.meta.DoubleValue value2) {
        Double v1 = value1 == null ? null : value1.value;
        Double v2 = value2 == null ? null : value2.value;
        return Comparison.lt(v1, v2);
    }
    /**
     * Compares two values
     * @param value1
     * @param value2
     * @return true if  value1 not equal value2
     */
    public static boolean ne(org.openl.meta.DoubleValue value1, org.openl.meta.DoubleValue value2) {
        if (value1 == null || value2 == null){
            return value1 != value2;
        }

        return Comparison.ne(value1.getValue(), value2.getValue());
    }

     /**
     * average
     * @param values  array of org.openl.meta.DoubleValue values
     * @return the average value from the array
     */
    public static org.openl.meta.DoubleValue avg(org.openl.meta.DoubleValue[] values) {
        if (CollectionUtils.isEmpty(values)) {
            return null;
        }
        Double[] unwrappedArray = unwrap(values);
        Double avg = MathUtils.avg(unwrappedArray);
        return avg != null ? new org.openl.meta.DoubleValue(new org.openl.meta.DoubleValue(avg), NumberOperations.AVG, values) : null;
    }
     /**
     * sum
     * @param values  array of org.openl.meta.DoubleValue values
     * @return the sum value from the array
     */
    public static org.openl.meta.DoubleValue sum(org.openl.meta.DoubleValue[] values) {
        if (CollectionUtils.isEmpty(values)) {
            return null;
        }
        Double[] unwrappedArray = unwrap(values);
        Double sum = MathUtils.sum(unwrappedArray);
        return sum != null ? new org.openl.meta.DoubleValue(new org.openl.meta.DoubleValue(sum), NumberOperations.SUM, values) : null;
    }
     /**
     * median
     * @param values  array of org.openl.meta.DoubleValue values
     * @return the median value from the array
     */
    public static org.openl.meta.DoubleValue median(org.openl.meta.DoubleValue[] values) {
        if (CollectionUtils.isEmpty(values)) {
            return null;
        }
        Double[] unwrappedArray = unwrap(values);
        Double median = MathUtils.median(unwrappedArray);
        return median != null ? new org.openl.meta.DoubleValue(new org.openl.meta.DoubleValue(median), NumberOperations.MEDIAN, values) : null;
    }

     /**
     * Compares value1 and value2 and returns the max value
     * @param value1
     * @param value2
     * @return max value
     */
    public static org.openl.meta.DoubleValue max(org.openl.meta.DoubleValue value1, org.openl.meta.DoubleValue value2) {
        // Commented to support operations with nulls
        // "null" means that data does not exist
        // validate(value1, value2, NumberOperations.MAX.toString());
        if (value1 == null)
            return value2; 
        if (value2 == null)
            return value1; 

        return new org.openl.meta.DoubleValue(MathUtils.max(value1.getValue(), value2.getValue()) ? value1 : value2,
            NumberOperations.MAX,
            new org.openl.meta.DoubleValue[] { value1, value2 });
    }
     /**
     * Compares value1 and value2 and returns the min value
     * @param value1
     * @param value2
     * @return min value
     */
    public static org.openl.meta.DoubleValue min(org.openl.meta.DoubleValue value1, org.openl.meta.DoubleValue value2) {
        // Commented to support operations with nulls
        // "null" means that data does not exist
        // validate(value1, value2, NumberOperations.MIN.toString());
        if (value1 == null)
            return value2; 
        if (value2 == null)
            return value1; 

        return new org.openl.meta.DoubleValue(MathUtils.min(value1.getValue(), value2.getValue()) ? value1 : value2,
            NumberOperations.MIN,
            new org.openl.meta.DoubleValue[] { value1, value2 });
    }

    /**
     * 
     * @param values an array org.openl.meta.DoubleValue, must not be null
     * @return org.openl.meta.DoubleValue the max element from array
     */
    public static org.openl.meta.DoubleValue max(org.openl.meta.DoubleValue[] values) {
        org.openl.meta.DoubleValue result = (org.openl.meta.DoubleValue) MathUtils.max(values);
        if (result == null) {
            return null;
        }

        return new org.openl.meta.DoubleValue((org.openl.meta.DoubleValue) getAppropriateValue(values, result),
            NumberOperations.MAX_IN_ARRAY, values);
    }
    /**
     * 
     * @param values an array org.openl.meta.DoubleValue, must not be null
     * @return org.openl.meta.DoubleValue the min element from array
     */
    public static org.openl.meta.DoubleValue min(org.openl.meta.DoubleValue[] values) {
        org.openl.meta.DoubleValue result = (org.openl.meta.DoubleValue) MathUtils.min(values);
        if (result == null) {
            return null;
        }

        return new org.openl.meta.DoubleValue((org.openl.meta.DoubleValue) getAppropriateValue(values, result),
            NumberOperations.MIN_IN_ARRAY, values);
    }
        /**
     * 
     * @param value of variable which should be copied
     * @param name of new variable
     * @return the new org.openl.meta.DoubleValue variable with name <b>name</b> and value <b>value</b>
     */
    public static org.openl.meta.DoubleValue copy(org.openl.meta.DoubleValue value, String name) {
        if (value.getName() == null) {
            value.setName(name);

            return value;
        } else if (!value.getName().equals(name)) {
            org.openl.meta.DoubleValue result = new org.openl.meta.DoubleValue (value, NumberOperations.COPY, 
                new org.openl.meta.DoubleValue[] { value });
            result.setName(name);

            return result;
        }
        return value;
    }

    //REM
    /**
     * Divides left hand operand by right hand operand and returns remainder
     * @param value1 org.openl.meta.DoubleValue 
     * @param value2 org.openl.meta.DoubleValue 
     * @return remainder from division value1 by value2
     */
    public static org.openl.meta.DoubleValue rem(org.openl.meta.DoubleValue value1, org.openl.meta.DoubleValue value2) {
        // Commented to support operations with nulls. See also MathUtils.mod()
        // validate(value1, value2, Formulas.REM.toString());
        if (value1 == null || value2 == null) {
            return ZERO;
        }

        return new org.openl.meta.DoubleValue(value1, value2, Operators.rem(value1.getValue(), value2.getValue()),
            Formulas.REM);
    }

    //ADD
    public static String add(DoubleValue value1, String value2) {
        return value1 + value2;
    }
    
    public static String add(String value1, DoubleValue value2) {
        return value1 + value2;
    } 
    
     /**
     * Adds left hand operand to right hand operand
     * @param value1 org.openl.meta.DoubleValue
     * @param value2 org.openl.meta.DoubleValue
     * @return the result of addition operation
     */
    public static org.openl.meta.DoubleValue add(org.openl.meta.DoubleValue value1, org.openl.meta.DoubleValue value2) {
        // temporary commented to support operations with nulls
        //
        //        validate(value1, value2, Formulas.ADD.toString());
        //conditions for classes that are wrappers over primitives
        if (value1 == null) {
            return value2;
        }

        if (value2 == null) {
            return value1;
        }

        return new org.openl.meta.DoubleValue(value1, value2, Operators.add(value1.getValue(), value2.getValue()),
            Formulas.ADD);
}

    // MULTIPLY
     /**
     * Multiplies left hand operand to right hand operand
     * @param value1 org.openl.meta.DoubleValue
     * @param value2 org.openl.meta.DoubleValue
     * @return the result of multiplication  operation
     */
    public static org.openl.meta.DoubleValue multiply(org.openl.meta.DoubleValue value1, org.openl.meta.DoubleValue value2) {
        // temporary commented to support operations with nulls
        //
        //        validate(value1, value2, Formulas.MULTIPLY.toString());
        if (value1 == null) {
            return value2;
        }

        if (value2 == null) {
            return value1;
        }

        return new org.openl.meta.DoubleValue(value1, value2, Operators.multiply(value1.getValue(), value2.getValue()),
            Formulas.MULTIPLY);
    }

    //SUBTRACT
    /**
     * Subtracts left hand operand to right hand operand
     * @param value1 org.openl.meta.DoubleValue
     * @param value2 org.openl.meta.DoubleValue
     * @return the result of subtraction  operation
     */
    public static org.openl.meta.DoubleValue subtract(org.openl.meta.DoubleValue value1, org.openl.meta.DoubleValue value2) {
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

        return new org.openl.meta.DoubleValue(value1, value2, Operators.subtract(value1.getValue(), value2.getValue()), 
            Formulas.SUBTRACT);
    }

    // DIVIDE
    /**
     * Divides left hand operand by right hand operand
     * @param value1 org.openl.meta.DoubleValue
     * @param value2 org.openl.meta.DoubleValue
     * @return the result of division  operation
     */
    public static org.openl.meta.DoubleValue divide(org.openl.meta.DoubleValue value1, org.openl.meta.DoubleValue value2) {
        // temporary commented to support operations with nulls
        //
        //        validate(value1, value2, Formulas.DIVIDE.toString());
        if (value1 == null && value2 == null) {
            return null;
        }

        if (value1 == null) {
            if (value2 != null && value2.doubleValue() != 0) {
                return new org.openl.meta.DoubleValue(value1, value2, divide(ONE, value2).getValue(), Formulas.DIVIDE);
            }
        }

        if (value2 == null) {
            return new org.openl.meta.DoubleValue(value1, value2, value1.getValue(), Formulas.DIVIDE);
        }

        if (value2.doubleValue() == 0) {

            // FIXME: temporary commented the throwing exception
            // Is needed for the one of the commercial products, pls contact Denis Levchuk
            //
            return new org.openl.meta.DoubleValue(value1, value2, Double.POSITIVE_INFINITY, Formulas.DIVIDE);
        }

        return new org.openl.meta.DoubleValue(value1, value2, Operators.divide(value1.getValue(), value2.getValue()),
            Formulas.DIVIDE);
    }

    // QUAOTIENT
    /**
     * Divides left hand operand by right hand operand
     * @param number org.openl.meta.DoubleValue
     * @param divisor org.openl.meta.DoubleValue
     * @return LongValue the result of division  operation
     */
    public static LongValue quotient(org.openl.meta.DoubleValue number, org.openl.meta.DoubleValue divisor) {
        if (number != null && divisor != null) {
            LongValue result = new LongValue(MathUtils.quotient(number.getValue(), divisor.getValue()));
            return new LongValue(result, NumberOperations.QUOTIENT, null);
        }
        return null;
    }

    // generated product function for types that are wrappers over primitives
     /**
     * Multiplies the numbers from the provided array and returns the product as a number.
     * @param values an array of IntValue which will be converted to DoubleValue
     * @return the product as a number
     */
    public static DoubleValue product(org.openl.meta.DoubleValue[] values) {
        if (CollectionUtils.isEmpty(values)) {
            return null;
        }
        Double[] unwrappedArray = unwrap(values);
        Double product = MathUtils.product(unwrappedArray);
        // we loose the parameters, but not the result of computation.
        return product != null ? new DoubleValue(new DoubleValue(product), NumberOperations.PRODUCT, null) : null;
    }
     /**
     *   
     * @param number
     * @param divisor
     * @return the remainder after a number is divided by a divisor. The result is a numeric value and has the same sign as the devisor.
     */
    public static org.openl.meta.DoubleValue mod(org.openl.meta.DoubleValue number, org.openl.meta.DoubleValue divisor) {
        if (number != null && divisor != null) {
            org.openl.meta.DoubleValue result = new org.openl.meta.DoubleValue(MathUtils.mod(number.getValue(), divisor.getValue()));
            return new org.openl.meta.DoubleValue(result, NumberOperations.MOD, new org.openl.meta.DoubleValue[]{number, divisor} );
        }
        return null;
    }

    /**
     * Sorts the array <b>values</b> in ascending order and returns the value from array <b>values</b> at position <b>position</b>
     * @param values array of org.openl.meta.DoubleValue values 
     * @param position int value
     * @return the value from array <b>values</b> at position <b>position</b>
     */
    public static org.openl.meta.DoubleValue small(org.openl.meta.DoubleValue[] values, int position) {
        if (CollectionUtils.isEmpty(values)) {
            return null;
        }
        Double[] unwrappedArray = unwrap(values);
        Double small = MathUtils.small(unwrappedArray, position);
        return new org.openl.meta.DoubleValue((org.openl.meta.DoubleValue) getAppropriateValue(values, new org.openl.meta.DoubleValue(small)), 
            NumberOperations.SMALL, values);
    }

    /**
     * Sorts the array <b>values</b> in descending order and returns the value from array <b>values</b> at position <b>position</b>
     * @param values array of org.openl.meta.DoubleValue values 
     * @param position int value
     * @return the value from array <b>values</b> at position <b>position</b>
     */
    public static org.openl.meta.DoubleValue big(org.openl.meta.DoubleValue[] values, int position) {
        if (CollectionUtils.isEmpty(values)) {
            return null;
        }
        Double[] unwrappedArray = unwrap(values);
        Double big = MathUtils.big(unwrappedArray, position);
        return new org.openl.meta.DoubleValue((org.openl.meta.DoubleValue) getAppropriateValue(values, new org.openl.meta.DoubleValue(big)),
            NumberOperations.BIG, values);
    }

    /**
     * 
     * @param value1
     * @param value2
     * @return the result of value1 raised to the power of value2
     */
    public static org.openl.meta.DoubleValue pow(org.openl.meta.DoubleValue value1, org.openl.meta.DoubleValue value2) {
        // Commented to support operations with nulls
        // "null" means that data does not exist
        //
        // validate(value1, value2, NumberOperations.POW);
        if (value1 == null) {
            return value2 == null ? null : new org.openl.meta.DoubleValue((double) 0);
        } else if (value2 == null) {
            return value1;
        }

        return new org.openl.meta.DoubleValue(new org.openl.meta.DoubleValue(Operators.pow(value1.getValue(), value2.getValue())), 
            NumberOperations.POW, new org.openl.meta.DoubleValue[] { value1, value2 });
    }

    /**
     * 
     * @param value
     * @return the absolute value (module) of the value <b>value </b>
     */
    public static org.openl.meta.DoubleValue abs(org.openl.meta.DoubleValue value) {
        // Commented to support operations with nulls.
        // validate(value, NumberOperations.ABS);
        if (value == null) {
            return null;
        }
        // evaluate result
        org.openl.meta.DoubleValue result = new org.openl.meta.DoubleValue(Operators.abs(value.getValue()));
        // create instance with information about last operation
        return new org.openl.meta.DoubleValue(result, NumberOperations.ABS, new org.openl.meta.DoubleValue[] { value });
    }

    /**
     * 
     * @param value
     * @return the negative value of the <b>value</b>
     */
    public static org.openl.meta.DoubleValue negative(org.openl.meta.DoubleValue value) {
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
    public static org.openl.meta.DoubleValue inc(org.openl.meta.DoubleValue value) {
        return add(value, ONE);
    }

    /**
     * 
     * @param value
     * @return the <b>value</b>
     */
    public static org.openl.meta.DoubleValue positive(org.openl.meta.DoubleValue value) {
        return value;
    }

    /**
     * 
     * @param value
     * @return the <b>value </b> decreased by 1
     */
    public static org.openl.meta.DoubleValue dec(org.openl.meta.DoubleValue value) {
        return subtract(value, ONE);
    }

    // Autocasts

    /**
     * Is used to overload implicit cast operators from byte to org.openl.meta.DoubleValue
     * @param x
     * @param y is needed to avoid ambiguity in Java method resolution
     * @return the casted value to org.openl.meta.DoubleValue
     */
    public static org.openl.meta.DoubleValue autocast(byte x, org.openl.meta.DoubleValue y) {
        return new org.openl.meta.DoubleValue((double) x);
    }
    /**
     * Is used to overload implicit cast operators from short to org.openl.meta.DoubleValue
     * @param x
     * @param y is needed to avoid ambiguity in Java method resolution
     * @return the casted value to org.openl.meta.DoubleValue
     */
    public static org.openl.meta.DoubleValue autocast(short x, org.openl.meta.DoubleValue y) {
        return new org.openl.meta.DoubleValue((double) x);
    }
    /**
     * Is used to overload implicit cast operators from int to org.openl.meta.DoubleValue
     * @param x
     * @param y is needed to avoid ambiguity in Java method resolution
     * @return the casted value to org.openl.meta.DoubleValue
     */
    public static org.openl.meta.DoubleValue autocast(int x, org.openl.meta.DoubleValue y) {
        return new org.openl.meta.DoubleValue((double) x);
    }
    /**
     * Is used to overload implicit cast operators from int to org.openl.meta.DoubleValue
     * @param x
     * @param y is needed to avoid ambiguity in Java method resolution
     * @return the casted value to org.openl.meta.DoubleValue
     */
    public static org.openl.meta.DoubleValue autocast(char x, org.openl.meta.DoubleValue y) {
        return new org.openl.meta.DoubleValue((double) x);
    }
    /**
     * Is used to overload implicit cast operators from long to org.openl.meta.DoubleValue
     * @param x
     * @param y is needed to avoid ambiguity in Java method resolution
     * @return the casted value to org.openl.meta.DoubleValue
     */
    public static org.openl.meta.DoubleValue autocast(long x, org.openl.meta.DoubleValue y) {
        return new org.openl.meta.DoubleValue((double) x);
    }
    /**
     * Is used to overload implicit cast operators from float to org.openl.meta.DoubleValue
     * @param x
     * @param y is needed to avoid ambiguity in Java method resolution
     * @return the casted value to org.openl.meta.DoubleValue
     */
    public static org.openl.meta.DoubleValue autocast(float x, org.openl.meta.DoubleValue y) {
        return new org.openl.meta.DoubleValue((double) x);
    }
    /**
     * Is used to overload implicit cast operators from double to org.openl.meta.DoubleValue
     * @param x
     * @param y is needed to avoid ambiguity in Java method resolution
     * @return the casted value to org.openl.meta.DoubleValue
     */
    public static org.openl.meta.DoubleValue autocast(double x, org.openl.meta.DoubleValue y) {
        return new org.openl.meta.DoubleValue((double) x);
    }

    // Constructors
    public DoubleValue(double value) { 
        this.value = value;
        this.hashCode = ((Double) this.value).hashCode();
    }

    /**Formula constructor**/
    public DoubleValue(org.openl.meta.DoubleValue lv1, org.openl.meta.DoubleValue lv2, double value, Formulas operand) {
        super(lv1, lv2, operand);
        this.value = value;
        this.hashCode = ((Double) this.value).hashCode();
    }

    /**Cast constructor**/
    public DoubleValue(double value, ExplanationNumberValue<?> beforeCastValue, boolean autocast) {
        super(beforeCastValue, new CastOperand("DoubleValue", autocast));
        this.value = value;
        this.hashCode = ((Double) this.value).hashCode();
    }

    /**
    *Copy the current value with new name <b>name</b>
    */
    @Override
    public org.openl.meta.DoubleValue copy(String name) {
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
    public double getValue() {
        return value;
    }

    //Equals
    @Override
     /**
     * Indicates whether some other object is "equal to" this org.openl.meta.DoubleValue variable. 
     */
    public boolean equals(Object obj) {
        return obj instanceof DoubleValue && value == ((DoubleValue) obj).value;
    }

    public static boolean isNumeric(String str)
    {
        try
        {
            double d = Double.parseDouble(str);
        }
        catch(NumberFormatException nfe)
        {
            return false;
        }
        return true;
    }

    // sort
    /**
    * Sorts the array <b>values</b>
    * @param values an array for sorting
    * @return the sorted array
    */
    public static org.openl.meta.DoubleValue[] sort (org.openl.meta.DoubleValue[] values ) {
        org.openl.meta.DoubleValue[] sortedArray = null;
        if (values != null) {
            sortedArray = new org.openl.meta.DoubleValue[values.length];
           org.openl.meta.DoubleValue[] notNullArray = ArrayTool.removeNulls(values);

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
        return BigInteger.valueOf(x.longValue());
    }
    
    public static BigDecimal cast(DoubleValue x, BigDecimal y) {
        return BigDecimal.valueOf(x.doubleValue());
    }

    public static DoubleValue round(DoubleValue value) {
        if (value == null) {
            return null;
        }

        double rounded = Round.round(value.value, 0);
        DoubleValue newValue = new DoubleValue(rounded);
        return new DoubleValue(newValue, NumberOperations.ROUND, value);
    }

    public static DoubleValue round(DoubleValue value, int scale) {
        if (value == null) {
            return null;
        }

        double rounded = Round.round(value.value, scale);
        DoubleValue newValue = new DoubleValue(rounded);
        return new DoubleValue(newValue, NumberOperations.ROUND, value, new DoubleValue(scale));
    }

    public static DoubleValue round(DoubleValue value, int scale, int roundingMethod) {
        if (value == null) {
            return null;
        }

        double rounded = Round.round(value.value, scale, roundingMethod);
        DoubleValue newValue = new DoubleValue(rounded);
        return new DoubleValue(newValue, NumberOperations.ROUND, value, new DoubleValue(scale));
    }

    /**
     * 
     * @deprecated This method is obsolete. Use {@link #round(DoubleValue, int)}
     *             instead
     * @see #round(DoubleValue, int)
     */
    @Deprecated
    public static DoubleValue round(DoubleValue d, DoubleValue p) {
        if (d == null || p == null) {
            throw new OpenLRuntimeException("None of the arguments for 'round' operation can be null");
        }

        int scale;
        double preRoundedValue;

        if (p.doubleValue() == 0) {
            scale = 0;
            preRoundedValue = d.doubleValue();
        } else {
            scale = (int) Round.round(-Math.log10(p.doubleValue()), 0, Round.HALF_UP);
            preRoundedValue = d.doubleValue();
            // preRoundedValue = Math.round(d.doubleValue() / p.doubleValue()) *
            // p.doubleValue();
        }

        double roundedValue = Round.round(preRoundedValue, scale, Round.HALF_UP);

        return new DoubleValue(new DoubleValue(roundedValue), NumberOperations.ROUND, new DoubleValue[] { d, p });
    }

    public DoubleValue(String valueString) {
        super();
        value = Double.parseDouble(valueString);
    }

    /** Function constructor **/
    public DoubleValue(DoubleValue result, NumberOperations function, DoubleValue... params) {
        super(function, params);
        this.value = result.doubleValue();
    }

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
        return hashCode;
    }

    private static Double[] unwrap(DoubleValue[] values) {
        values = ArrayTool.removeNulls(values);

        Double[] unwrappedArray = new Double[values.length];
        for (int i = 0; i < values.length; i++) {
            unwrappedArray[i] = values[i].getValue();
        }
        return unwrappedArray;
    }

}
