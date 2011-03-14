package org.openl.meta;

import org.apache.commons.lang.ArrayUtils;
import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.meta.number.NumberOperations;
import org.openl.util.ArrayTool;
import org.openl.util.math.MathUtils;

public class IntValue extends ExplanationNumberValue<IntValue> {
    
    private static final long serialVersionUID = -3821702883606493390L;    
    
    private int value;
    
    public static IntValue add(IntValue intValue1, IntValue intValue2) {

        if (intValue1 == null || intValue1.getValue() == 0) {
            return intValue2;
        }

        if (intValue2 == null || intValue2.getValue() == 0) {
            return intValue1;
        }

        return new IntValue(intValue1, intValue2, intValue1.getValue() + intValue2.getValue(), 
            NumberOperations.ADD.toString(), false);
    }    
    
    public static IntValue rem(IntValue intValue1, IntValue intValue2) {
        return new IntValue(intValue1, intValue2, intValue1.getValue() % intValue2.getValue(), 
            NumberOperations.REM.toString(), true);
    }
    
    // ******* Autocasts*************
    
    public static IntValue autocast(byte x, IntValue y) {
        return new IntValue(x);
    }

    public static IntValue autocast(short x, IntValue y) {
        return new IntValue(x);
    }
    
    public static IntValue autocast(char x, IntValue y) {
        return new IntValue(x);    
    }

    public static IntValue autocast(int x, IntValue y) {
        return new IntValue(x);
    }

    public static IntValue autocast(long x, IntValue y) {
        return new IntValue((int)x);
    }

    public static IntValue autocast(float x, IntValue y) {
        return new IntValue((int)x);
    }

    public static IntValue autocast(double x, IntValue y) {
        return new IntValue((int)x);
    }

    public static IntValue autocast(Integer x, IntValue y) {
        if (x == null) {
            return null;
        }

        return new IntValue(x);
    }
    
    public static LongValue autocast(IntValue x, LongValue y) {
        if (x == null) {
            return null;
        }

        return new LongValue(x.getValue());
    }
    
    public static FloatValue autocast(IntValue x, FloatValue y) {
        if (x == null) {
            return null;
        }

        return new FloatValue(x.getValue());
    }
    
    public static DoubleValue autocast(IntValue x, DoubleValue y) {
        if (x == null) {
            return null;
        }

        return new DoubleValue(x.getValue());
    }
    
    public static BigIntegerValue autocast(IntValue x, BigIntegerValue y) {
        if (x == null) {
            return null;
        }
        return new BigIntegerValue(String.valueOf(x.getValue()));
    }
    
    public static BigDecimalValue autocast(IntValue x, BigDecimalValue y) {
        if (x == null) {
            return null;
        }
        return new BigDecimalValue(String.valueOf(x.getValue()));
    }
    
    // ******* Casts*************

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

    public static Integer cast(IntValue x, Integer y) {
        if (x == null) {
            return null;
        }

        return x.intValue();
    }
    
    public static ByteValue cast(IntValue x, ByteValue y) {
        if (x == null) {
            return null;
        }
        return new ByteValue(x.byteValue());
    }
        
    public static ShortValue cast(IntValue x, ShortValue y) {
        if (x == null) {
            return null;
        }
        return new ShortValue(x.shortValue());
    }    

    public static IntValue copy(IntValue value, String name) {

        if (value.getName() == null) {
            value.setName(name);

            return value;
        } else if (!value.getName().equals(name)) {
            IntValue lv = new IntValue(value, NumberOperations.COPY.toString(), new IntValue[] { value });
            lv.setName(name);

            return lv;
        }

        return value;
    }

    public static IntValue divide(IntValue intValue1, IntValue intValue2) {
        return new IntValue(intValue1, intValue2, intValue1.getValue() / intValue2.getValue(), 
            NumberOperations.DIVIDE.toString(), true);
    }

    public static boolean eq(IntValue intValue1, IntValue intValue2) {
        return intValue1.getValue() == intValue2.getValue();
    }

    public static boolean ge(IntValue intValue1, IntValue intValue2) {
        return intValue1.getValue() >= intValue2.getValue();
    }

    public static boolean gt(IntValue intValue1, IntValue intValue2) {
        return intValue1.getValue() > intValue2.getValue();
    }

    public static boolean le(IntValue intValue1, IntValue intValue2) {
        return intValue1.getValue() <= intValue2.getValue();
    }

    public static boolean lt(IntValue intValue1, IntValue intValue2) {
        return intValue1.getValue() < intValue2.getValue();
    }

    public static IntValue max(IntValue intValue1, IntValue intValue2) {
        return new IntValue(intValue2.getValue() > intValue1.getValue() ? intValue2 : intValue1,
            NumberOperations.MAX.toString(),
            new IntValue[] { intValue1, intValue2 });
    }

    public static IntValue min(IntValue intValue1, IntValue intValue2) {
        return new IntValue(intValue2.getValue() < intValue1.getValue() ? intValue2 : intValue1,
            NumberOperations.MIN.toString(),
            new IntValue[] { intValue1, intValue2 });
    }

    public static IntValue multiply(IntValue intValue1, IntValue intValue2) {
        return new IntValue(intValue1, intValue2, intValue1.getValue() * intValue2.getValue(), 
            NumberOperations.MULTIPLY.toString(), true);
    }

    public static boolean ne(IntValue intValue1, IntValue intValue2) {
        return intValue1.getValue() != intValue2.getValue();
    }

    public static IntValue negative(IntValue value) {
        return multiply(value, new IntValue(-1));
    }

