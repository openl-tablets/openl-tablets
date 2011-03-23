package org.openl.meta;

import org.apache.commons.lang.ArrayUtils;
import org.openl.binding.impl.Operators;
import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.meta.number.NumberOperations;
import org.openl.util.ArrayTool;
import org.openl.util.math.MathUtils;

public class ShortValue extends ExplanationNumberValue<ShortValue> {

    private static final long serialVersionUID = 5259931539737847856L;

    private short value;

    public static ShortValue add(ShortValue shortValue1, ShortValue shortValue2) {
        validate(shortValue1, shortValue2, NumberOperations.ADD);

        return new ShortValue(shortValue1, shortValue2, Operators.add(shortValue1.getValue(), shortValue2.getValue()),
            NumberOperations.ADD, false);
    }

    public static ShortValue rem(ShortValue shortValue1, ShortValue shortValue2) {
        validate(shortValue1, shortValue2, NumberOperations.REM);
        
        return new ShortValue(shortValue1, shortValue2, Operators.rem(shortValue1.getValue(), shortValue2.getValue()),
            NumberOperations.REM, true);
    }

    // ******* Autocasts*************

    public static ShortValue autocast(byte x, ShortValue y) {
        return new ShortValue(x);
    }

    public static ShortValue autocast(short x, ShortValue y) {
        return new ShortValue(x);
    }

    public static ShortValue autocast(char x, ShortValue y) {
        return new ShortValue((short) x);
    }

    public static ShortValue autocast(int x, ShortValue y) {
        return new ShortValue((short) x);
    }

    public static ShortValue autocast(long x, ShortValue y) {
        return new ShortValue((short) x);
    }

    public static ShortValue autocast(float x, ShortValue y) {
        return new ShortValue((short) x);
    }

    public static ShortValue autocast(double x, ShortValue y) {
        return new ShortValue((short) x);
    }

    public static ShortValue autocast(Short x, ShortValue y) {
        if (x == null) {
            return null;
        }

        return new ShortValue(x);
    }

    public static IntValue autocast(ShortValue x, IntValue y) {
        if (x == null) {
            return null;
        }

        return new IntValue(x.getValue());
    }

    public static LongValue autocast(ShortValue x, LongValue y) {
        if (x == null) {
            return null;
        }

        return new LongValue(x.getValue());
    }

    public static FloatValue autocast(ShortValue x, FloatValue y) {
        if (x == null) {
            return null;
        }

        return new FloatValue(x.getValue());
    }

    public static DoubleValue autocast(ShortValue x, DoubleValue y) {
        if (x == null) {
            return null;
        }

        return new DoubleValue(x.getValue());
    }

    public static BigIntegerValue autocast(ShortValue x, BigIntegerValue y) {
        if (x == null) {
            return null;
        }
        return new BigIntegerValue(String.valueOf(x.getValue()));
    }

    public static BigDecimalValue autocast(ShortValue x, BigDecimalValue y) {
        if (x == null) {
            return null;
        }
        return new BigDecimalValue(String.valueOf(x.getValue()));
    }

    // ******* Casts*************

    public static byte cast(ShortValue x, byte y) {
        return x.byteValue();
    }

    public static short cast(ShortValue x, short y) {
        return x.shortValue();
    }

    public static char cast(ShortValue x, char y) {
        return (char) x.shortValue();
    }

    public static int cast(ShortValue x, int y) {
        return x.intValue();
    }

    public static long cast(ShortValue x, long y) {
        return x.longValue();
    }

    public static float cast(ShortValue x, float y) {
        return x.floatValue();
    }

    public static double cast(ShortValue x, double y) {
        return x.doubleValue();
    }

    public static Short cast(ShortValue x, Short y) {
        if (x == null) {
            return null;
        }

        return x.shortValue();
    }

    public static ByteValue cast(ShortValue x, ByteValue y) {
        if (x == null) {
            return null;
        }
        return new ByteValue(x.byteValue());
    }

    public static ShortValue copy(ShortValue value, String name) {

        if (value.getName() == null) {
            value.setName(name);

            return value;
        } else if (!value.getName().equals(name)) {
            ShortValue lv = new ShortValue(value, NumberOperations.COPY, new ShortValue[] { value });
            lv.setName(name);

            return lv;
        }

        return value;
    }

    public static ShortValue divide(ShortValue shortValue1, ShortValue shortValue2) {
        validate(shortValue1, shortValue2, NumberOperations.DIVIDE);
        
        return new ShortValue(shortValue1, shortValue2, Operators.divide(shortValue1.getValue(), shortValue2.getValue()),
            NumberOperations.DIVIDE, true);
    }

