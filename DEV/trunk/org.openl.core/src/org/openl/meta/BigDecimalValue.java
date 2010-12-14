package org.openl.meta;

import java.math.BigDecimal;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.meta.number.NumberOperations;

public class BigDecimalValue extends ExplanationNumberValue<BigDecimalValue> {

    private static final long serialVersionUID = 1996508840075924034L;
    
    private BigDecimal value;
    
    public static BigDecimalValue add(BigDecimalValue bigDecimalValue1, BigDecimalValue bigDecimalValue2) {

        if (bigDecimalValue1 == null) {
            return bigDecimalValue2;
        }

        if (bigDecimalValue2 == null) {
            return bigDecimalValue1;
        }

        return new BigDecimalValue(bigDecimalValue1, bigDecimalValue2, 
            bigDecimalValue1.getValue().add(bigDecimalValue2.getValue()), NumberOperations.ADD.toString(), false);
    }    
    
    // ******* Autocasts *************
    
    public static BigDecimalValue autocast(byte x, BigDecimalValue y) {
        return new BigDecimalValue(String.valueOf(x));
    }

    public static BigDecimalValue autocast(short x, BigDecimalValue y) {
        return new BigDecimalValue(String.valueOf(x));
    }
    
    public static BigDecimalValue autocast(char x, BigDecimalValue y) {
        return new BigDecimalValue(String.valueOf((int)x));    
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
            BigDecimalValue lv = new BigDecimalValue(value, NumberOperations.COPY.toString(), 
                new BigDecimalValue[] { value });
            lv.setName(name);

            return lv;
        }

