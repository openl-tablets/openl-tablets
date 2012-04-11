package org.openl.meta;

import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.meta.number.NumberOperations;

public class ByteValue extends ExplanationNumberValue<ByteValue> {

    private static final long serialVersionUID = -3137978912171407672L;
    
    private byte value;
    
    public static ByteValue add(ByteValue byteValue1, ByteValue byteValue2) {

        if (byteValue1 == null || byteValue1.getValue() == 0) {
            return byteValue2;
        }

        if (byteValue2 == null || byteValue2.getValue() == 0) {
            return byteValue1;
        }

        return new ByteValue(byteValue1, byteValue2, (byte)(byteValue1.getValue() + byteValue2.getValue()), 
            NumberOperations.ADD.toString(), false);
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
            ByteValue lv = new ByteValue(value, NumberOperations.COPY.toString(), new ByteValue[] { value });
            lv.setName(name);

            return lv;
        }

        return value;
    }

    public static ByteValue divide(ByteValue byteValue1, ByteValue byteValue2) {
        return new ByteValue(byteValue1, byteValue2, (byte)(byteValue1.getValue() / byteValue2.getValue()),
            NumberOperations.DIVIDE.toString(), true);
    }

    public static boolean eq(ByteValue byteValue1, ByteValue byteValue2) {
        return byteValue1.getValue() == byteValue2.getValue();
    }

    public static boolean ge(ByteValue byteValue1, ByteValue byteValue2) {
        return byteValue1.getValue() >= byteValue2.getValue();
    }

    public static boolean gt(ByteValue byteValue1, ByteValue byteValue2) {
        return byteValue1.getValue() > byteValue2.getValue();
    }

    public static boolean le(ByteValue byteValue1, ByteValue byteValue2) {
        return byteValue1.getValue() <= byteValue2.getValue();
    }

    public static boolean lt(ByteValue byteValue1, ByteValue byteValue2) {
        return byteValue1.getValue() < byteValue2.getValue();
    }

    public static ByteValue max(ByteValue byteValue1, ByteValue byteValue2) {
        return new ByteValue(byteValue2.getValue() > byteValue1.getValue() ? byteValue2 : byteValue1,
            NumberOperations.MAX.toString(),
            new ByteValue[] { byteValue1, byteValue2 });
    }

    public static ByteValue min(ByteValue byteValue1, ByteValue byteValue2) {
        return new ByteValue(byteValue2.getValue() < byteValue1.getValue() ? byteValue2 : byteValue1,
            NumberOperations.MIN.toString(),
            new ByteValue[] { byteValue1, byteValue2 });
    }

    public static ByteValue multiply(ByteValue byteValue1, ByteValue byteValue2) {
        return new ByteValue(byteValue1, byteValue2, (byte)(byteValue1.getValue() * byteValue2.getValue()), 
            NumberOperations.MULTIPLY.toString(), true);
    }

    public static boolean ne(ByteValue byteValue1, ByteValue byteValue2) {
        return byteValue1.getValue() != byteValue2.getValue();
    }

    public static ByteValue negative(ByteValue dv) {
        ByteValue neg = new ByteValue((byte)-dv.getValue());
        neg.setMetaInfo(dv.getMetaInfo());

        return neg;
    }

    public static ByteValue pow(ByteValue byteValue1, ByteValue byteValue2) {
        return new ByteValue(new ByteValue((byte)Math.pow(byteValue1.getValue(), byteValue2.getValue())), 
            NumberOperations.POW.toString(), new ByteValue[] { byteValue1, byteValue2 });
    }

    public static ByteValue round(ByteValue byteValue1) {
        return new ByteValue(new ByteValue((byte)Math.round(byteValue1.getValue())), 
            NumberOperations.ROUND.toString(), new ByteValue[] { byteValue1 });
    }

    public static ByteValue subtract(ByteValue byteValue1, ByteValue byteValue2) {

        if (byteValue2 == null || byteValue2.getValue() == 0) {
            return byteValue1;
        }

        return new ByteValue(byteValue1, byteValue2, (byte)(byteValue1.getValue() - byteValue2.getValue()), 
            NumberOperations.SUBTRACT.toString(), false);
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
    public ByteValue(ByteValue byteValue1, ByteValue byteValue2, byte value, String operand, boolean isMultiplicative) {
        super(byteValue1, byteValue2, operand, isMultiplicative);
        this.value = value;
    }

    /**Function constructor**/
    public ByteValue(ByteValue result, String functionName, ByteValue[] params) {
        super(result, functionName, params);
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
            return value == secondObj.byteValue();
        }

        return false;
    }

    @Override
    public int hashCode() {
        return ((Byte) value).hashCode();
    }

}
