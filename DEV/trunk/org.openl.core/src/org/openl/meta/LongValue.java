package org.openl.meta;

import org.apache.commons.lang.ArrayUtils;
import org.openl.binding.impl.Operators;
import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.meta.number.NumberOperations;
import org.openl.util.ArrayTool;
import org.openl.util.math.MathUtils;

public class LongValue extends ExplanationNumberValue<LongValue> {
   
    private static final long serialVersionUID = -437788531108803012L;
    private long value;

    public static LongValue add(LongValue lv1, LongValue lv2) {
        validate(lv1, lv2, NumberOperations.ADD);
        
        return new LongValue(lv1, lv2, Operators.add(lv1.getValue(), lv2.getValue()), 
            NumberOperations.ADD.toString(), false);
    }
    
    public static LongValue rem(LongValue lv1, LongValue lv2) {
        validate(lv1, lv2, NumberOperations.REM);
        
        return new LongValue(lv1, lv2, Operators.rem(lv1.getValue(), lv2.getValue()), 
            NumberOperations.REM.toString(), true);
    }
    
    // ******* Autocasts*************
    
    public static LongValue autocast(byte x, LongValue y) {
        return new LongValue(x);
    }

    public static LongValue autocast(short x, LongValue y) {
        return new LongValue(x);
    }
    
    public static LongValue autocast(char x, LongValue y) {
        return new LongValue(x);    
    }

    public static LongValue autocast(int x, LongValue y) {
        return new LongValue(x);
    }

    public static LongValue autocast(long x, LongValue y) {
        return new LongValue(x);
    }

    public static LongValue autocast(float x, LongValue y) {
        return new LongValue((long)x);
    }

    public static LongValue autocast(double x, LongValue y) {
        return new LongValue((long)x);
    }

    public static LongValue autocast(Long x, LongValue y) {
        if (x == null) {
            return null;
        }

        return new LongValue(x);
    }
    
    public static FloatValue autocast(LongValue x, FloatValue y) {
        if (x == null) {
            return null;
        }

        return new FloatValue(x.getValue());
    }
    
    public static DoubleValue autocast(LongValue x, DoubleValue y) {
        if (x == null) {
            return null;
        }

        return new DoubleValue(x.getValue());
    }
    
    public static BigIntegerValue autocast(LongValue x, BigIntegerValue y) {
        if (x == null) {
            return null;
        }
        return new BigIntegerValue(String.valueOf(x.getValue()));
    }
    
    public static BigDecimalValue autocast(LongValue x, BigDecimalValue y) {
        if (x == null) {
            return null;
        }
        return new BigDecimalValue(String.valueOf(x.getValue()));
    }
    
    // ******* Casts *************

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

    public static Long cast(LongValue x, Long y) {
        if (x == null) {
            return null;
        }

        return x.longValue();
    }
    
    public static ByteValue cast(LongValue x, ByteValue y) {
        if (x == null) {
            return null;
        }
        return new ByteValue(x.byteValue());
    }
        
    public static ShortValue cast(LongValue x, ShortValue y) {
        if (x == null) {
            return null;
        }
        return new ShortValue(x.shortValue());
    }

    public static IntValue cast(LongValue x, IntValue y) {
        if (x == null) {
            return null;
        }
        return new IntValue(x.intValue());
    }
    
    public static LongValue copy(LongValue value, String name) {

        if (value.getName() == null) {
            value.setName(name);

            return value;
        } else if (!value.getName().equals(name)) {
            LongValue lv = new LongValue(value, NumberOperations.COPY.toString(), new LongValue[] { value });
            lv.setName(name);

            return lv;
        }

        return value;
    }

    public static LongValue divide(LongValue lv1, LongValue lv2) {
        validate(lv1, lv2, NumberOperations.DIVIDE);
        
        return new LongValue(lv1, lv2, Operators.divide(lv1.getValue(), lv2.getValue()), 
            NumberOperations.DIVIDE.toString(), true);
    }

    public static boolean eq(LongValue lv1, LongValue lv2) {
        validate(lv1, lv2, NumberOperations.EQ);
        
        return Operators.eq(lv1.getValue(), lv2.getValue());
    }

