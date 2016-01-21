package org.openl.meta;

import org.apache.commons.lang3.ArrayUtils;
import org.openl.binding.impl.Operators;
import org.openl.meta.DoubleValue.DoubleValueAdapter;
import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.meta.number.CastOperand;
import org.openl.meta.number.Formulas;
import org.openl.meta.number.LogicalExpressions;
import org.openl.meta.number.NumberOperations;
import org.openl.util.ArrayTool;
import org.openl.util.math.MathUtils;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.math.BigDecimal;
import java.util.Arrays;

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

    // <<< INSERT Functions >>>
    private final double value;


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
        return Operators.eq(value1.getValue(), value2.getValue());
    }
    /**
     * Compares two values
     * @param value1
     * @param value2
     * @return true if  value1 greater or equal value2
     */
    public static boolean ge(org.openl.meta.DoubleValue value1, org.openl.meta.DoubleValue value2) {
        validate(value1, value2, LogicalExpressions.GE.toString());

        return Operators.ge(value1.getValue(), value2.getValue());
    }
    /**
     * Compares two values
     * @param value1
     * @param value2
     * @return true if  value1 greater value2
     */
    public static boolean gt(org.openl.meta.DoubleValue value1, org.openl.meta.DoubleValue value2) {
        validate(value1, value2, LogicalExpressions.GT.toString());

        return Operators.gt(value1.getValue(), value2.getValue());
    }
    /**
     * Compares two values
     * @param value1
     * @param value2
     * @return true if  value1 less or equal value2
     */
    public static boolean le(org.openl.meta.DoubleValue value1, org.openl.meta.DoubleValue value2) {
        validate(value1, value2, LogicalExpressions.LE.toString());

        return Operators.le(value1.getValue(), value2.getValue());
    }
    /**
     * Compares two values
     * @param value1
     * @param value2
     * @return true if  value1 less value2
     */
    public static boolean lt(org.openl.meta.DoubleValue value1, org.openl.meta.DoubleValue value2) {
        validate(value1, value2, LogicalExpressions.LT.toString());

        return Operators.lt(value1.getValue(), value2.getValue());
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

        return Operators.ne(value1.getValue(), value2.getValue());
    }

     /**
     * average
     * @param values  array of org.openl.meta.DoubleValue values
     * @return the average value from the array
     */
    public static org.openl.meta.DoubleValue avg(org.openl.meta.DoubleValue[] values) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        double[] primitiveArray = unwrap(values);
        double avg = MathUtils.avg(primitiveArray);
        return new org.openl.meta.DoubleValue(new org.openl.meta.DoubleValue(avg), NumberOperations.AVG, values);
    }
     /**
     * sum
     * @param values  array of org.openl.meta.DoubleValue values
     * @return the sum value from the array
     */
    public static org.openl.meta.DoubleValue sum(org.openl.meta.DoubleValue[] values) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        double[] primitiveArray = unwrap(values);
        double sum = MathUtils.sum(primitiveArray);
        return new org.openl.meta.DoubleValue(new org.openl.meta.DoubleValue(sum), NumberOperations.SUM, values);
    }
     /**
     * median
     * @param values  array of org.openl.meta.DoubleValue values
     * @return the median value from the array
     */
    public static org.openl.meta.DoubleValue median(org.openl.meta.DoubleValue[] values) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        double[] primitiveArray = unwrap(values);
        double median = MathUtils.median(primitiveArray);
        return new org.openl.meta.DoubleValue(new org.openl.meta.DoubleValue(median), NumberOperations.MEDIAN, values);
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
    public static DoubleValue add(DoubleValue value1, String value2) {
        if (value2 == null) {
            return value1;
        }

        if (value1 == null) {
            return new DoubleValue(Double.valueOf(value2));
        }
        
        double v = Double.valueOf(value2);

        return new org.openl.meta.DoubleValue(value1, new DoubleValue(v), Operators.add(value1.getValue(), v),
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
        
        return new org.openl.meta.DoubleValue(new DoubleValue(v), value2, Operators.add(v, value2.getValue()),
            Formulas.ADD);
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
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        double[] primitiveArray = unwrap(values);
        double product = MathUtils.product(primitiveArray);
        // we loose the parameters, but not the result of computation.
        return new DoubleValue(new DoubleValue(product), NumberOperations.PRODUCT, null);
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
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        double[] primitiveArray = unwrap(values);
        double small = MathUtils.small(primitiveArray, position);
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
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        double[] primitiveArray = unwrap(values);
        double big = MathUtils.big(primitiveArray, position);
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
    }

    /**Formula constructor**/
    public DoubleValue(org.openl.meta.DoubleValue lv1, org.openl.meta.DoubleValue lv2, double value, Formulas operand) {
        super(lv1, lv2, operand);
        this.value = value;
    }

    /**Cast constructor**/
    public DoubleValue(double value, ExplanationNumberValue<?> beforeCastValue, boolean autocast) {
        super(beforeCastValue, new CastOperand("DoubleValue", autocast));
        this.value = value;
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
        if (obj instanceof org.openl.meta.DoubleValue) {
            org.openl.meta.DoubleValue secondObj = (org.openl.meta.DoubleValue) obj;
            return Operators.eq(getValue(), secondObj.getValue());
        }

        // FIXME: Temporary fix for the commercial line to support
        // var == "0" where var is of type DoubleValue
        // In this case autocast should work, need investigation
        //
        if (obj instanceof String) {
            double d;
            try
            {
                d = Double.parseDouble((String)obj);
            }
            catch(NumberFormatException nfe)
            {
                return false;
            }

            return Operators.eq(getValue(), d);
        }

        return false;
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
            for (int i = 0; i < notNullArray.length; i++) {
                sortedArray[i] = notNullArray[i];
            }
        }
        return sortedArray;
    }
        // <<< END INSERT Functions >>>

    // ******* Autocasts *************

    public static DoubleValue autocast(Double x, DoubleValue y) {
        if (x == null) {
            return null;
        }

        return new DoubleValue(x);
    }

    public static BigDecimalValue autocast(DoubleValue x, BigDecimalValue y) {
        if (x == null) {
            return null;
        }
        return new BigDecimalValue(String.valueOf(x.getValue()), x, true);
    }

    public static String autocast(DoubleValue x, String y) {
        if (x == null) {
            return null;
        }

        return x.toString();
    }
    
    public static Integer distance(DoubleValue x, String y) {
        return 11;
    }

    public static DoubleValue autocast(String x, DoubleValue y) {
        if (x == null || "".equals(x)) {
            return null;
        }
        return new DoubleValue(Double.valueOf(x));
    }
    
    public static Integer distance(String x, DoubleValue y) {
        return 10;
    }

    // ******* Casts *************

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

    public static Double cast(DoubleValue x, Double y) {
        if (x == null) {
            return null;
        }

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

    public static org.openl.meta.DoubleValue round(org.openl.meta.DoubleValue value) {
        if (value == null) {
            return null;
        }

        // ULP is used for fix imprecise operations of double values
        double ulp = Math.ulp(value.getValue());
        return new org.openl.meta.DoubleValue(new org.openl.meta.DoubleValue((double) Math.round(value.getValue() + ulp)),
            NumberOperations.ROUND,
            new org.openl.meta.DoubleValue[] { value });
    }

    public static DoubleValue round(DoubleValue value, int scale) {
        if (value == null) {
            return null;
        }

        // ULP is used for fix imprecise operations of double values
        double ulp = Math.ulp(value.getValue());
        DoubleValue returnValue = new DoubleValue(new DoubleValue(MathUtils.round(value.doubleValue() + ulp,
                scale, BigDecimal.ROUND_HALF_UP)),
                NumberOperations.ROUND,
                new DoubleValue[] { value, new DoubleValue(scale) });

        return returnValue;
    }

    public static DoubleValue round(DoubleValue value, int scale, int roundingMethod) {
        if (value == null) {
            return null;
        }

        return new DoubleValue(new DoubleValue(MathUtils.round(value.doubleValue(),
            scale,
            roundingMethod)), NumberOperations.ROUND, new DoubleValue[] { value, new DoubleValue(scale) });
    }

    /**
     * 
     * @deprecated This method is obsolete. Use {@link #round(DoubleValue, int)}
     *             instead
     * @see #round(DoubleValue, int)
     */
    @Deprecated
    public static DoubleValue round(DoubleValue d, DoubleValue p) {
        validate(d, p, NumberOperations.ROUND);

        int scale;
        double preRoundedValue;

        if (p.doubleValue() == 0) {
            scale = 0;
            preRoundedValue = d.doubleValue();
        } else {
            scale = (int) MathUtils.round(-Math.log10(p.doubleValue()), 0, BigDecimal.ROUND_HALF_UP);
            preRoundedValue = d.doubleValue();
            // preRoundedValue = Math.round(d.doubleValue() / p.doubleValue()) *
            // p.doubleValue();
        }

        double roundedValue = MathUtils.round(preRoundedValue, scale, BigDecimal.ROUND_HALF_UP);

        return new DoubleValue(new DoubleValue(roundedValue), NumberOperations.ROUND, new DoubleValue[] { d, p });
    }

    public DoubleValue(String valueString) {
        super();
        value = Double.parseDouble(valueString);
    }

    /** Function constructor **/
    public DoubleValue(DoubleValue result, NumberOperations function, DoubleValue[] params) {
        super(function, params);
        this.value = result.doubleValue();
    }

    public int compareTo(Number o) {
        return Double.compare(value, (o).doubleValue());
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
        return ((Double) value).hashCode();
    }

    private static double[] unwrap(DoubleValue[] values) {
        values = ArrayTool.removeNulls(values);

        double[] primitiveArray = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            primitiveArray[i] = values[i].getValue();
        }
        return primitiveArray;
    }

}
