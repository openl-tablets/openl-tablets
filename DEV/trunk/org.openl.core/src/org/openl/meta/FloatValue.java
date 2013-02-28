package org.openl.meta;

import java.util.Arrays;

import org.apache.commons.lang.ArrayUtils;
import org.openl.binding.impl.Operators;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.meta.number.CastOperand;
import org.openl.meta.number.Formulas;
import org.openl.meta.number.LogicalExpressions;
import org.openl.meta.number.NumberOperations;
import org.openl.util.ArrayTool;
import org.openl.util.math.MathUtils;

public class FloatValue extends ExplanationNumberValue<FloatValue> {

    private static final long serialVersionUID = -8235832583740963916L;

    private static final FloatValue ZERO = new FloatValue((float) 0);
    private static final FloatValue ONE = new FloatValue((float) 1);
    private static final FloatValue MINUS_ONE = new FloatValue((float) -1);

    // <<< INSERT Functions >>>
    private float value;


    public static boolean eq(org.openl.meta.FloatValue value1, org.openl.meta.FloatValue value2) {
        validate(value1, value2, LogicalExpressions.EQ.toString());

        return Operators.eq(value1.getValue(), value2.getValue());
    }
    public static boolean ge(org.openl.meta.FloatValue value1, org.openl.meta.FloatValue value2) {
        validate(value1, value2, LogicalExpressions.GE.toString());

        return Operators.ge(value1.getValue(), value2.getValue());
    }
    public static boolean gt(org.openl.meta.FloatValue value1, org.openl.meta.FloatValue value2) {
        validate(value1, value2, LogicalExpressions.GT.toString());

        return Operators.gt(value1.getValue(), value2.getValue());
    }
    public static boolean le(org.openl.meta.FloatValue value1, org.openl.meta.FloatValue value2) {
        validate(value1, value2, LogicalExpressions.LE.toString());

        return Operators.le(value1.getValue(), value2.getValue());
    }
    public static boolean lt(org.openl.meta.FloatValue value1, org.openl.meta.FloatValue value2) {
        validate(value1, value2, LogicalExpressions.LT.toString());

        return Operators.lt(value1.getValue(), value2.getValue());
    }
    public static boolean ne(org.openl.meta.FloatValue value1, org.openl.meta.FloatValue value2) {
        validate(value1, value2, LogicalExpressions.NE.toString());

        return Operators.ne(value1.getValue(), value2.getValue());
    }