    public static boolean ge(LongValue lv1, LongValue lv2) {
        validate(lv1, lv2, NumberOperations.GE);
        
        return Operators.ge(lv1.getValue(), lv2.getValue());
    }

    public static boolean gt(LongValue lv1, LongValue lv2) {
        validate(lv1, lv2, NumberOperations.GT);
        
        return Operators.gt(lv1.getValue(), lv2.getValue());
    }

    public static boolean le(LongValue lv1, LongValue lv2) {
        validate(lv1, lv2, NumberOperations.LE);
        
        return Operators.le(lv1.getValue(), lv2.getValue());
    }

    public static boolean lt(LongValue lv1, LongValue lv2) {
        validate(lv1, lv2, NumberOperations.LT);
        
        return Operators.lt(lv1.getValue(), lv2.getValue());
    }

    public static LongValue max(LongValue lv1, LongValue lv2) {
        validate(lv1, lv2, NumberOperations.MAX);
        
        return new LongValue(lv2.getValue() > lv1.getValue() ? lv2 : lv1, NumberOperations.MAX.toString(),
            new LongValue[] { lv1, lv2 });
    }

    public static LongValue min(LongValue lv1, LongValue lv2) {
        validate(lv1, lv2, NumberOperations.MIN);
        
        return new LongValue(lv2.getValue() < lv1.getValue() ? lv2 : lv1, NumberOperations.MIN.toString(),
            new LongValue[] { lv1, lv2 });
    }

    public static LongValue multiply(LongValue lv1, LongValue lv2) {
        validate(lv1, lv2, NumberOperations.MULTIPLY);
        
        return new LongValue(lv1, lv2, Operators.multiply(lv1.getValue(), lv2.getValue()), 
            NumberOperations.MULTIPLY.toString(), true);
    }

    public static boolean ne(LongValue lv1, LongValue lv2) {
        validate(lv1, lv2, NumberOperations.NE);
        
        return Operators.ne(lv1.getValue(), lv2.getValue());
    }

    public static LongValue negative(LongValue value) {
        return multiply(value, new LongValue(-1));
    }

    public static LongValue pow(LongValue lv1, LongValue lv2) {
        validate(lv1, lv2, NumberOperations.POW);
        
        return new LongValue(new LongValue(Operators.pow(lv1.getValue(), lv2.getValue())), 
            NumberOperations.POW.toString(), new LongValue[] { lv1, lv2 });
    }

    public static LongValue round(LongValue lv1) {
        validate(lv1, NumberOperations.ROUND);
        
        return new LongValue(new LongValue((long)Math.round(lv1.getValue())), 
            NumberOperations.ROUND.toString(), new LongValue[] { lv1 });
    }

    public static LongValue subtract(LongValue lv1, LongValue lv2) {
        validate(lv1, lv2, NumberOperations.SUBTRACT);
        
        return new LongValue(lv1, lv2, Operators.subtract(lv1.getValue(), lv2.getValue()), 
            NumberOperations.SUBTRACT.toString(), false);
    }
    
    // Math functions
    
    public static LongValue max(LongValue[] values) {
        LongValue result = (LongValue) MathUtils.max(values);        
        return new LongValue((LongValue) getAppropriateValue(values, result), NumberOperations.MAX_IN_ARRAY.toString(), 
            values);
    }

    public static LongValue min(LongValue[] values) {
        LongValue result = (LongValue) MathUtils.min(values);
        return new LongValue((LongValue) getAppropriateValue(values, result), NumberOperations.MIN_IN_ARRAY.toString(), 
            values);
    }
    
    public static LongValue avg(LongValue[] values) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        long[] primitiveArray = longValueArrayToLong(values);
        long avg = MathUtils.avg(primitiveArray);
        
