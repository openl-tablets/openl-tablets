package org.openl.meta;

import java.math.BigInteger;

import org.apache.commons.lang.ArrayUtils;
import org.openl.binding.impl.Operators;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.meta.number.Formulas;
import org.openl.meta.number.LogicalExpressions;
import org.openl.meta.number.NumberOperations;
import org.openl.util.ArrayTool;
import org.openl.util.math.MathUtils;

public class BigIntegerValue extends ExplanationNumberValue<BigIntegerValue> {

    private static final long serialVersionUID = -3936317402079096501L;
    
    // <<< INSERT Functions >>>
	private java.math.BigInteger value;

	public static org.openl.meta.BigIntegerValue add(org.openl.meta.BigIntegerValue value1, org.openl.meta.BigIntegerValue value2) {
		validate(value1, value2, Formulas.ADD.toString());
		
		return new org.openl.meta.BigIntegerValue(value1, value2, Operators.add(value1.getValue(), value2.getValue()), 
			Formulas.ADD);		
	}
	public static org.openl.meta.BigIntegerValue multiply(org.openl.meta.BigIntegerValue value1, org.openl.meta.BigIntegerValue value2) {
		validate(value1, value2, Formulas.MULTIPLY.toString());
		
		return new org.openl.meta.BigIntegerValue(value1, value2, Operators.multiply(value1.getValue(), value2.getValue()), 
			Formulas.MULTIPLY);		
	}
	public static org.openl.meta.BigIntegerValue subtract(org.openl.meta.BigIntegerValue value1, org.openl.meta.BigIntegerValue value2) {
		validate(value1, value2, Formulas.SUBTRACT.toString());
		
		return new org.openl.meta.BigIntegerValue(value1, value2, Operators.subtract(value1.getValue(), value2.getValue()), 
			Formulas.SUBTRACT);		
	}
	public static org.openl.meta.BigIntegerValue divide(org.openl.meta.BigIntegerValue value1, org.openl.meta.BigIntegerValue value2) {
		validate(value1, value2, Formulas.DIVIDE.toString());
		
		return new org.openl.meta.BigIntegerValue(value1, value2, Operators.divide(value1.getValue(), value2.getValue()), 
			Formulas.DIVIDE);		
	}
	public static org.openl.meta.BigIntegerValue rem(org.openl.meta.BigIntegerValue value1, org.openl.meta.BigIntegerValue value2) {
		validate(value1, value2, Formulas.REM.toString());
		
		return new org.openl.meta.BigIntegerValue(value1, value2, Operators.rem(value1.getValue(), value2.getValue()), 
			Formulas.REM);		
	}

	public static boolean eq(org.openl.meta.BigIntegerValue value1, org.openl.meta.BigIntegerValue value2) {
		validate(value1, value2, LogicalExpressions.EQ.toString());
		
		return Operators.eq(value1.getValue(), value2.getValue());		
	}
	public static boolean ge(org.openl.meta.BigIntegerValue value1, org.openl.meta.BigIntegerValue value2) {
		validate(value1, value2, LogicalExpressions.GE.toString());
		
		return Operators.ge(value1.getValue(), value2.getValue());		
	}
	public static boolean gt(org.openl.meta.BigIntegerValue value1, org.openl.meta.BigIntegerValue value2) {
		validate(value1, value2, LogicalExpressions.GT.toString());
		
		return Operators.gt(value1.getValue(), value2.getValue());		
	}
	public static boolean le(org.openl.meta.BigIntegerValue value1, org.openl.meta.BigIntegerValue value2) {
		validate(value1, value2, LogicalExpressions.LE.toString());
		
		return Operators.le(value1.getValue(), value2.getValue());		
	}
	public static boolean lt(org.openl.meta.BigIntegerValue value1, org.openl.meta.BigIntegerValue value2) {
		validate(value1, value2, LogicalExpressions.LT.toString());
		
		return Operators.lt(value1.getValue(), value2.getValue());		
	}
	public static boolean ne(org.openl.meta.BigIntegerValue value1, org.openl.meta.BigIntegerValue value2) {
		validate(value1, value2, LogicalExpressions.NE.toString());
		
		return Operators.ne(value1.getValue(), value2.getValue());		
	}

