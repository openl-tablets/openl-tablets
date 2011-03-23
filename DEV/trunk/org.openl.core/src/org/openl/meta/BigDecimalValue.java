package org.openl.meta;

import java.math.BigDecimal;

import org.apache.commons.lang.ArrayUtils;
import org.openl.binding.impl.Operators;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.meta.number.NumberOperations;
import org.openl.util.ArrayTool;
import org.openl.util.math.MathUtils;

public class BigDecimalValue extends ExplanationNumberValue<BigDecimalValue> {

    private static final long serialVersionUID = 1996508840075924034L;

    private BigDecimal value;

    public static BigDecimalValue add(BigDecimalValue bigDecimalValue1, BigDecimalValue bigDecimalValue2) {
        validate(bigDecimalValue1, bigDecimalValue2, NumberOperations.ADD);
        
        return new BigDecimalValue(bigDecimalValue1, bigDecimalValue2, Operators.add(bigDecimalValue1.getValue(),
            bigDecimalValue2.getValue()), NumberOperations.ADD, false);
    }

    public static BigDecimalValue rem(BigDecimalValue bigDecimalValue1, BigDecimalValue bigDecimalValue2) {
        validate(bigDecimalValue1, bigDecimalValue2, NumberOperations.REM);
        
        return new BigDecimalValue(bigDecimalValue1, bigDecimalValue2, 
            Operators.rem(bigDecimalValue1.getValue(), bigDecimalValue2.getValue()), NumberOperations.REM, true);
    }

    // ******* Autocasts *************

    public static BigDecimalValue autocast(byte x, BigDecimalValue y) {
        return new BigDecimalValue(String.valueOf(x));
    }

    public static BigDecimalValue autocast(short x, BigDecimalValue y) {
        return new BigDecimalValue(String.valueOf(x));
    }

    public static BigDecimalValue autocast(char x, BigDecimalValue y) {
        return new BigDecimalValue(String.valueOf((int) x));
    }

    public static BigDecimalValue autocast(int x, BigDecimalValue y) {
        return new BigDecimalValue(String.valueOf(x));
    }

    public static BigDecimalValue autocast(long x, BigDecimalValue y) {
        return new BigDecimalValue(String.valueOf(x));
    }

    public static BigDecimalValue autocast(float x, BigDecimalValue y) {
        return new BigDecimalValue(String.valueOf(x));
    }

    public static BigDecimalValue autocast(double x, BigDecimalValue y) {
        return new BigDecimalValue(String.valueOf(x));
    }

