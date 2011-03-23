package org.openl.meta;

import org.apache.commons.lang.ArrayUtils;
import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.meta.number.NumberOperations;
import org.openl.util.ArrayTool;
import org.openl.util.math.MathUtils;
import org.openl.binding.impl.Operators;


public class ByteValue extends ExplanationNumberValue<ByteValue> {

    private static final long serialVersionUID = -3137978912171407672L;
    
    private byte value;
    
    public static ByteValue add(ByteValue byteValue1, ByteValue byteValue2) {
        validate(byteValue1, byteValue2, NumberOperations.ADD);

        return new ByteValue(byteValue1, byteValue2, Operators.add(byteValue1.getValue(), byteValue2.getValue()), 
            NumberOperations.ADD, false);
    }
    
    public static ByteValue rem(ByteValue byteValue1, ByteValue byteValue2) {        
        validate(byteValue1, byteValue2, NumberOperations.REM);
        
        return new ByteValue(byteValue1, byteValue2, Operators.rem(byteValue1.getValue(), byteValue2.getValue()),
            NumberOperations.REM, true);
    }
    
    // ******* Autocasts *************
    
    public static ByteValue autocast(byte x, ByteValue y) {
        return new ByteValue(x);
    }

    public static ByteValue autocast(short x, ByteValue y) {
        return new ByteValue((byte)x);
    }
    
    public static ByteValue autocast(char x, ByteValue y) {
        return new ByteValue((byte)x);    
    }

    public static ByteValue autocast(int x, ByteValue y) {
        return new ByteValue((byte)x);
    }

    public static ByteValue autocast(long x, ByteValue y) {
        return new ByteValue((byte)x);
    }

    public static ByteValue autocast(float x, ByteValue y) {
        return new ByteValue((byte)x);
    }

    public static ByteValue autocast(double x, ByteValue y) {
        return new ByteValue((byte)x);
    }

    public static ByteValue autocast(Byte x, ByteValue y) {
        if (x == null) {
            return null;
        }

        return new ByteValue(x);
    }
    
    public static ShortValue autocast(ByteValue x, ShortValue y) {
        if (x == null) {
            return null;
        }

        return new ShortValue(x.getValue());
    }
    
    public static IntValue autocast(ByteValue x, IntValue y) {
        if (x == null) {
            return null;
        }

        return new IntValue(x.getValue());
    }
    
    public static LongValue autocast(ByteValue x, LongValue y) {
        if (x == null) {
            return null;
        }

        return new LongValue(x.getValue());
    }
    
    public static FloatValue autocast(ByteValue x, FloatValue y) {
        if (x == null) {
            return null;
        }

        return new FloatValue(x.getValue());
    }
    
    public static DoubleValue autocast(ByteValue x, DoubleValue y) {
        if (x == null) {
            return null;
        }

        return new DoubleValue(x.getValue());
    }
    
    public static BigIntegerValue autocast(ByteValue x, BigIntegerValue y) {
        if (x == null) {
            return null;
        }
        return new BigIntegerValue(String.valueOf(x.getValue()));
    }
    
    public static BigDecimalValue autocast(ByteValue x, BigDecimalValue y) {
        if (x == null) {
            return null;
        }
        return new BigDecimalValue(String.valueOf(x.getValue()));
    }
    
    // ******* Casts *************

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

    public static Byte cast(ByteValue x, Byte y) {
        if (x == null) {
            return null;
        }

        return x.byteValue();
    }

    public static ByteValue copy(ByteValue value, String name) {
        
        if (value.getName() == null) {
            value.setName(name);

            return value;
        } else if (!value.getName().equals(name)) {
            ByteValue lv = new ByteValue(value, NumberOperations.COPY, new ByteValue[] { value });
            lv.setName(name);

            return lv;
        }

        return value;
    }

    public static ByteValue divide(ByteValue byteValue1, ByteValue byteValue2) {
        validate(byteValue1, byteValue2, NumberOperations.DIVIDE);
        
        return new ByteValue(byteValue1, byteValue2, Operators.divide(byteValue1.getValue(), byteValue2.getValue()),
            NumberOperations.DIVIDE, true);
    }

    public static boolean eq(ByteValue byteValue1, ByteValue byteValue2) {     
        validate(byteValue1, byteValue2, NumberOperations.EQ);
        
        return byteValue1.equals(byteValue2);
    }