    public static org.openl.meta.FloatValue avg(org.openl.meta.FloatValue[] values) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        float[] primitiveArray = unwrap(values);
        float avg = MathUtils.avg(primitiveArray);
        return new org.openl.meta.FloatValue(new org.openl.meta.FloatValue(avg), NumberOperations.AVG, values);
    }
    public static org.openl.meta.FloatValue sum(org.openl.meta.FloatValue[] values) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        float[] primitiveArray = unwrap(values);
        float sum = MathUtils.sum(primitiveArray);
        return new org.openl.meta.FloatValue(new org.openl.meta.FloatValue(sum), NumberOperations.SUM, values);
    }
    public static org.openl.meta.FloatValue median(org.openl.meta.FloatValue[] values) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        float[] primitiveArray = unwrap(values);
        float median = MathUtils.median(primitiveArray);
        return new org.openl.meta.FloatValue(new org.openl.meta.FloatValue(median), NumberOperations.MEDIAN, values);
    }

    public static org.openl.meta.FloatValue max(org.openl.meta.FloatValue value1, org.openl.meta.FloatValue value2) {
        // Commented to support operations with nulls
        // "null" means that data does not exist
        // validate(value1, value2, NumberOperations.MAX.toString());
        if (value1 == null)
            return value2; 
        if (value2 == null)
            return value1; 

        return new org.openl.meta.FloatValue(MathUtils.max(value1.getValue(), value2.getValue()) ? value1 : value2,
            NumberOperations.MAX,
            new org.openl.meta.FloatValue[] { value1, value2 });
    }
    public static org.openl.meta.FloatValue min(org.openl.meta.FloatValue value1, org.openl.meta.FloatValue value2) {
        // Commented to support operations with nulls
        // "null" means that data does not exist
        // validate(value1, value2, NumberOperations.MIN.toString());
        if (value1 == null)
            return value2; 
        if (value2 == null)
            return value1; 

        return new org.openl.meta.FloatValue(MathUtils.min(value1.getValue(), value2.getValue()) ? value1 : value2,
            NumberOperations.MIN,
            new org.openl.meta.FloatValue[] { value1, value2 });
    }

    public static org.openl.meta.FloatValue max(org.openl.meta.FloatValue[] values) {
        org.openl.meta.FloatValue result = (org.openl.meta.FloatValue) MathUtils.max(values);

        return new org.openl.meta.FloatValue((org.openl.meta.FloatValue) getAppropriateValue(values, result),
            NumberOperations.MAX_IN_ARRAY, values);
    }
    public static org.openl.meta.FloatValue min(org.openl.meta.FloatValue[] values) {
        org.openl.meta.FloatValue result = (org.openl.meta.FloatValue) MathUtils.min(values);

        return new org.openl.meta.FloatValue((org.openl.meta.FloatValue) getAppropriateValue(values, result),
            NumberOperations.MIN_IN_ARRAY, values);
    }

    public static org.openl.meta.FloatValue copy(org.openl.meta.FloatValue value, String name) {
        if (value.getName() == null) {
            value.setName(name);

            return value;
        } else if (!value.getName().equals(name)) {
            org.openl.meta.FloatValue result = new org.openl.meta.FloatValue (value, NumberOperations.COPY, 
                new org.openl.meta.FloatValue[] { value });
            result.setName(name);

            return result;
        }
        return value;
    }

    //REM
    public static org.openl.meta.FloatValue rem(org.openl.meta.FloatValue value1, org.openl.meta.FloatValue value2) {
        // Commented to support operations with nulls. See also MathUtils.mod()
        // validate(value1, value2, Formulas.REM.toString());
        if (value1 == null || value2 == null) {
            return ZERO;
        }

        return new org.openl.meta.FloatValue(value1, value2, Operators.rem(value1.getValue(), value2.getValue()),
            Formulas.REM);
    }

    //ADD
    public static org.openl.meta.FloatValue add(org.openl.meta.FloatValue value1, org.openl.meta.FloatValue value2) {
        // temporary commented to support operations with nulls
        //
        //        validate(value1, value2, Formulas.ADD.toString());
        //conditions for classes that are wrappers over primitives
        if (value1 == null || value1.getValue() == 0) {
            return value2;
        }

        if (value2 == null || value2.getValue() == 0) {
            return value1;
        }

        return new org.openl.meta.FloatValue(value1, value2, Operators.add(value1.getValue(), value2.getValue()),
            Formulas.ADD);
}

    // MULTIPLY
    public static org.openl.meta.FloatValue multiply(org.openl.meta.FloatValue value1, org.openl.meta.FloatValue value2) {
        // temporary commented to support operations with nulls
        //
        //        validate(value1, value2, Formulas.MULTIPLY.toString());
        if (value1 == null) {
            return value2;
        }

        if (value2 == null) {
            return value1;
        }

        return new org.openl.meta.FloatValue(value1, value2, Operators.multiply(value1.getValue(), value2.getValue()),
            Formulas.MULTIPLY);
    }

    //SUBTRACT
    public static org.openl.meta.FloatValue subtract(org.openl.meta.FloatValue value1, org.openl.meta.FloatValue value2) {
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

        return new org.openl.meta.FloatValue(value1, value2, Operators.subtract(value1.getValue(), value2.getValue()), 
            Formulas.SUBTRACT);
    }

    // DIVIDE
    public static org.openl.meta.FloatValue divide(org.openl.meta.FloatValue value1, org.openl.meta.FloatValue value2) {
        // temporary commented to support operations with nulls
        //
        //        validate(value1, value2, Formulas.DIVIDE.toString());
        if (value1 == null && value2 == null) {
            return null;
        }

        if (value1 == null) {
            if (value2 != null && value2.doubleValue() != 0) {
                return new org.openl.meta.FloatValue(value1, value2, divide(ONE, value2).getValue(), Formulas.DIVIDE);
            }
        }

        if (value2 == null) {
            return new org.openl.meta.FloatValue(value1, value2, value1.getValue(), Formulas.DIVIDE);
        }

        if (value2.doubleValue() == 0) {
            throw new OpenlNotCheckedException("Division by zero");
        }

        return new org.openl.meta.FloatValue(value1, value2, Operators.divide(value1.getValue(), value2.getValue()),
            Formulas.DIVIDE);
    }

    // QUAOTIENT
    public static LongValue quotient(org.openl.meta.FloatValue number, org.openl.meta.FloatValue divisor) {
        if (number != null && divisor != null) {
            LongValue result = new LongValue(MathUtils.quotient(number.getValue(), divisor.getValue()));
            return new LongValue(result, NumberOperations.QUOTIENT, null);
        }
        return null;
    }

    // generated product function for types that are wrappers over primitives
    public static DoubleValue product(org.openl.meta.FloatValue[] values) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        float[] primitiveArray = unwrap(values);
        double product = MathUtils.product(primitiveArray);
        // we loose the parameters, but not the result of computation.
        return new DoubleValue(new DoubleValue(product), NumberOperations.PRODUCT, null);
    }

    public static org.openl.meta.FloatValue mod(org.openl.meta.FloatValue number, org.openl.meta.FloatValue divisor) {
        if (number != null && divisor != null) {
            org.openl.meta.FloatValue result = new org.openl.meta.FloatValue(MathUtils.mod(number.getValue(), divisor.getValue()));
            return new org.openl.meta.FloatValue(result, NumberOperations.MOD, new org.openl.meta.FloatValue[]{number, divisor} );
        }
        return null;
    }

    public static org.openl.meta.FloatValue small(org.openl.meta.FloatValue[] values, int position) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        float[] primitiveArray = unwrap(values);
        float small = MathUtils.small(primitiveArray, position);
        return new org.openl.meta.FloatValue((org.openl.meta.FloatValue) getAppropriateValue(values, new org.openl.meta.FloatValue(small)), 
            NumberOperations.SMALL, values);
    }

    public static org.openl.meta.FloatValue big(org.openl.meta.FloatValue[] values, int position) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        float[] primitiveArray = unwrap(values);
        float big = MathUtils.big(primitiveArray, position);
        return new org.openl.meta.FloatValue((org.openl.meta.FloatValue) getAppropriateValue(values, new org.openl.meta.FloatValue(big)),
            NumberOperations.BIG, values);
    }

    public static org.openl.meta.FloatValue pow(org.openl.meta.FloatValue value1, org.openl.meta.FloatValue value2) {
        // Commented to support operations with nulls
        // "null" means that data does not exist
        //
        // validate(value1, value2, NumberOperations.POW);
        if (value1 == null) {
            return value2 == null ? null : new org.openl.meta.FloatValue((float) 0);
        } else if (value2 == null) {
            return value1;
        }

        return new org.openl.meta.FloatValue(new org.openl.meta.FloatValue(Operators.pow(value1.getValue(), value2.getValue())), 
            NumberOperations.POW, new org.openl.meta.FloatValue[] { value1, value2 });
    }

    public static org.openl.meta.FloatValue abs(org.openl.meta.FloatValue value) {
        // Commented to support operations with nulls.
        // validate(value, NumberOperations.ABS);
        if (value == null) {
            return null;
        }
        // evaluate result
        org.openl.meta.FloatValue result = new org.openl.meta.FloatValue(Operators.abs(value.getValue()));
        // create instance with information about last operation
        return new org.openl.meta.FloatValue(result, NumberOperations.ABS, new org.openl.meta.FloatValue[] { value });
    }

    public static org.openl.meta.FloatValue negative(org.openl.meta.FloatValue value) {
        if (value == null) {
            return null;
        }
        return multiply(value, MINUS_ONE);
    }

    public static org.openl.meta.FloatValue inc(org.openl.meta.FloatValue value) {
        return add(value, ONE);
    }

    public static org.openl.meta.FloatValue positive(org.openl.meta.FloatValue value) {
        return value;
    }

    public static org.openl.meta.FloatValue dec(org.openl.meta.FloatValue value) {
        return subtract(value, ONE);
    }

    // Autocasts

    public static org.openl.meta.FloatValue autocast(byte x, org.openl.meta.FloatValue y) {
        return new org.openl.meta.FloatValue((float) x);
    }
    public static org.openl.meta.FloatValue autocast(short x, org.openl.meta.FloatValue y) {
        return new org.openl.meta.FloatValue((float) x);
    }
    public static org.openl.meta.FloatValue autocast(int x, org.openl.meta.FloatValue y) {
        return new org.openl.meta.FloatValue((float) x);
    }
    public static org.openl.meta.FloatValue autocast(long x, org.openl.meta.FloatValue y) {
        return new org.openl.meta.FloatValue((float) x);
    }
    public static org.openl.meta.FloatValue autocast(float x, org.openl.meta.FloatValue y) {
        return new org.openl.meta.FloatValue((float) x);
    }
    public static org.openl.meta.FloatValue autocast(double x, org.openl.meta.FloatValue y) {
        return new org.openl.meta.FloatValue((float) x);
    }

    // Constructors
    public FloatValue(float value) {
        this.value = value;
    }

    public FloatValue(float value, String name) {
        super(name);
        this.value = value;
    }

    public FloatValue(float value, IMetaInfo metaInfo) {
        super(metaInfo);
        this.value = value;
    }

    /**Formula constructor**/
    public FloatValue(org.openl.meta.FloatValue lv1, org.openl.meta.FloatValue lv2, float value, Formulas operand) {
        super(lv1, lv2, operand);
        this.value = value;
    }

    /**Cast constructor**/
    public FloatValue(float value, ExplanationNumberValue<?> beforeCastValue, boolean autocast) {
        super(beforeCastValue, new CastOperand("FloatValue", autocast));
        this.value = value;
    }

    @Override
    public org.openl.meta.FloatValue copy(String name) {
        return copy(this, name);
    }

    public String printValue() {
        return String.valueOf(value);
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    //Equals
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof org.openl.meta.FloatValue) {
            org.openl.meta.FloatValue secondObj = (org.openl.meta.FloatValue) obj;
            return Operators.eq(getValue(), secondObj.getValue());
        }

        return false;
    }

    // sort
    public static org.openl.meta.FloatValue[] sort (org.openl.meta.FloatValue[] values ) {
        org.openl.meta.FloatValue[] sortedArray = null;
        if (values != null) {
            sortedArray = new org.openl.meta.FloatValue[values.length];
           org.openl.meta.FloatValue[] notNullArray = ArrayTool.removeNulls(values);

            Arrays.sort(notNullArray);

            /* Filling sortedArray by sorted and null values */
            for (int i = 0; i < notNullArray.length; i++) {
                sortedArray[i] = notNullArray[i];
            }
        }
        return sortedArray;
    }
        // <<< END INSERT Functions >>>

    // ******* Autocasts*************

    public static FloatValue autocast(Float x, FloatValue y) {
        if (x == null) {
            return null;
        }

        return new FloatValue(x);
    }

    public static DoubleValue autocast(FloatValue x, DoubleValue y) {
        if (x == null) {
            return null;
        }

        return new DoubleValue(x.getValue(), x, true);
    }

    public static BigDecimalValue autocast(FloatValue x, BigDecimalValue y) {
        if (x == null) {
            return null;
        }
        return new BigDecimalValue(String.valueOf(x.getValue()), x, true);
    }

    // ******* Casts *************

    public static byte cast(FloatValue x, byte y) {
        return x.byteValue();
    }

    public static short cast(FloatValue x, short y) {
        return x.shortValue();
    }

    public static char cast(FloatValue x, char y) {
        return (char) x.floatValue();
    }

    public static int cast(FloatValue x, int y) {
        return x.intValue();
    }

    public static long cast(FloatValue x, long y) {
        return x.longValue();
    }

    public static float cast(FloatValue x, float y) {
        return x.floatValue();
    }

    public static double cast(FloatValue x, double y) {
        return x.doubleValue();
    }

    public static Float cast(FloatValue x, Float y) {
        if (x == null) {
            return null;
        }

        return x.floatValue();
    }

    public static ByteValue cast(FloatValue x, ByteValue y) {
        if (x == null) {
            return null;
        }
        return new ByteValue(x.byteValue(), x, false);
    }

    public static ShortValue cast(FloatValue x, ShortValue y) {
        if (x == null) {
            return null;
        }
        return new ShortValue(x.shortValue(), x, false);
    }

    public static IntValue cast(FloatValue x, IntValue y) {
        if (x == null) {
            return null;
        }
        return new IntValue(x.intValue(), x, false);
    }

    public static LongValue cast(FloatValue x, LongValue y) {
        if (x == null) {
            return null;
        }
        return new LongValue(x.longValue(), x, false);
    }

    public static org.openl.meta.FloatValue round(org.openl.meta.FloatValue value) {
        validate(value, NumberOperations.ROUND);
        // ULP is used for fix imprecise operations of float values
        float ulp = Math.ulp(value.getValue());
        return new org.openl.meta.FloatValue(new org.openl.meta.FloatValue((float) Math.round(value.getValue() + ulp)),
            NumberOperations.ROUND,
            new org.openl.meta.FloatValue[] { value });
    }

    public static FloatValue round(FloatValue value, int scale) {
        // ULP is used for fix imprecise operations of float values
        float ulp = Math.ulp(value.getValue());
        return new FloatValue(new FloatValue(org.apache.commons.math.util.MathUtils.round(value.floatValue() + ulp,
            scale)), NumberOperations.ROUND, new FloatValue[] { value, new FloatValue(scale) });
    }

    public static FloatValue round(FloatValue value, int scale, int roundingMethod) {
        return new FloatValue(new FloatValue(org.apache.commons.math.util.MathUtils.round(value.floatValue(),
            scale,
            roundingMethod)), NumberOperations.ROUND, new FloatValue[] { value, new FloatValue(scale) });
    }

    public FloatValue(String valueString) {
        value = Float.parseFloat(valueString);
    }

    /** Function constructor **/
    public FloatValue(FloatValue result, NumberOperations function, FloatValue[] params) {
        super(result, function, params);
        this.value = result.floatValue();
    }

    @Override
    public double doubleValue() {
        return (double) value;
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
        return (long) value;
    }

    public int compareTo(Number o) {
        return Float.compare(value, o.floatValue());
    }

    @Override
    public int hashCode() {
        return ((Float) value).hashCode();
    }

    private static float[] unwrap(FloatValue[] values) {
        values = ArrayTool.removeNulls(values);

        float[] primitiveArray = new float[values.length];
        for (int i = 0; i < values.length; i++) {
            primitiveArray[i] = values[i].getValue();
        }
        return primitiveArray;
    }

}
