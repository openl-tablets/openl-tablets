package org.openl.meta;

import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.meta.number.NumberOperations;

public class LongValue extends ExplanationNumberValue<LongValue> {
   
    private static final long serialVersionUID = -437788531108803012L;
    private long value;

    public static LongValue add(LongValue lv1, LongValue lv2) {

        if (lv1 == null || lv1.getValue() == 0) {
            return lv2;
        }

        if (lv2 == null || lv2.getValue() == 0) {
            return lv1;
        }

        return new LongValue(lv1, lv2, lv1.getValue() + lv2.getValue(), NumberOperations.ADD.toString(), false);
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
        return new ByteValue(x.byteValue());
    }
        
    public static ShortValue cast(LongValue x, ShortValue y) {
        return new ShortValue(x.shortValue());
    }

    public static IntValue cast(LongValue x, IntValue y) {
        return new IntValue(x.intValue());
    }
    
    public static FloatValue cast(LongValue x, FloatValue y) {
        return new FloatValue(x.floatValue());
    }
    
    public static DoubleValue cast(LongValue x, DoubleValue y) {
        return new DoubleValue(x.doubleValue());
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
        return new LongValue(lv1, lv2, lv1.getValue() / lv2.getValue(), 
            NumberOperations.DIVIDE.toString(), true);
    }

    public static boolean eq(LongValue lv1, LongValue lv2) {
        return lv1.getValue() == lv2.getValue();
    }

    public static boolean ge(LongValue lv1, LongValue lv2) {
        return lv1.getValue() >= lv2.getValue();
    }

    public static boolean gt(LongValue lv1, LongValue lv2) {
        return lv1.getValue() > lv2.getValue();
    }

    public static boolean le(LongValue lv1, LongValue lv2) {
        return lv1.getValue() <= lv2.getValue();
    }

    public static boolean lt(LongValue lv1, LongValue lv2) {
        return lv1.getValue() < lv2.getValue();
    }

    public static LongValue max(LongValue lv1, LongValue lv2) {
        return new LongValue(lv2.getValue() > lv1.getValue() ? lv2 : lv1, NumberOperations.MAX.toString(),
            new LongValue[] { lv1, lv2 });
    }

    public static LongValue min(LongValue lv1, LongValue lv2) {
        return new LongValue(lv2.getValue() < lv1.getValue() ? lv2 : lv1, NumberOperations.MIN.toString(),
            new LongValue[] { lv1, lv2 });
    }

    public static LongValue multiply(LongValue lv1, LongValue lv2) {
        return new LongValue(lv1, lv2, lv1.getValue() * lv2.getValue(), 
            NumberOperations.MULTIPLY.toString(), true);
    }

    public static boolean ne(LongValue lv1, LongValue lv2) {
        return lv1.getValue() != lv2.getValue();
    }

    public static LongValue negative(LongValue dv) {
        LongValue neg = new LongValue(-dv.getValue());
        neg.setMetaInfo(dv.getMetaInfo());

        return neg;
    }

    public static LongValue pow(LongValue lv1, LongValue lv2) {
        return new LongValue(new LongValue((long)Math.pow(lv1.getValue(), lv2.getValue())), 
            NumberOperations.POW.toString(), new LongValue[] { lv1, lv2 });
    }

    public static LongValue round(LongValue lv1) {
        return new LongValue(new LongValue((long)Math.round(lv1.getValue())), 
            NumberOperations.ROUND.toString(), new LongValue[] { lv1 });
    }

    public static LongValue subtract(LongValue lv1, LongValue lv2) {

        if (lv2 == null || lv2.getValue() == 0) {
            return lv1;
        }

        return new LongValue(lv1, lv2, lv1.getValue() - lv2.getValue(), 
            NumberOperations.SUBTRACT.toString(), false);
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
}