	public static org.openl.meta.BigIntegerValue avg(org.openl.meta.BigIntegerValue[] values) {
		if (ArrayUtils.isEmpty(values)) {
            return null;
        }
		java.math.BigInteger[] primitiveArray = unwrap(values);
		java.math.BigInteger avg = MathUtils.avg(primitiveArray);
		return new org.openl.meta.BigIntegerValue(new org.openl.meta.BigIntegerValue(avg), NumberOperations.AVG, values);
	}
	public static org.openl.meta.BigIntegerValue sum(org.openl.meta.BigIntegerValue[] values) {
		if (ArrayUtils.isEmpty(values)) {
            return null;
        }
		java.math.BigInteger[] primitiveArray = unwrap(values);
		java.math.BigInteger sum = MathUtils.sum(primitiveArray);
		return new org.openl.meta.BigIntegerValue(new org.openl.meta.BigIntegerValue(sum), NumberOperations.SUM, values);
	}
	public static org.openl.meta.BigIntegerValue median(org.openl.meta.BigIntegerValue[] values) {
		if (ArrayUtils.isEmpty(values)) {
            return null;
        }
		java.math.BigInteger[] primitiveArray = unwrap(values);
		java.math.BigInteger median = MathUtils.median(primitiveArray);
		return new org.openl.meta.BigIntegerValue(new org.openl.meta.BigIntegerValue(median), NumberOperations.MEDIAN, values);
	}

	public static org.openl.meta.BigIntegerValue max(org.openl.meta.BigIntegerValue value1, org.openl.meta.BigIntegerValue value2) {
		validate(value1, value2, NumberOperations.MAX.toString());
		
		return new org.openl.meta.BigIntegerValue(MathUtils.max(value1.getValue(), value2.getValue()) ? value1 : value2,
            NumberOperations.MAX,
            new org.openl.meta.BigIntegerValue[] { value1, value2 });
	}
	public static org.openl.meta.BigIntegerValue min(org.openl.meta.BigIntegerValue value1, org.openl.meta.BigIntegerValue value2) {
		validate(value1, value2, NumberOperations.MIN.toString());
		
		return new org.openl.meta.BigIntegerValue(MathUtils.min(value1.getValue(), value2.getValue()) ? value1 : value2,
            NumberOperations.MIN,
            new org.openl.meta.BigIntegerValue[] { value1, value2 });
	}

	public static org.openl.meta.BigIntegerValue max(org.openl.meta.BigIntegerValue[] values) {
		org.openl.meta.BigIntegerValue result = (org.openl.meta.BigIntegerValue) MathUtils.max(values); 		
		
		return new org.openl.meta.BigIntegerValue((org.openl.meta.BigIntegerValue) getAppropriateValue(values, result), 
            NumberOperations.MAX_IN_ARRAY, values);
	}
	public static org.openl.meta.BigIntegerValue min(org.openl.meta.BigIntegerValue[] values) {
		org.openl.meta.BigIntegerValue result = (org.openl.meta.BigIntegerValue) MathUtils.min(values); 		
		
		return new org.openl.meta.BigIntegerValue((org.openl.meta.BigIntegerValue) getAppropriateValue(values, result), 
            NumberOperations.MIN_IN_ARRAY, values);
	}

	public static org.openl.meta.BigIntegerValue copy(org.openl.meta.BigIntegerValue value, String name) {
		if (value.getName() == null) {
            value.setName(name);

            return value;
        } else if (!value.getName().equals(name)) {
        	org.openl.meta.BigIntegerValue result = new org.openl.meta.BigIntegerValue (value, NumberOperations.COPY, 
        		new org.openl.meta.BigIntegerValue[] { value });
        	result.setName(name);

            return result;
        }
        return value;
	}
	
	// QUAOTIENT
	public static LongValue quotient(org.openl.meta.BigIntegerValue number, org.openl.meta.BigIntegerValue divisor) {
        if (number != null && divisor != null) {
            LongValue result = new LongValue(MathUtils.quotient(number.getValue(), divisor.getValue()));
            return new LongValue(result, NumberOperations.QUOTIENT, null);
        }
        return null;
    }
	
	// generated product function for big types
	public static org.openl.meta.BigIntegerValue product(org.openl.meta.BigIntegerValue[] values) {
		if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        java.math.BigInteger[] primitiveArray = unwrap(values);
        java.math.BigInteger product = MathUtils.product(primitiveArray);
        // we loose the parameters, but not the result of computation.
        return new org.openl.meta.BigIntegerValue(new org.openl.meta.BigIntegerValue(product), NumberOperations.PRODUCT, null);
	}
	