    public static boolean ge(ByteValue byteValue1, ByteValue byteValue2) {
        validate(byteValue1, byteValue2, NumberOperations.GE);
        
        return Operators.ge(byteValue1.getValue(), byteValue2.getValue());
    }

    public static boolean gt(ByteValue byteValue1, ByteValue byteValue2) {
        validate(byteValue1, byteValue2, NumberOperations.GT);
        
        return Operators.gt(byteValue1.getValue(), byteValue2.getValue());
    }

    public static boolean le(ByteValue byteValue1, ByteValue byteValue2) {
        validate(byteValue1, byteValue1, NumberOperations.LE);
        
        return Operators.le(byteValue1.getValue(), byteValue2.getValue());
    }

    public static boolean lt(ByteValue byteValue1, ByteValue byteValue2) {
        validate(byteValue1, byteValue1, NumberOperations.LT);
        
        return Operators.lt(byteValue1.getValue(), byteValue2.getValue());
    }

    public static ByteValue max(ByteValue byteValue1, ByteValue byteValue2) {
        validate(byteValue1, byteValue2, NumberOperations.MAX);
        
        return new ByteValue(byteValue2.getValue() > byteValue1.getValue() ? byteValue2 : byteValue1,
            NumberOperations.MAX,
            new ByteValue[] { byteValue1, byteValue2 });
    }

    public static ByteValue min(ByteValue byteValue1, ByteValue byteValue2) {
        validate(byteValue1, byteValue2, NumberOperations.MIN);
        
        return new ByteValue(byteValue2.getValue() < byteValue1.getValue() ? byteValue2 : byteValue1,
            NumberOperations.MIN,
            new ByteValue[] { byteValue1, byteValue2 });
    }

    public static ByteValue abs(ByteValue value) {
        validate(value, NumberOperations.ABS);
        // evaluate result
        ByteValue result = new ByteValue(Operators.abs(value.getValue()));
        // create instance with information about last operation
        return new ByteValue(result, NumberOperations.ABS, new ByteValue[] { value });
    }

    public static ByteValue inc(ByteValue value) {
        return add(value, new ByteValue((byte) 1));
    }
    
    public static ByteValue positive(ByteValue value) {
        return value;
    }
    
    public static ByteValue dec(ByteValue value) {
        return subtract(value, new ByteValue((byte) 1));
    }

    public static ByteValue multiply(ByteValue byteValue1, ByteValue byteValue2) {
        validate(byteValue1, byteValue2, NumberOperations.MULTIPLY);
        
        return new ByteValue(byteValue1, byteValue2, Operators.multiply(byteValue1.getValue(), byteValue2.getValue()), 
            NumberOperations.MULTIPLY, true);
    }

    public static boolean ne(ByteValue byteValue1, ByteValue byteValue2) {
        validate(byteValue1, byteValue2, NumberOperations.NE);
        
        return Operators.ne(byteValue1.getValue(), byteValue2.getValue());
    }

    public static ByteValue negative(ByteValue value) {
        return multiply(value, new ByteValue((byte) -1));
    }

    public static ByteValue pow(ByteValue byteValue1, ByteValue byteValue2) {
        validate(byteValue1, byteValue2, NumberOperations.POW);
        
        return new ByteValue(new ByteValue(Operators.pow(byteValue1.getValue(), byteValue2.getValue())), 
            NumberOperations.POW, new ByteValue[] { byteValue1, byteValue2 });
    }

    public static ByteValue round(ByteValue byteValue1) {
        validate(byteValue1, NumberOperations.ROUND);
        
        return new ByteValue(new ByteValue((byte)Math.round(byteValue1.getValue())), 
            NumberOperations.ROUND, new ByteValue[] { byteValue1 });
    }

    public static ByteValue subtract(ByteValue byteValue1, ByteValue byteValue2) {
        validate(byteValue1, byteValue2, NumberOperations.SUBTRACT);

        return new ByteValue(byteValue1, byteValue2, Operators.subtract(byteValue1.getValue(), byteValue2.getValue()), 
            NumberOperations.SUBTRACT, false);
    }
    
    // Math functions
    
    public static ByteValue max(ByteValue[] values) {
        ByteValue result = (ByteValue) MathUtils.max(values);        
        return new ByteValue((ByteValue) getAppropriateValue(values, result), 
            NumberOperations.MAX_IN_ARRAY, values);
    }