    public static BigDecimalValue autocast(BigDecimal x, BigDecimalValue y) {
        if (x == null) {
            return null;
        }

        return new BigDecimalValue(x);
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

    public static ByteValue cast(BigDecimalValue x, ByteValue y) {
        if (x == null) {
            return null;
        }
        return new ByteValue(x.byteValue());
    }

    public static ShortValue cast(BigDecimalValue x, ShortValue y) {
        if (x == null) {
            return null;
        }
        return new ShortValue(x.shortValue());
    }

    public static IntValue cast(BigDecimalValue x, IntValue y) {
        if (x == null) {
            return null;
        }
        return new IntValue(x.intValue());
    }

    public static LongValue cast(BigDecimalValue x, LongValue y) {
        if (x == null) {
            return null;
        }
        return new LongValue(x.longValue());
    }

    public static FloatValue cast(BigDecimalValue x, FloatValue y) {
        if (x == null) {
            return null;
        }
        return new FloatValue(x.floatValue());
    }

    public static DoubleValue cast(BigDecimalValue x, DoubleValue y) {
        if (x == null) {
            return null;
        }
        return new DoubleValue(x.doubleValue());
    }

    public static BigIntegerValue cast(BigDecimalValue x, BigIntegerValue y) {
        if (x == null) {
            return null;
        }
        return new BigIntegerValue(String.valueOf(x.longValue()));
    }

    public static BigDecimalValue copy(BigDecimalValue value, String name) {
        if (value.getName() == null) {
            value.setName(name);

            return value;
        } else if (!value.getName().equals(name)) {
            BigDecimalValue lv = new BigDecimalValue(value, NumberOperations.COPY,
                new BigDecimalValue[] { value });
            lv.setName(name);

            return lv;
        }

        return value;
    }

    public static BigDecimalValue divide(BigDecimalValue bigDecimalValue1, BigDecimalValue bigDecimalValue2) {
        validate(bigDecimalValue1, bigDecimalValue2, NumberOperations.DIVIDE);
        
        return new BigDecimalValue(bigDecimalValue1, bigDecimalValue2, 
            Operators.divide(bigDecimalValue1.getValue(), bigDecimalValue2.getValue()), 
            NumberOperations.DIVIDE, true);
    }

    public static boolean eq(BigDecimalValue bigDecimalValue1, BigDecimalValue bigDecimalValue2) {
        validate(bigDecimalValue1, bigDecimalValue2, NumberOperations.EQ);

        return bigDecimalValue1.equals(bigDecimalValue2);
    }

    public static boolean ge(BigDecimalValue bigDecimalValue1, BigDecimalValue bigDecimalValue2) {
        validate(bigDecimalValue1, bigDecimalValue2, NumberOperations.GE);
        
        return Operators.ge(bigDecimalValue1.getValue(), bigDecimalValue2.getValue());
    }

    public static boolean gt(BigDecimalValue bigDecimalValue1, BigDecimalValue bigDecimalValue2) {
        validate(bigDecimalValue1, bigDecimalValue2, NumberOperations.GT);
        
        return Operators.gt(bigDecimalValue1.getValue(), bigDecimalValue2.getValue());
    }

    public static boolean le(BigDecimalValue bigDecimalValue1, BigDecimalValue bigDecimalValue2) {
        validate(bigDecimalValue1, bigDecimalValue2, NumberOperations.LE);
        
        return Operators.le(bigDecimalValue1.getValue(), bigDecimalValue2.getValue());
    }

    public static boolean lt(BigDecimalValue bigDecimalValue1, BigDecimalValue bigDecimalValue2) {
        validate(bigDecimalValue1, bigDecimalValue2, NumberOperations.LT);
        
        return Operators.lt(bigDecimalValue1.getValue(), bigDecimalValue2.getValue());
    }

    public static BigDecimalValue max(BigDecimalValue bigDecimalValue1, BigDecimalValue bigDecimalValue2) {
        validate(bigDecimalValue1, bigDecimalValue2, NumberOperations.MAX);
        BigDecimalValue maxValue = null;        
        
        maxValue = bigDecimalValue1.compareTo(bigDecimalValue2) > 0 ? bigDecimalValue1 : bigDecimalValue2;
        
        return new BigDecimalValue(maxValue, NumberOperations.MAX, new BigDecimalValue[] { bigDecimalValue1,
                bigDecimalValue2 });
    }

    public static BigDecimalValue min(BigDecimalValue bigDecimalValue1, BigDecimalValue bigDecimalValue2) {
        validate(bigDecimalValue1, bigDecimalValue2, NumberOperations.MIN);
        
        BigDecimalValue minValue = null;
        minValue = bigDecimalValue1.compareTo(bigDecimalValue2) < 0 ? bigDecimalValue1 : bigDecimalValue2;
        
        return new BigDecimalValue(minValue, NumberOperations.MIN, new BigDecimalValue[] { bigDecimalValue1,
                bigDecimalValue2 });
        
    }

    public static BigDecimalValue multiply(BigDecimalValue bigDecimalValue1, BigDecimalValue bigDecimalValue2) {
        validate(bigDecimalValue1, bigDecimalValue2, NumberOperations.MULTIPLY);
        
        return new BigDecimalValue(bigDecimalValue1, bigDecimalValue2, 
            Operators.multiply(bigDecimalValue1.getValue(), bigDecimalValue2.getValue()), 
            NumberOperations.MULTIPLY, true);
    }

    public static boolean ne(BigDecimalValue bigDecimalValue1, BigDecimalValue bigDecimalValue2) {
        validate(bigDecimalValue1, bigDecimalValue2, NumberOperations.NE);
        
        return Operators.ne(bigDecimalValue1.getValue(), bigDecimalValue2.getValue());
    }

    public static BigDecimalValue negative(BigDecimalValue bigIntValue) {        
        BigDecimalValue neg = new BigDecimalValue(bigIntValue.getValue().negate());
        neg.setMetaInfo(bigIntValue.getMetaInfo());

        return neg;
    }

    public static BigDecimalValue pow(BigDecimalValue bigDecimalValue1, BigDecimalValue bigDecimalValue2) {
        validate(bigDecimalValue1, bigDecimalValue2, NumberOperations.POW);
        
        return new BigDecimalValue(new BigDecimalValue(
            Operators.pow(bigDecimalValue1.getValue(), bigDecimalValue2.getValue().intValue())), 
            NumberOperations.POW, new BigDecimalValue[] { bigDecimalValue1, bigDecimalValue2 });
    }

    public static BigDecimalValue subtract(BigDecimalValue bigDecimalValue1, BigDecimalValue bigDecimalValue2) {
        validate(bigDecimalValue1, bigDecimalValue2, NumberOperations.SUBTRACT);

        return new BigDecimalValue(bigDecimalValue1, bigDecimalValue2, 
            Operators.subtract(bigDecimalValue1.getValue(), bigDecimalValue2.getValue()), 
            NumberOperations.SUBTRACT, false);
    }
    
// Math functions
    
    public static BigDecimalValue max(BigDecimalValue[] values) {
        BigDecimalValue result = (BigDecimalValue) MathUtils.max(values);        
        return new BigDecimalValue((BigDecimalValue) getAppropriateValue(values, result), 
            NumberOperations.MAX_IN_ARRAY, values);
    }

    public static BigDecimalValue min(BigDecimalValue[] values) {
        BigDecimalValue result = (BigDecimalValue) MathUtils.min(values);
        return new BigDecimalValue((BigDecimalValue) getAppropriateValue(values, result), 
            NumberOperations.MIN_IN_ARRAY, values);
    }
    
    public static BigDecimalValue avg(BigDecimalValue[] values) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        BigDecimal[] primitiveArray = unwrap(values);
        BigDecimal avg = MathUtils.avg(primitiveArray);
        
        return new BigDecimalValue(new BigDecimalValue(avg), NumberOperations.AVG, values);
    }
    
