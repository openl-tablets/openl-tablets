package org.openl.meta;

import org.apache.commons.lang.ArrayUtils;
import org.openl.binding.impl.Operators;
import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.meta.number.NumberOperations;
import org.openl.util.ArrayTool;
import org.openl.util.math.MathUtils;

public class FloatValue extends ExplanationNumberValue<FloatValue> {

    private static final long serialVersionUID = -8235832583740963916L;
    
    private float value;
    
    public static FloatValue add(FloatValue floatValue1, FloatValue floatValue2) {
        validate(floatValue1, floatValue2, NumberOperations.ADD);
        
        return new FloatValue(floatValue1, floatValue2, Operators.add(floatValue1.getValue(), floatValue2.getValue()), 
            NumberOperations.ADD.toString(), false);
    }    
    
    public static FloatValue rem(FloatValue floatValue1, FloatValue floatValue2) {
        validate(floatValue1, floatValue2, NumberOperations.REM);
        
        return new FloatValue(floatValue1, floatValue2, Operators.rem(floatValue1.getValue(), floatValue2.getValue()), 
            NumberOperations.REM.toString(), true);
    }
    
    // ******* Autocasts*************
    
    public static FloatValue autocast(byte x, FloatValue y) {
        return new FloatValue(x);
    }

    public static FloatValue autocast(short x, FloatValue y) {
        return new FloatValue(x);
    }
    
    public static FloatValue autocast(char x, FloatValue y) {
        return new FloatValue(x);    
    }

    public static FloatValue autocast(int x, FloatValue y) {
        return new FloatValue(x);
    }

    public static FloatValue autocast(long x, FloatValue y) {
        return new FloatValue(x);
    }

    public static FloatValue autocast(float x, FloatValue y) {
        return new FloatValue(x);
    }

    public static FloatValue autocast(double x, FloatValue y) {
        return new FloatValue((float)x);
    }

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