    public static ByteValue min(ByteValue[] values) {
        ByteValue result = (ByteValue) MathUtils.min(values);
        return new ByteValue((ByteValue) getAppropriateValue(values, result), 
            NumberOperations.MIN_IN_ARRAY, values);
    }
    
    public static ByteValue avg(ByteValue[] values) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        byte[] primitiveArray = byteValueArrayToByte(values);
        byte avg = MathUtils.avg(primitiveArray);
        
        return new ByteValue(new ByteValue(avg), NumberOperations.AVG, values);
    }
    
    public static ByteValue sum(ByteValue[] values) { 
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        byte[] primitiveArray = byteValueArrayToByte(values);
        byte sum = MathUtils.sum(primitiveArray);
        return new ByteValue(new ByteValue(sum), NumberOperations.SUM, values);
    }
    
    public static ByteValue median(ByteValue[] values) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        byte[] primitiveArray = byteValueArrayToByte(values);
        byte median = MathUtils.median(primitiveArray);
        return new ByteValue(new ByteValue(median), NumberOperations.MEDIAN, values);
    }
    
    public static DoubleValue product(ByteValue[] values) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        byte[] primitiveArray = byteValueArrayToByte(values);
        double product = MathUtils.product(primitiveArray);
        
        // we loose the parameters, but not the result of computation.
        return new DoubleValue(new DoubleValue(product), NumberOperations.PRODUCT, null);
    }
    
    public static ByteValue quaotient(ByteValue number, ByteValue divisor) {
        if (number != null && divisor != null) {
            ByteValue result = new ByteValue(MathUtils.quaotient(number.getValue(), divisor.getValue()));
            return new ByteValue(result, NumberOperations.QUAOTIENT, new ByteValue[]{number, divisor} );
        }
        return null;
    }
    
    public static ByteValue mod(ByteValue number, ByteValue divisor) {
        if (number != null && divisor != null) {
            ByteValue result = new ByteValue(MathUtils.mod(number.getValue(), divisor.getValue()));
            return new ByteValue(result, NumberOperations.MOD, new ByteValue[]{number, divisor} );
        }
        return null;
    }
    
    public static ByteValue small(ByteValue[] values, int position) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        byte[] primitiveArray = byteValueArrayToByte(values);
        byte small = MathUtils.small(primitiveArray, position);
        return new ByteValue((ByteValue) getAppropriateValue(values, new ByteValue(small)), 
            NumberOperations.SMALL, values);
    }

    public ByteValue(byte value) {
        this.value = value;
    }

    public ByteValue(String valueString) {        
        value = Byte.parseByte(valueString);
    }

    public ByteValue(byte value, String name) {
        super(name);
        this.value = value;
    }

    public ByteValue(byte value, IMetaInfo metaInfo) {
        super(metaInfo);
        this.value = value;        
    }    

    /**Formula constructor**/
    public ByteValue(ByteValue byteValue1, ByteValue byteValue2, byte value, NumberOperations operand, boolean isMultiplicative) {
        super(byteValue1, byteValue2, operand, isMultiplicative);
        this.value = value;
    }

    /**Function constructor**/
    public ByteValue(ByteValue result, NumberOperations function, ByteValue[] params) {
        super(result, function, params);
        this.value = result.byteValue();
    }
   
    @Override
    public ByteValue copy(String name) {        
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
        return (long)value;
    }

    public String printValue() {
        return String.valueOf(value);
    }

    public int compareTo(Number o) {        
        return (int)(value - o.byteValue());
    }
    
    public byte getValue() {        
        return value;
    }
    
    public void setValue(byte value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ByteValue) {
            ByteValue secondObj = (ByteValue) obj;
            return Operators.eq(value, secondObj.byteValue());
        }

        return false;
    }

    @Override
    public int hashCode() {
        return ((Byte) value).hashCode();
    }
    
    private static byte[] byteValueArrayToByte(ByteValue[] values) {
        if (ArrayTool.noNulls(values)) {
            byte[] primitiveArray = new byte[values.length];
            for (int i = 0; i < values.length; i++) {
                primitiveArray[i] = values[i].getValue();
            }
            return primitiveArray;
        }
        return ArrayUtils.EMPTY_BYTE_ARRAY;
    }
}