    public static BigDecimalValue sum(BigDecimalValue[] values) { 
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        BigDecimal[] primitiveArray = unwrap(values);
        BigDecimal sum = MathUtils.sum(primitiveArray);
        return new BigDecimalValue(new BigDecimalValue(sum), NumberOperations.SUM, values);
    }
    
    public static BigDecimalValue product(BigDecimalValue[] values) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        BigDecimal[] primitiveArray = unwrap(values);
        BigDecimal product = MathUtils.product(primitiveArray);
        
        // we loose the parameters, but not the result of computation.
        return new BigDecimalValue(new BigDecimalValue(product), NumberOperations.PRODUCT, null);
    }
    
    public static LongValue quaotient(BigDecimalValue number, BigDecimalValue divisor) {
        if (number != null && divisor != null) {
            LongValue result = new LongValue(MathUtils.quaotient(number.getValue(), divisor.getValue()));
            return new LongValue(result, NumberOperations.QUAOTIENT, null);
        }
        return null;
    }
    
    public static BigDecimalValue mod(BigDecimalValue number, BigDecimalValue divisor) {
        if (number != null && divisor != null) {
            BigDecimalValue result = new BigDecimalValue(MathUtils.mod(number.getValue(), divisor.getValue()));
            return new BigDecimalValue(result, NumberOperations.MOD, new BigDecimalValue[]{number, divisor} );
        }
        return null;
    }

    public BigDecimalValue(BigDecimal value) {
        this.value = value;
    }

    public BigDecimalValue(String valueString) {
        value = new BigDecimal(valueString);
    }

    public BigDecimalValue(BigDecimal value, String name) {
        super(name);
        this.value = value;
    }

    public BigDecimalValue(BigDecimal value, IMetaInfo metaInfo) {
        super(metaInfo);
        this.value = value;
    }

    public BigDecimalValue(String value, String name) {
        super(name);
        this.value = new BigDecimal(value);
    }

    public BigDecimalValue(String value, IMetaInfo metaInfo) {
        super(metaInfo);
        this.value = new BigDecimal(value);
    }

    /** Formula constructor **/
    public BigDecimalValue(BigDecimalValue bigDecimalValue1, BigDecimalValue bigDecimalValue2, BigDecimal value,
        NumberOperations operand, boolean isMultiplicative) {
        super(bigDecimalValue1, bigDecimalValue2, operand, isMultiplicative);
        this.value = value;
    }

    /** Function constructor **/
    public BigDecimalValue(BigDecimalValue result, NumberOperations function, BigDecimalValue[] params) {
        super(result, function, params);
        this.value = result.getValue();
    }

    @Override
    public BigDecimalValue copy(String name) {
        return copy(this, name);
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
        return value.intValue();
    }

    public String printValue() {
        return value.toString();
    }

    public int compareTo(Number o) {
        if (o == null) {
            return 1;
        } else if (o instanceof BigDecimalValue) {
            return value.compareTo(((BigDecimalValue) o).getValue());
        } else {
            throw new OpenlNotCheckedException("Can`t compare BigDecimalValue with unknown type.");
        }
    }

    public static BigDecimalValue abs(BigDecimalValue value) {
        // evaluate result
        BigDecimalValue result = new BigDecimalValue(value.getValue().abs());
        // create instance with information about last operation
        return new BigDecimalValue(result, NumberOperations.ABS, new BigDecimalValue[] { value });
    }

    public static BigDecimalValue inc(BigDecimalValue value) {
        return add(value, new BigDecimalValue(BigDecimal.ONE));
    }

    public static BigDecimalValue dec(BigDecimalValue value) {
        return subtract(value, new BigDecimalValue(BigDecimal.ONE));
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BigDecimalValue) {
            BigDecimalValue secondObj = (BigDecimalValue) obj;
            return Operators.eq(value, secondObj.getValue());
        }

        return false;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    public static BigDecimalValue positive(BigDecimalValue value) {
        return value;
    }
    
    private static BigDecimal[] unwrap(BigDecimalValue[] values) {
        if (ArrayTool.noNulls(values)) {
            BigDecimal[] primitiveArray = new BigDecimal[values.length];
            for (int i = 0; i < values.length; i++) {
                primitiveArray[i] = values[i].getValue();
            }
            return primitiveArray;
        }
        return new BigDecimal[0];
    }

}