        return new DoubleValue(x.getValue());
    }
    
    public static BigDecimalValue autocast(FloatValue x, BigDecimalValue y) {
        if (x == null) {
            return null;
        }
        return new BigDecimalValue(String.valueOf(x.getValue()));
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
        return new ByteValue(x.byteValue());
    }

    public static ShortValue cast(FloatValue x, ShortValue y) {
        if (x == null) {
            return null;
        }
        return new ShortValue(x.shortValue());
    }
        
    public static IntValue cast(FloatValue x, IntValue y) {
        if (x == null) {
            return null;
        }
        return new IntValue(x.intValue());
    }

    public static LongValue cast(FloatValue x, LongValue y) {
        if (x == null) {
            return null;
        }
        return new LongValue(x.longValue());
    }

    public static FloatValue copy(FloatValue value, String name) {

        if (value.getName() == null) {
            value.setName(name);

            return value;
        } else if (!value.getName().equals(name)) {
            FloatValue lv = new FloatValue(value, NumberOperations.COPY.toString(), new FloatValue[] { value });
            lv.setName(name);

            return lv;
        }

        return value;
    }

    public static FloatValue divide(FloatValue floatValue1, FloatValue floatValue2) {
        validate(floatValue1, floatValue2, NumberOperations.DIVIDE);
        
        return new FloatValue(floatValue1, floatValue2, Operators.divide(floatValue1.getValue(), floatValue2.getValue()), 
            NumberOperations.DIVIDE.toString(), true);
    }

    public static boolean eq(FloatValue floatValue1, FloatValue floatValue2) {
        validate(floatValue1, floatValue2, NumberOperations.EQ);
        
        return Operators.eq(floatValue1.getValue(), floatValue2.getValue());
    }

    public static boolean ge(FloatValue floatValue1, FloatValue floatValue2) {
        validate(floatValue1, floatValue2, NumberOperations.GE);
        
        return Operators.ge(floatValue1.getValue(), floatValue2.getValue());
    }

    public static boolean gt(FloatValue floatValue1, FloatValue floatValue2) {
        validate(floatValue1, floatValue2, NumberOperations.GT);
        
        return Operators.gt(floatValue1.getValue(), floatValue2.getValue());
    }

    public static boolean le(FloatValue floatValue1, FloatValue floatValue2) {
        validate(floatValue1, floatValue2, NumberOperations.LE);
        
        return Operators.le(floatValue1.getValue(), floatValue2.getValue());
    }

    public static boolean lt(FloatValue floatValue1, FloatValue floatValue2) {
        validate(floatValue1, floatValue2, NumberOperations.LT);
        
        return Operators.lt(floatValue1.getValue(), floatValue2.getValue());
    }

    public static FloatValue max(FloatValue floatValue1, FloatValue floatValue2) {
        validate(floatValue1, floatValue2, NumberOperations.MAX);
        
        return new FloatValue(floatValue2.getValue() > floatValue1.getValue() ? floatValue2 : floatValue1,
            NumberOperations.MAX.toString(),
            new FloatValue[] { floatValue1, floatValue2 });
    }

    public static FloatValue min(FloatValue floatValue1, FloatValue floatValue2) {
        validate(floatValue1, floatValue2, NumberOperations.MIN);
        
        return new FloatValue(floatValue2.getValue() < floatValue1.getValue() ? floatValue2 : floatValue1,
            NumberOperations.MIN.toString(),
            new FloatValue[] { floatValue1, floatValue2 });
    }

    public static FloatValue multiply(FloatValue floatValue1, FloatValue floatValue2) {
        validate(floatValue1, floatValue2, NumberOperations.MULTIPLY);
        
        return new FloatValue(floatValue1, floatValue2, Operators.multiply(floatValue1.getValue(), floatValue2.getValue()), 
            NumberOperations.MULTIPLY.toString(), true);
    }

    public static boolean ne(FloatValue floatValue1, FloatValue floatValue2) {
        validate(floatValue1, floatValue2, NumberOperations.NE);
        
        return Operators.ne(floatValue1.getValue(), floatValue2.getValue());
    }

    public static FloatValue negative(FloatValue value) {
        return multiply(value, new FloatValue(-1F));
    }
    
    public static FloatValue pow(FloatValue floatValue1, FloatValue floatValue2) {
        validate(floatValue1, floatValue2, NumberOperations.POW);
        
        return new FloatValue(new FloatValue(Operators.pow(floatValue1.getValue(), floatValue2.getValue())), 
            NumberOperations.POW.toString(), new FloatValue[] { floatValue1, floatValue2 });
    }

    public static FloatValue round(FloatValue floatValue1) {
        validate(floatValue1, NumberOperations.ROUND);
        
        return new FloatValue(new FloatValue((float)Math.round(floatValue1.getValue())), 
            NumberOperations.ROUND.toString(), new FloatValue[] { floatValue1 });
    }

    public static FloatValue subtract(FloatValue floatValue1, FloatValue floatValue2) {
        validate(floatValue1, floatValue2, NumberOperations.SUBTRACT);
        
        return new FloatValue(floatValue1, floatValue2, Operators.subtract(floatValue1.getValue(), floatValue2.getValue()), 
            NumberOperations.SUBTRACT.toString(), false);
    }
    
    // Math functions
    
    public static FloatValue max(FloatValue[] values) {
        FloatValue result = (FloatValue) MathUtils.max(values);        
        return new FloatValue((FloatValue) getAppropriateValue(values, result), NumberOperations.MAX_IN_ARRAY.toString(), values);
    }

    public static FloatValue min(FloatValue[] values) {
        FloatValue result = (FloatValue) MathUtils.min(values);
        return new FloatValue((FloatValue) getAppropriateValue(values, result), NumberOperations.MIN_IN_ARRAY.toString(), values);
    }
    
    public static FloatValue avg(FloatValue[] values) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        float[] primitiveArray = floatValueArrayToFloat(values);
        float avg = MathUtils.avg(primitiveArray);
        
        return new FloatValue(new FloatValue(avg), NumberOperations.AVG.toString(), values);
    }
    
    public static FloatValue sum(FloatValue[] values) {  
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        float[] primitiveArray = floatValueArrayToFloat(values);
        float sum = MathUtils.sum(primitiveArray);
        return new FloatValue(new FloatValue(sum), NumberOperations.SUM.toString(), values);
    }
    
    public static FloatValue median(FloatValue[] values) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        float[] primitiveArray = floatValueArrayToFloat(values);
        float median = MathUtils.median(primitiveArray);
        return new FloatValue(new FloatValue(median), NumberOperations.MEDIAN.toString(), values);
    }
    
    public static DoubleValue product(FloatValue[] values) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        float[] primitiveArray = floatValueArrayToFloat(values);
        double product = MathUtils.product(primitiveArray);
        return new DoubleValue(new DoubleValue(product), NumberOperations.PRODUCT.toString(), null);
    }
    
    public static LongValue quaotient(FloatValue number, FloatValue divisor) {
        if (number != null && divisor != null) {
            LongValue result = new LongValue(MathUtils.quaotient(number.getValue(), divisor.getValue()));
            return new LongValue(result, NumberOperations.QUAOTIENT.toString(), null);
        }
        return null;
    }
    
    public static FloatValue mod(FloatValue number, FloatValue divisor) {
        if (number != null && divisor != null) {
            FloatValue result = new FloatValue(MathUtils.mod(number.getValue(), divisor.getValue()));
            return new FloatValue(result, NumberOperations.MOD.toString(), new FloatValue[]{number, divisor} );
        }
        return null;
    }
    
    public static FloatValue small(FloatValue[] values, int position) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        float[] primitiveArray = floatValueArrayToFloat(values);
        float small = MathUtils.small(primitiveArray, position);
        return new FloatValue((FloatValue) getAppropriateValue(values, new FloatValue(small)), NumberOperations.SMALL.toString(), values);
    }
    
    public FloatValue(float value) {
        this.value = value;
    }
    
    public FloatValue(String valueString) {        
        value = Float.parseFloat(valueString);
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
    public FloatValue(FloatValue floatValue1, FloatValue floatValue2, float value, String operand, boolean isMultiplicative) {
        super(floatValue1, floatValue2, operand, isMultiplicative);
        this.value = value;
    }
    
    /**Function constructor**/
    public FloatValue(FloatValue result, String functionName, FloatValue[] params) {
        super(result, functionName, params);
        this.value = result.floatValue();
    }

    @Override
    public FloatValue copy(String name) {        
        return copy(this, name);
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
        return (int)value;
    }

    @Override
    public long longValue() {
        return (long)value;
    }

    public String printValue() {
        return String.valueOf(value);
    }

    public int compareTo(Number o) {        
        return Float.compare(value, o.floatValue());
    }
    
    public static FloatValue abs(FloatValue value) {
        // evaluate result
        FloatValue result = new FloatValue(Math.abs(value.getValue()));
        // create instance with information about last operation
        return new FloatValue(result, NumberOperations.ABS.toString(), new FloatValue[] { value });
    }
    
    public static FloatValue inc(FloatValue value) {
        return add(value, new FloatValue(1F));
    }
    
    public static FloatValue dec(FloatValue value) {
        return subtract(value, new FloatValue(1F));
    }
    
    public float getValue() {        
        return value;
    }
    
    public void setValue(float value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FloatValue) {
            FloatValue secondObj = (FloatValue) obj;
            return value == secondObj.floatValue();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return ((Float) value).hashCode();
    }

    public static FloatValue positive(FloatValue value) {
        return value;
    }
    
    private static float[] floatValueArrayToFloat(FloatValue[] values) {
        if (ArrayTool.noNulls(values)) {
            float[] primitiveArray = new float[values.length];
            for (int i = 0; i < values.length; i++) {
                primitiveArray[i] = values[i].getValue();
            }
            return primitiveArray;
        }
        return ArrayUtils.EMPTY_FLOAT_ARRAY;
    }

}