    public static boolean eq(ShortValue shortValue1, ShortValue shortValue2) {
        validate(shortValue1, shortValue2, NumberOperations.EQ);
        
        return Operators.eq(shortValue1.getValue(), shortValue2.getValue());
    }

    public static boolean ge(ShortValue shortValue1, ShortValue shortValue2) {
        validate(shortValue1, shortValue2, NumberOperations.GE);
        
        return Operators.ge(shortValue1.getValue(), shortValue2.getValue());
    }

    public static boolean gt(ShortValue shortValue1, ShortValue shortValue2) {
        validate(shortValue1, shortValue2, NumberOperations.GT);
        
        return Operators.gt(shortValue1.getValue(), shortValue2.getValue());
    }

    public static boolean le(ShortValue shortValue1, ShortValue shortValue2) {
        validate(shortValue1, shortValue2, NumberOperations.LE);
        
        return Operators.le(shortValue1.getValue(), shortValue2.getValue());
    }

    public static boolean lt(ShortValue shortValue1, ShortValue shortValue2) {
        validate(shortValue1, shortValue2, NumberOperations.LT);
        
        return Operators.lt(shortValue1.getValue(), shortValue2.getValue());
    }

    public static ShortValue max(ShortValue shortValue1, ShortValue shortValue2) {
        validate(shortValue1, shortValue2, NumberOperations.MAX);
        
        return new ShortValue(shortValue2.getValue() > shortValue1.getValue() ? shortValue2 : shortValue1,
            NumberOperations.MAX, new ShortValue[] { shortValue1, shortValue2 });
    }

    public static ShortValue min(ShortValue shortValue1, ShortValue shortValue2) {
        validate(shortValue1, shortValue2, NumberOperations.MIN);
        
        return new ShortValue(shortValue2.getValue() < shortValue1.getValue() ? shortValue2 : shortValue1,
            NumberOperations.MIN, new ShortValue[] { shortValue1, shortValue2 });
    }

    public static ShortValue multiply(ShortValue shortValue1, ShortValue shortValue2) {
        validate(shortValue1, shortValue2, NumberOperations.MULTIPLY);
        
        return new ShortValue(shortValue1, shortValue2, Operators.multiply(shortValue1.getValue(), shortValue2.getValue()),
            NumberOperations.MULTIPLY, true);
    }

    public static boolean ne(ShortValue shortValue1, ShortValue shortValue2) {
        validate(shortValue1, shortValue2, NumberOperations.NE);
        
        return Operators.ne(shortValue1.getValue(),shortValue2.getValue());
    }

    public static ShortValue negative(ShortValue value) {
        return multiply(value, new ShortValue((short) -1));
    }

    public static ShortValue pow(ShortValue shortValue1, ShortValue shortValue2) {
        validate(shortValue1, shortValue2, NumberOperations.POW);
        
        return new ShortValue(new ShortValue(Operators.pow(shortValue1.getValue(), shortValue2.getValue())),
            NumberOperations.POW, new ShortValue[] { shortValue1, shortValue2 });
    }

    public static ShortValue round(ShortValue shortValue1) {
        validate(shortValue1, NumberOperations.ROUND);
        
        return new ShortValue(new ShortValue((short) Math.round(shortValue1.getValue())), NumberOperations.ROUND, 
            new ShortValue[] { shortValue1 });
    }

    public static ShortValue subtract(ShortValue shortValue1, ShortValue shortValue2) {
        validate(shortValue1, shortValue2, NumberOperations.SUBTRACT);

        return new ShortValue(shortValue1, shortValue2, Operators.subtract(shortValue1.getValue(), shortValue2.getValue()),
            NumberOperations.SUBTRACT, false);
    }
    
    // Math functions
    
    public static ShortValue max(ShortValue[] values) {
        ShortValue result = (ShortValue) MathUtils.max(values);        
        return new ShortValue((ShortValue) getAppropriateValue(values, result), NumberOperations.MAX_IN_ARRAY,
            values);
    }

    public static ShortValue min(ShortValue[] values) {
        ShortValue result = (ShortValue) MathUtils.min(values);
        return new ShortValue((ShortValue) getAppropriateValue(values, result), NumberOperations.MIN_IN_ARRAY, 
            values);
    }
    