        return new LongValue(new LongValue(avg), NumberOperations.AVG.toString(), values);
    }
    
    public static LongValue sum(LongValue[] values) {       
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        long[] primitiveArray = longValueArrayToLong(values);
        long sum = MathUtils.sum(primitiveArray);
        return new LongValue(new LongValue(sum), NumberOperations.SUM.toString(), values);
    }
    
    public static LongValue median(LongValue[] values) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        long[] primitiveArray = longValueArrayToLong(values);
        long median = MathUtils.median(primitiveArray);
        return new LongValue(new LongValue(median), NumberOperations.MEDIAN.toString(), values);
    }
    
    public static DoubleValue product(LongValue[] values) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        long[] primitiveArray = longValueArrayToLong(values);
        double product = MathUtils.product(primitiveArray);
        return new DoubleValue(new DoubleValue(product), NumberOperations.PRODUCT.toString(), null);
    }
    
    public static LongValue quaotient(LongValue number, LongValue divisor) {
        if (number != null && divisor != null) {
            LongValue result = new LongValue(MathUtils.quaotient(number.getValue(), divisor.getValue()));
            return new LongValue(result, NumberOperations.QUAOTIENT.toString(), new LongValue[]{number, divisor} );
        }
        return null;
    }
    
    public static LongValue mod(LongValue number, LongValue divisor) {
        if (number != null && divisor != null) {
            LongValue result = new LongValue(MathUtils.mod(number.getValue(), divisor.getValue()));
            return new LongValue(result, NumberOperations.MOD.toString(), new LongValue[]{number, divisor} );
        }
        return null;
    }
    
    public static LongValue small(LongValue[] values, int position) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        long[] primitiveArray = longValueArrayToLong(values);
        long small = MathUtils.small(primitiveArray, position);
        return new LongValue((LongValue) getAppropriateValue(values, new LongValue(small)), 
            NumberOperations.SMALL.toString(), values);
    }

    public LongValue(long value) {
        this.value = value;
    }

    public LongValue(String valueString) {        
        value = Long.parseLong(valueString);
    }

    public LongValue(long value, String name) {
        super(name);
        this.value = value;
    }

    public LongValue(long value, IMetaInfo metaInfo) {
        super(metaInfo);
        this.value = value;        
    }    

    /**Formula constructor**/
    public LongValue(LongValue lv1, LongValue lv2, long value, String operand, boolean isMultiplicative) {
        super(lv1, lv2, operand, isMultiplicative);
        this.value = value;
    }

    /**Function constructor**/
    public LongValue(LongValue result, String functionName, LongValue[] params) {
        super(result, functionName, params);
        this.value = result.longValue();
    }

    @Override
    public LongValue copy(String name) {
        return copy(this, name);        
    }    

    @Override
    public double doubleValue() {        
        return (double)value;
    }

    @Override
    public float floatValue() {        
        return (float)value;
    }

    @Override
    public int intValue() {        
        return (int)value;
    }
    
    @Override
    public long longValue() {        
        return value;
    }
    
    public String printValue() {        
        return String.valueOf(value);
    }

    public int compareTo(Number o) {
        return value < o.longValue() ? -1 : (value == o.longValue() ? 0 : 1);        
    }
    
    public long getValue() {        
        return value;
    }
    
    public void setValue(long value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof LongValue) {
            LongValue secondObj = (LongValue) obj;
            return value == secondObj.longValue();
        }

        return false;
    }

    @Override
    public int hashCode() {
        return ((Long) value).hashCode();
    }
    
    public static LongValue abs(LongValue value) {
        // evaluate result
        LongValue result = new LongValue(Math.abs(value.getValue()));
        // create instance with information about last operation
        return new LongValue(result, NumberOperations.ABS.toString(), new LongValue[] { value });
    }
    
    public static LongValue inc(LongValue value) {
        return add(value, new LongValue(1));
    }
    
    public static LongValue dec(LongValue value) {
        return subtract(value, new LongValue(1));
    }
    
    public static LongValue positive(LongValue value) {
        return value;
    }
    
    private static long[] longValueArrayToLong(LongValue[] values) {
        if (ArrayTool.noNulls(values)) {
            long[] longArray = new long[values.length];
            for (int i = 0; i < values.length; i++) {
                longArray[i] = values[i].getValue();
            }
            return longArray;
        }
        return ArrayUtils.EMPTY_LONG_ARRAY;
    }

}