	public static org.openl.meta.BigIntegerValue mod(org.openl.meta.BigIntegerValue number, org.openl.meta.BigIntegerValue divisor) {
        if (number != null && divisor != null) {
            org.openl.meta.BigIntegerValue result = new org.openl.meta.BigIntegerValue(MathUtils.mod(number.getValue(), divisor.getValue()));
            return new org.openl.meta.BigIntegerValue(result, NumberOperations.MOD, new org.openl.meta.BigIntegerValue[]{number, divisor} );
        }
        return null;
    }
    
    public static org.openl.meta.BigIntegerValue small(org.openl.meta.BigIntegerValue[] values, int position) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        java.math.BigInteger[] primitiveArray = unwrap(values);
        java.math.BigInteger small = MathUtils.small(primitiveArray, position);
        return new org.openl.meta.BigIntegerValue((org.openl.meta.BigIntegerValue) getAppropriateValue(values, new org.openl.meta.BigIntegerValue(small)), 
            NumberOperations.SMALL, values);
    }
    
    public static org.openl.meta.BigIntegerValue big(org.openl.meta.BigIntegerValue[] values, int position) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        java.math.BigInteger[] primitiveArray = unwrap(values);
        java.math.BigInteger big = MathUtils.big(primitiveArray, position);
        return new org.openl.meta.BigIntegerValue((org.openl.meta.BigIntegerValue) getAppropriateValue(values, new org.openl.meta.BigIntegerValue(big)), 
            NumberOperations.BIG, values);
    }
    
    public static org.openl.meta.BigIntegerValue pow(org.openl.meta.BigIntegerValue value1, org.openl.meta.BigIntegerValue value2) {
        validate(value1, value2, NumberOperations.POW);
        
        return new org.openl.meta.BigIntegerValue(new org.openl.meta.BigIntegerValue(Operators.pow(value1.getValue(), value2.getValue())), 
            NumberOperations.POW, new org.openl.meta.BigIntegerValue[] { value1, value2 });
    }
    
    public static org.openl.meta.BigIntegerValue abs(org.openl.meta.BigIntegerValue value) {
        validate(value, NumberOperations.ABS);
        // evaluate result
        org.openl.meta.BigIntegerValue result = new org.openl.meta.BigIntegerValue(Operators.abs(value.getValue()));
        // create instance with information about last operation
        return new org.openl.meta.BigIntegerValue(result, NumberOperations.ABS, new org.openl.meta.BigIntegerValue[] { value });
    }
    
    public static org.openl.meta.BigIntegerValue negative(org.openl.meta.BigIntegerValue value) {
        return multiply(value, new org.openl.meta.BigIntegerValue("-1"));
    }
    
    public static org.openl.meta.BigIntegerValue inc(org.openl.meta.BigIntegerValue value) {
        return add(value, new org.openl.meta.BigIntegerValue("1"));
    }
    
    public static org.openl.meta.BigIntegerValue positive(org.openl.meta.BigIntegerValue value) {
        return value;
    }
    
    public static org.openl.meta.BigIntegerValue dec(org.openl.meta.BigIntegerValue value) {
        return subtract(value, new org.openl.meta.BigIntegerValue("1"));
    }
    
    // Autocasts
    
	public static org.openl.meta.BigIntegerValue autocast(byte x, org.openl.meta.BigIntegerValue y) {
		return new org.openl.meta.BigIntegerValue(String.valueOf(x));
	}		
	public static org.openl.meta.BigIntegerValue autocast(short x, org.openl.meta.BigIntegerValue y) {
		return new org.openl.meta.BigIntegerValue(String.valueOf(x));
	}		
	public static org.openl.meta.BigIntegerValue autocast(int x, org.openl.meta.BigIntegerValue y) {
		return new org.openl.meta.BigIntegerValue(String.valueOf(x));
	}		
	public static org.openl.meta.BigIntegerValue autocast(long x, org.openl.meta.BigIntegerValue y) {
		return new org.openl.meta.BigIntegerValue(String.valueOf(x));
	}		
	public static org.openl.meta.BigIntegerValue autocast(float x, org.openl.meta.BigIntegerValue y) {
		return new org.openl.meta.BigIntegerValue(String.valueOf(x));
	}		
	public static org.openl.meta.BigIntegerValue autocast(double x, org.openl.meta.BigIntegerValue y) {
		return new org.openl.meta.BigIntegerValue(String.valueOf(x));
	}		
    
    // Constructors
    public BigIntegerValue(java.math.BigInteger value) {
        this.value = value;
    }    

    public BigIntegerValue(java.math.BigInteger value, String name) {
        super(name);
        this.value = value;
    }

    public BigIntegerValue(java.math.BigInteger value, IMetaInfo metaInfo) {
        super(metaInfo);
        this.value = value;        
    }    

    /**Formula constructor**/
    public BigIntegerValue(org.openl.meta.BigIntegerValue lv1, org.openl.meta.BigIntegerValue lv2, java.math.BigInteger value, Formulas operand) {
        super(lv1, lv2, operand);
        this.value = value;
    }    

    @Override
    public org.openl.meta.BigIntegerValue copy(String name) {
        return copy(this, name);        
    }    
    
    public String printValue() {        
        return String.valueOf(value);
    }
    
    public java.math.BigInteger getValue() {        
        return value;
    }
    
    public void setValue(java.math.BigInteger value) {
        this.value = value;
    }
	
	
	


	
	 
      
                            // <<< END INSERT Functions >>>        

    // ******* Autocasts 8*************    

    public static BigIntegerValue autocast(BigInteger x, BigIntegerValue y) {
        if (x == null) {
            return null;
        }

        return new BigIntegerValue(x);
    }

    public static BigIntegerValue autocast(BigIntegerValue x, BigDecimalValue y) {
        if (x == null) {
            return null;
        }
        return new BigIntegerValue(String.valueOf(x.getValue()));
    }

    // ******* Casts 8*************

    public static byte cast(BigIntegerValue x, byte y) {
        return x.byteValue();
    }

    public static short cast(BigIntegerValue x, short y) {
        return x.shortValue();
    }

    public static char cast(BigIntegerValue x, char y) {
        return (char) x.intValue();
    }

    public static int cast(BigIntegerValue x, int y) {
        return x.intValue();
    }

    public static long cast(BigIntegerValue x, long y) {
        return x.longValue();
    }

    public static float cast(BigIntegerValue x, float y) {
        return x.floatValue();
    }

    public static double cast(BigIntegerValue x, double y) {
        return x.doubleValue();
    }

    public static BigInteger cast(BigIntegerValue x, BigInteger y) {
        if (x == null) {
            return null;
        }

        return x.getValue();
    }

    public static ByteValue cast(BigIntegerValue x, ByteValue y) {
        if (x == null) {
            return null;
        }
        return new ByteValue(x.byteValue());
    }

    public static ShortValue cast(BigIntegerValue x, ShortValue y) {
        if (x == null) {
            return null;
        }
        return new ShortValue(x.shortValue());
    }

    public static IntValue cast(BigIntegerValue x, IntValue y) {
        if (x == null) {
            return null;
        }
        return new IntValue(x.intValue());
    }

    public static LongValue cast(BigIntegerValue x, LongValue y) {
        if (x == null) {
            return null;
        }
        return new LongValue(x.longValue());
    }

    public static FloatValue cast(BigIntegerValue x, FloatValue y) {
        if (x == null) {
            return null;
        }
        return new FloatValue(x.floatValue());
    }

    public static DoubleValue cast(BigIntegerValue x, DoubleValue y) {
        if (x == null) {
            return null;
        }
        return new DoubleValue(x.doubleValue());
    }   

    public BigIntegerValue(String valueString) {
        value = new BigInteger(valueString);
    }    

    public BigIntegerValue(String value, String name) {
        super(name);
        this.value = new BigInteger(value);
    }

    public BigIntegerValue(String value, IMetaInfo metaInfo) {
        super(metaInfo);
        this.value = new BigInteger(value);
    }
    
    /** Function constructor **/
    public BigIntegerValue(BigIntegerValue result, NumberOperations function, BigIntegerValue[] params) {
        super(result, function, params);
        this.value = result.getValue();
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
        return value.longValue();
    }
    
    public int compareTo(Number o) {
        if (o == null) {
            return 1;
        } else if (o instanceof BigIntegerValue) {
            return value.compareTo(((BigIntegerValue) o).getValue());
        } else {
            throw new OpenlNotCheckedException("Can`t compare BigIntegerValue with unknown type.");
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BigIntegerValue) {
            BigIntegerValue secondObj = (BigIntegerValue) obj;
            return Operators.eq(value, secondObj.getValue());
        }

        return false;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
    
    private static BigInteger[] unwrap(BigIntegerValue[] values) {
        if (ArrayTool.noNulls(values)) {
            BigInteger[] unwrapArray = new BigInteger[values.length];
            for (int i = 0; i < values.length; i++) {
                unwrapArray[i] = values[i].value;
            }
            return unwrapArray;
        }
        return new BigInteger[0];
    }

}