    public static ShortValue avg(ShortValue[] values) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        short[] shortArray = shortValueArrayToShort(values);
        short avg = MathUtils.avg(shortArray);
        
        return new ShortValue(new ShortValue(avg), NumberOperations.AVG, values);
    }
    
    public static ShortValue sum(ShortValue[] values) {    
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        short[] shortArray = shortValueArrayToShort(values);
        short sum = MathUtils.sum(shortArray);
        return new ShortValue(new ShortValue(sum), NumberOperations.SUM, values);
    }
    
    public static ShortValue median(ShortValue[] values) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        short[] shortArray = shortValueArrayToShort(values);
        short median = MathUtils.median(shortArray);
        return new ShortValue(new ShortValue(median), NumberOperations.MEDIAN, values);
    }
    
    public static DoubleValue product(ShortValue[] values) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        short[] shortArray = shortValueArrayToShort(values);
        double product = MathUtils.product(shortArray);
        
        // we loose the parameters, but not the result of computation.
        return new DoubleValue(new DoubleValue(product), NumberOperations.PRODUCT, null);
    }
    
    public static ShortValue quaotient(ShortValue number, ShortValue divisor) {
        if (number != null && divisor != null) {
            ShortValue result = new ShortValue(MathUtils.quaotient(number.getValue(), divisor.getValue()));
            return new ShortValue(result, NumberOperations.QUAOTIENT, new ShortValue[]{number, divisor} );
        }
        return null;
    }
    
    public static ShortValue mod(ShortValue number, ShortValue divisor) {
        if (number != null && divisor != null) {
            ShortValue result = new ShortValue(MathUtils.mod(number.getValue(), divisor.getValue()));
            return new ShortValue(result, NumberOperations.MOD, new ShortValue[]{number, divisor} );
        }
        return null;
    }
    
    public static ShortValue small(ShortValue[] values, int position) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        short[] shortArray = shortValueArrayToShort(values);
        short small = MathUtils.small(shortArray, position);
        return new ShortValue((ShortValue) getAppropriateValue(values, new ShortValue(small)), NumberOperations.SMALL, 
            values);
    }

    public ShortValue(short value) {
        this.value = value;
    }

    public ShortValue(String valueString) {
        value = Short.parseShort(valueString);
    }

    public ShortValue(short value, String name) {
        super(name);
        this.value = value;
    }

    public ShortValue(short value, IMetaInfo metaInfo) {
        super(metaInfo);
        this.value = value;
    }

    /** Formula constructor **/
    public ShortValue(ShortValue shortValue1, ShortValue shortValue2, short value, NumberOperations operand,
        boolean isMultiplicative) {
        super(shortValue1, shortValue2, operand, isMultiplicative);
        this.value = value;
    }

    /** Function constructor **/
    public ShortValue(ShortValue result, NumberOperations function, ShortValue[] params) {
        super(result, function, params);
        this.value = result.shortValue();
    }

    @Override
    public ShortValue copy(String name) {
        return copy(this, name);
    }

    @Override
    public double doubleValue() {
        return value;
    }

    @Override
    public float floatValue() {
        return value;
    }

    @Override
    public int intValue() {
        return value;
    }

    @Override
    public long longValue() {
        return value;
    }

    public String printValue() {
        return String.valueOf(value);
    }

    public int compareTo(Number o) {
        return value - o.shortValue();
    }

    public short getValue() {
        return value;
    }

    public void setValue(short value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ShortValue) {
            ShortValue secondObj = (ShortValue) obj;
            return value == secondObj.longValue();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return ((Short) value).hashCode();
    }

    public static ShortValue abs(ShortValue value) {
        // evaluate result
        ShortValue result = new ShortValue(Operators.abs(value.getValue()));
        // create instance with information about last operation
        return new ShortValue(result, NumberOperations.ABS, new ShortValue[] { value });
    }

    public static ShortValue inc(ShortValue value) {
        return add(value, new ShortValue((short) 1));
    }

    public static ShortValue dec(ShortValue value) {
        return subtract(value, new ShortValue((short) 1));
    }

    public static ShortValue positive(ShortValue value) {
        return value;
    }
    
    private static short[] shortValueArrayToShort(ShortValue[] values) {
        if (ArrayTool.noNulls(values)) {
            short[] shortArray = new short[values.length];
            for (int i = 0; i < values.length; i++) {
                shortArray[i] = values[i].getValue();
            }
            return shortArray;
        }
        return ArrayUtils.EMPTY_SHORT_ARRAY;
    }

}