    public static IntValue pow(IntValue intValue1, IntValue intValue2) {
        return new IntValue(new IntValue((int)Math.pow(intValue1.getValue(), intValue2.getValue())), 
            NumberOperations.POW.toString(), new IntValue[] { intValue1, intValue2 });
    }

    public static IntValue round(IntValue intValue1) {
        return new IntValue(new IntValue((int)Math.round(intValue1.getValue())), 
            NumberOperations.ROUND.toString(), new IntValue[] { intValue1 });
    }

    public static IntValue subtract(IntValue intValue1, IntValue intValue2) {

        if (intValue2 == null || intValue2.getValue() == 0) {
            return intValue1;
        }

        return new IntValue(intValue1, intValue2, intValue1.getValue() - intValue2.getValue(), 
            NumberOperations.SUBTRACT.toString(), false);
    }
    
    // Math functions
    
    public static IntValue max(IntValue[] values) {
        IntValue result = (IntValue) MathUtils.max(values);        
        return new IntValue((IntValue) getAppropriateValue(values, result), NumberOperations.MAX_IN_ARRAY.toString(),
            values);
    }

    public static IntValue min(IntValue[] values) {
        IntValue result = (IntValue) MathUtils.min(values);
        return new IntValue((IntValue) getAppropriateValue(values, result), NumberOperations.MIN_IN_ARRAY.toString(), 
            values);
    }
    
    public static IntValue avg(IntValue[] values) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        int[] primitiveArray = intValueArrayToInt(values);
        int avg = MathUtils.avg(primitiveArray);
        
        return new IntValue(new IntValue(avg), NumberOperations.AVG.toString(), values);
    }
    
    public static IntValue sum(IntValue[] values) {    
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        int[] primitiveArray = intValueArrayToInt(values);
        int sum = MathUtils.sum(primitiveArray);
        return new IntValue(new IntValue(sum), NumberOperations.SUM.toString(), values);
    }
    
    public static IntValue median(IntValue[] values) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        int[] primitiveArray = intValueArrayToInt(values);
        int median = MathUtils.median(primitiveArray);
        return new IntValue(new IntValue(median), NumberOperations.MEDIAN.toString(), values);
    }
    
    public static DoubleValue product(IntValue[] values) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        int[] primitiveArray = intValueArrayToInt(values);
        double product = MathUtils.product(primitiveArray);
        return new DoubleValue(new DoubleValue(product), NumberOperations.PRODUCT.toString(), null);
    }
    
    public static IntValue quaotient(IntValue number, IntValue divisor) {
        if (number != null && divisor != null) {
            IntValue result = new IntValue(MathUtils.quaotient(number.getValue(), divisor.getValue()));
            return new IntValue(result, NumberOperations.QUAOTIENT.toString(), new IntValue[]{number, divisor} );
        }
        return null;
    }
    
    public static IntValue mod(IntValue number, IntValue divisor) {
        if (number != null && divisor != null) {
            IntValue result = new IntValue(MathUtils.mod(number.getValue(), divisor.getValue()));
            return new IntValue(result, NumberOperations.MOD.toString(), new IntValue[]{number, divisor} );
        }
        return null;
    }
    
    public static IntValue small(IntValue[] values, int position) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        int[] primitiveArray = intValueArrayToInt(values);
        int small = MathUtils.small(primitiveArray, position);
        return new IntValue((IntValue) getAppropriateValue(values, new IntValue(small)), NumberOperations.SMALL.toString(), values);
    }

    public IntValue(int value) {
        this.value = value;
    }

    public IntValue(String valueString) {        
        value = Integer.parseInt(valueString);
    }

    public IntValue(int value, String name) {
        super(name);
        this.value = value;
    }

    public IntValue(int value, IMetaInfo metaInfo) {
        super(metaInfo);
        this.value = value;        
    }    

    /**Formula constructor**/
    public IntValue(IntValue intValue1, IntValue intValue2, int value, String operand, boolean isMultiplicative) {
        super(intValue1, intValue2, operand, isMultiplicative);
        this.value = value;
    }

    /**Function constructor**/
    public IntValue(IntValue result, String functionName, IntValue[] params) {
        super(result, functionName, params);
        this.value = result.intValue();
    }
    

    @Override
    public IntValue copy(String name) {
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
        return value;
    }

    @Override
    public long longValue() {        
        return (long)value;
    }

    public String printValue() {        
        return String.valueOf(value);
    }

    public int compareTo(Number o) {        
        return value < o.intValue() ? -1 : (value == o.intValue() ? 0 : 1);
    }
    
    public int getValue() {        
        return value;
    }
    
    public void setValue(int value) {
        this.value = value;
    }

    public static IntValue abs(IntValue value) {
        // evaluate result
        IntValue result = new IntValue(Math.abs(value.getValue()));
        // create instance with information about last operation
        return new IntValue(result, NumberOperations.ABS.toString(), new IntValue[] { value });
    }
    
    public static IntValue inc(IntValue value) {
        return add(value, new IntValue(1));
    }
    
    public static IntValue dec(IntValue value) {
        return subtract(value, new IntValue(1));
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IntValue) {
            IntValue secondObj = (IntValue) obj;
            return value == secondObj.intValue();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return ((Integer) value).hashCode();
    }

    public static IntValue positive(IntValue value) {
        return value;
    }
    
    private static int[] intValueArrayToInt(IntValue[] values) {
        if (ArrayTool.noNulls(values)) {
            int[] intArray = new int[values.length];
            for (int i = 0; i < values.length; i++) {
                intArray[i] = values[i].getValue();
            }
            return intArray;
        }
        return ArrayUtils.EMPTY_INT_ARRAY;
    }

}