        return value;
    }

    public static BigDecimalValue divide(BigDecimalValue bigDecimalValue1, BigDecimalValue bigDecimalValue2) {
        if (bigDecimalValue1 == null && bigDecimalValue2 == null) {
            return null;
        } 
        return new BigDecimalValue(bigDecimalValue1, bigDecimalValue2, bigDecimalValue1.getValue().divide(bigDecimalValue2.getValue()),
            NumberOperations.DIVIDE.toString(), true);
    }

    public static boolean eq(BigDecimalValue bigDecimalValue1, BigDecimalValue bigDecimalValue2) {
        if (bigDecimalValue1 == null || bigDecimalValue2 == null) {
            return false;
        }
        return bigDecimalValue1.equals(bigDecimalValue2);
    }

    public static boolean ge(BigDecimalValue bigDecimalValue1, BigDecimalValue bigDecimalValue2) {
        if (bigDecimalValue1 == null || bigDecimalValue2 == null) {
            return false;
        }
        return bigDecimalValue1.compareTo(bigDecimalValue2) >= 0;
    }

    public static boolean gt(BigDecimalValue bigDecimalValue1, BigDecimalValue bigDecimalValue2) {
        if (bigDecimalValue1 == null || bigDecimalValue2 == null) {
            return false;
        }
        return bigDecimalValue1.compareTo(bigDecimalValue2) > 0;
    }

    public static boolean le(BigDecimalValue bigDecimalValue1, BigDecimalValue bigDecimalValue2) {
        if (bigDecimalValue1 == null || bigDecimalValue2 == null) {
            return false;
        }
        return bigDecimalValue1.compareTo(bigDecimalValue2) <= 0;
    }

    public static boolean lt(BigDecimalValue bigDecimalValue1, BigDecimalValue bigDecimalValue2) {
        if (bigDecimalValue1 == null || bigDecimalValue2 == null) {
            return false;
        }
        return bigDecimalValue1.compareTo(bigDecimalValue2) < 0;
    }

    public static BigDecimalValue max(BigDecimalValue bigDecimalValue1, BigDecimalValue bigDecimalValue2) {        
        BigDecimalValue maxValue = null;
        if (bigDecimalValue1 == null && bigDecimalValue2 == null) {
            return null;
        }        
        if (bigDecimalValue1 != null) {
            maxValue = bigDecimalValue1.compareTo(bigDecimalValue2) > 0 ? bigDecimalValue1 : bigDecimalValue2;            
        } else if (bigDecimalValue2 != null) {
            maxValue = bigDecimalValue2.compareTo(bigDecimalValue1) > 0 ? bigDecimalValue2 : bigDecimalValue1;
        } 
        return new BigDecimalValue(maxValue, NumberOperations.MAX.toString(), 
            new BigDecimalValue[] { bigDecimalValue1, bigDecimalValue2 });
    }
    

    public static BigDecimalValue min(BigDecimalValue bigDecimalValue1, BigDecimalValue bigDecimalValue2) {
        BigDecimalValue minValue = null;
        if (bigDecimalValue1 == null && bigDecimalValue2 == null) {
            return null;
        } 
        if (bigDecimalValue1 != null) {
            minValue = bigDecimalValue1.compareTo(bigDecimalValue2) < 0 ? bigDecimalValue1 : bigDecimalValue2;            
        } else if (bigDecimalValue2 != null) {
            minValue = bigDecimalValue2.compareTo(bigDecimalValue1) < 0 ? bigDecimalValue2 : bigDecimalValue1;
        }
        if (minValue != null) {
            return new BigDecimalValue(minValue, NumberOperations.MIN.toString(), 
                new BigDecimalValue[] { bigDecimalValue1, bigDecimalValue2 });
        } else {
            return null;
        }        
    }

    public static BigDecimalValue multiply(BigDecimalValue bigDecimalValue1, BigDecimalValue bigDecimalValue2) {
        if (bigDecimalValue1 == null && bigDecimalValue2 == null) {
            return null;
        }
        return new BigDecimalValue(bigDecimalValue1, bigDecimalValue2, 
            bigDecimalValue1.getValue().multiply(bigDecimalValue2.getValue()), NumberOperations.MULTIPLY.toString(), 
            true);
    }    

    public static boolean ne(BigDecimalValue bigDecimalValue1, BigDecimalValue bigDecimalValue2) {        
        return bigDecimalValue1.getValue() != bigDecimalValue2.getValue();
    }

    public static BigDecimalValue negative(BigDecimalValue bigIntValue) {
        if (bigIntValue == null) {
            return null;
        }
        BigDecimalValue neg = new BigDecimalValue(bigIntValue.getValue().negate());
        neg.setMetaInfo(bigIntValue.getMetaInfo());

        return neg;
    }

    public static BigDecimalValue pow(BigDecimalValue bigDecimalValue1, BigDecimalValue bigDecimalValue2) {   
        if (bigDecimalValue1 == null && bigDecimalValue2 == null) {
            return null;
        }
        return new BigDecimalValue(new BigDecimalValue(bigDecimalValue1.getValue().pow(bigDecimalValue2.getValue().intValue())),
            NumberOperations.POW.toString(), new BigDecimalValue[] { bigDecimalValue1, bigDecimalValue2 });
    }

    public static BigDecimalValue subtract(BigDecimalValue bigDecimalValue1, BigDecimalValue bigDecimalValue2) {

        if (bigDecimalValue2 == null) {
            return bigDecimalValue1;
        }

        return new BigDecimalValue(bigDecimalValue1, bigDecimalValue2, 
            bigDecimalValue1.getValue().subtract(bigDecimalValue2.getValue()), NumberOperations.SUBTRACT.toString(), 
            false);
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
    
    /**Formula constructor**/
    public BigDecimalValue(BigDecimalValue bigDecimalValue1, BigDecimalValue bigDecimalValue2, BigDecimal value, 
            String operand, boolean isMultiplicative) {
        super(bigDecimalValue1, bigDecimalValue2, operand, isMultiplicative);
        this.value = value;
    }
    
    /**Function constructor**/
    public BigDecimalValue(BigDecimalValue result, String functionName, BigDecimalValue[] params) {
        super(result, functionName, params);
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
            return value.compareTo(((BigDecimalValue)o).getValue());
        } else {
            throw new OpenlNotCheckedException("Can`t compare BigDecimalValue with unknown type.");
        }
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
            return value.equals(secondObj.getValue());
        }

        return false;
    }

    @Override
    public int hashCode() {
        return  value.hashCode();
    }

}
