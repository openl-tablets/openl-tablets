package org.openl.meta;

import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.meta.number.NumberOperations;

public class FloatValue extends ExplanationNumberValue<FloatValue> {

    private static final long serialVersionUID = -8235832583740963916L;
    
    private float value;
    
    public static FloatValue add(FloatValue floatValue1, FloatValue floatValue2) {

        if (floatValue1 == null || floatValue1.getValue() == 0) {
            return floatValue2;
        }

        if (floatValue2 == null || floatValue2.getValue() == 0) {
            return floatValue1;
        }

        return new FloatValue(floatValue1, floatValue2, floatValue1.getValue() + floatValue2.getValue(), 
            NumberOperations.ADD.toString(), false);
    }    
    
    public static FloatValue rem(FloatValue floatValue1, FloatValue floatValue2) {
        return new FloatValue(floatValue1, floatValue2, floatValue1.getValue() % floatValue2.getValue(), 
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
        return new FloatValue(floatValue1, floatValue2, floatValue1.getValue() / floatValue2.getValue(), 
            NumberOperations.DIVIDE.toString(), true);
    }

    public static boolean eq(FloatValue floatValue1, FloatValue floatValue2) {
        return floatValue1.getValue() == floatValue2.getValue();
    }

    public static boolean ge(FloatValue floatValue1, FloatValue floatValue2) {
        return floatValue1.getValue() >= floatValue2.getValue();
    }

    public static boolean gt(FloatValue floatValue1, FloatValue floatValue2) {
        return floatValue1.getValue() > floatValue2.getValue();
    }

    public static boolean le(FloatValue floatValue1, FloatValue floatValue2) {
        return floatValue1.getValue() <= floatValue2.getValue();
    }

    public static boolean lt(FloatValue floatValue1, FloatValue floatValue2) {
        return floatValue1.getValue() < floatValue2.getValue();
    }

    public static FloatValue max(FloatValue floatValue1, FloatValue floatValue2) {
        return new FloatValue(floatValue2.getValue() > floatValue1.getValue() ? floatValue2 : floatValue1,
            NumberOperations.MAX.toString(),
            new FloatValue[] { floatValue1, floatValue2 });
    }

    public static FloatValue min(FloatValue floatValue1, FloatValue floatValue2) {
        return new FloatValue(floatValue2.getValue() < floatValue1.getValue() ? floatValue2 : floatValue1,
            NumberOperations.MIN.toString(),
            new FloatValue[] { floatValue1, floatValue2 });
    }

    public static FloatValue multiply(FloatValue floatValue1, FloatValue floatValue2) {
        return new FloatValue(floatValue1, floatValue2, floatValue1.getValue() * floatValue2.getValue(), 
            NumberOperations.MULTIPLY.toString(), true);
    }

    public static boolean ne(FloatValue floatValue1, FloatValue floatValue2) {
        return floatValue1.getValue() != floatValue2.getValue();
    }

    public static FloatValue negative(FloatValue dv) {
        FloatValue neg = new FloatValue(-dv.getValue());
        neg.setMetaInfo(dv.getMetaInfo());

        return neg;
    }

    public static FloatValue pow(FloatValue floatValue1, FloatValue floatValue2) {
        return new FloatValue(new FloatValue((long)Math.pow(floatValue1.getValue(), floatValue2.getValue())), 
            NumberOperations.POW.toString(), new FloatValue[] { floatValue1, floatValue2 });
    }

    public static FloatValue round(FloatValue floatValue1) {
        return new FloatValue(new FloatValue((long)Math.round(floatValue1.getValue())), 
            NumberOperations.ROUND.toString(), new FloatValue[] { floatValue1 });
    }

    public static FloatValue subtract(FloatValue floatValue1, FloatValue floatValue2) {

        if (floatValue2 == null || floatValue2.getValue() == 0) {
            return floatValue1;
        }

        return new FloatValue(floatValue1, floatValue2, floatValue1.getValue() - floatValue2.getValue(), 
            NumberOperations.SUBTRACT.toString(), false);
    }
    
    public FloatValue(float value) {
        this.value = value;
    }
    
    public FloatValue(String valueString) {        
        value = Long.parseLong(valueString);
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
        this.value = result.longValue();
    }

    @Override
    public FloatValue copy(String name) {        
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

}
