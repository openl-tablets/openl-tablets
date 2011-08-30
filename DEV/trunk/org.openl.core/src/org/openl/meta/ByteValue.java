package org.openl.meta;

import org.apache.commons.lang.ArrayUtils;
import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.meta.number.Formulas;
import org.openl.meta.number.LogicalExpressions;
import org.openl.meta.number.NumberOperations;
import org.openl.util.ArrayTool;
import org.openl.util.math.MathUtils;
import org.openl.binding.impl.Operators;
import org.openl.exception.OpenlNotCheckedException;


public class ByteValue extends ExplanationNumberValue<ByteValue> {

    private static final long serialVersionUID = -3137978912171407672L;
    
    // <<< INSERT Functions >>>
	// generate zero for types that are wrappers over primitives
	private static final org.openl.meta.ByteValue ZERO1 = new org.openl.meta.ByteValue((byte)0);

	private byte value;


	public static boolean eq(org.openl.meta.ByteValue value1, org.openl.meta.ByteValue value2) {
		validate(value1, value2, LogicalExpressions.EQ.toString());
		
		return Operators.eq(value1.getValue(), value2.getValue());		
	}
	public static boolean ge(org.openl.meta.ByteValue value1, org.openl.meta.ByteValue value2) {
		validate(value1, value2, LogicalExpressions.GE.toString());
		
		return Operators.ge(value1.getValue(), value2.getValue());		
	}
	public static boolean gt(org.openl.meta.ByteValue value1, org.openl.meta.ByteValue value2) {
		validate(value1, value2, LogicalExpressions.GT.toString());
		
		return Operators.gt(value1.getValue(), value2.getValue());		
	}
	public static boolean le(org.openl.meta.ByteValue value1, org.openl.meta.ByteValue value2) {
		validate(value1, value2, LogicalExpressions.LE.toString());
		
		return Operators.le(value1.getValue(), value2.getValue());		
	}
	public static boolean lt(org.openl.meta.ByteValue value1, org.openl.meta.ByteValue value2) {
		validate(value1, value2, LogicalExpressions.LT.toString());
		
		return Operators.lt(value1.getValue(), value2.getValue());		
	}
	public static boolean ne(org.openl.meta.ByteValue value1, org.openl.meta.ByteValue value2) {
		validate(value1, value2, LogicalExpressions.NE.toString());
		
		return Operators.ne(value1.getValue(), value2.getValue());		
	}

	public static org.openl.meta.ByteValue avg(org.openl.meta.ByteValue[] values) {
		if (ArrayUtils.isEmpty(values)) {
            return null;
        }
		byte[] primitiveArray = unwrap(values);
		byte avg = MathUtils.avg(primitiveArray);
		return new org.openl.meta.ByteValue(new org.openl.meta.ByteValue(avg), NumberOperations.AVG, values);
	}
	public static org.openl.meta.ByteValue sum(org.openl.meta.ByteValue[] values) {
		if (ArrayUtils.isEmpty(values)) {
            return null;
        }
		byte[] primitiveArray = unwrap(values);
		byte sum = MathUtils.sum(primitiveArray);
		return new org.openl.meta.ByteValue(new org.openl.meta.ByteValue(sum), NumberOperations.SUM, values);
	}
	public static org.openl.meta.ByteValue median(org.openl.meta.ByteValue[] values) {
		if (ArrayUtils.isEmpty(values)) {
            return null;
        }
		byte[] primitiveArray = unwrap(values);
		byte median = MathUtils.median(primitiveArray);
		return new org.openl.meta.ByteValue(new org.openl.meta.ByteValue(median), NumberOperations.MEDIAN, values);
	}

	public static org.openl.meta.ByteValue max(org.openl.meta.ByteValue value1, org.openl.meta.ByteValue value2) {
		validate(value1, value2, NumberOperations.MAX.toString());
		
		return new org.openl.meta.ByteValue(MathUtils.max(value1.getValue(), value2.getValue()) ? value1 : value2,
            NumberOperations.MAX,
            new org.openl.meta.ByteValue[] { value1, value2 });
	}
	public static org.openl.meta.ByteValue min(org.openl.meta.ByteValue value1, org.openl.meta.ByteValue value2) {
		validate(value1, value2, NumberOperations.MIN.toString());
		
		return new org.openl.meta.ByteValue(MathUtils.min(value1.getValue(), value2.getValue()) ? value1 : value2,
            NumberOperations.MIN,
            new org.openl.meta.ByteValue[] { value1, value2 });
	}

	public static org.openl.meta.ByteValue max(org.openl.meta.ByteValue[] values) {
		org.openl.meta.ByteValue result = (org.openl.meta.ByteValue) MathUtils.max(values); 		
		
		return new org.openl.meta.ByteValue((org.openl.meta.ByteValue) getAppropriateValue(values, result), 
            NumberOperations.MAX_IN_ARRAY, values);
	}
	public static org.openl.meta.ByteValue min(org.openl.meta.ByteValue[] values) {
		org.openl.meta.ByteValue result = (org.openl.meta.ByteValue) MathUtils.min(values); 		
		
		return new org.openl.meta.ByteValue((org.openl.meta.ByteValue) getAppropriateValue(values, result), 
            NumberOperations.MIN_IN_ARRAY, values);
	}

	public static org.openl.meta.ByteValue copy(org.openl.meta.ByteValue value, String name) {
		if (value.getName() == null) {
            value.setName(name);

            return value;
        } else if (!value.getName().equals(name)) {
        	org.openl.meta.ByteValue result = new org.openl.meta.ByteValue (value, NumberOperations.COPY, 
        		new org.openl.meta.ByteValue[] { value });
        	result.setName(name);

            return result;
        }
        return value;
	}
	
	 	
	
	//ADD
	public static org.openl.meta.ByteValue add(org.openl.meta.ByteValue value1, org.openl.meta.ByteValue value2) {
		// temporary commented to support operations with nulls
		//
		//		validate(value1, value2, Formulas.ADD.toString());
		//conditions for classes that are wrappers over primitives
		if (value1 == null || value1.getValue() == 0) {
            return value2;
        }

        if (value2 == null || value2.getValue() == 0) {
            return value1;
        }
        
		return new org.openl.meta.ByteValue(value1, value2, Operators.add(value1.getValue(), value2.getValue()), 
			Formulas.ADD);	
	}
	
	// MULTIPLY
	public static org.openl.meta.ByteValue multiply(org.openl.meta.ByteValue value1, org.openl.meta.ByteValue value2) {
		// temporary commented to support operations with nulls
		//
		//		validate(value1, value2, Formulas.MULTIPLY.toString());
		if (value1 == null) {
			return ZERO1;
		}
		
		if (value2 == null) {
			return ZERO1;
		}
		
		return new org.openl.meta.ByteValue(value1, value2, Operators.multiply(value1.getValue(), value2.getValue()), 
			Formulas.MULTIPLY);		
	}
	
	//SUBTRACT
	public static org.openl.meta.ByteValue subtract(org.openl.meta.ByteValue value1, org.openl.meta.ByteValue value2) {
		// temporary commented to support operations with nulls
		//
		//		validate(value1, value2, Formulas.SUBTRACT.toString());
		
		if (value1 == null) {
			return negative(value2);
		}
		
		if (value2 == null) {
			return value1;
		}
		
		return new org.openl.meta.ByteValue(value1, value2, Operators.subtract(value1.getValue(), value2.getValue()), 
			Formulas.SUBTRACT);		
	}
	
	public static org.openl.meta.ByteValue divide(org.openl.meta.ByteValue value1, org.openl.meta.ByteValue value2) {
		// temporary commented to support operations with nulls
		//
		//		validate(value1, value2, Formulas.DIVIDE.toString());
		if (value1 == null) {
			if (value2 != null && value2.doubleValue() != 0) {
				return ZERO1;
			}
		}
		
		if (value2 == null || value2.doubleValue() == 0) {
			throw new OpenlNotCheckedException("Division by zero");
		}
		
		return new org.openl.meta.ByteValue(value1, value2, Operators.divide(value1.getValue(), value2.getValue()), 
			Formulas.DIVIDE);		
	}
	
	
	// QUAOTIENT
	public static LongValue quotient(org.openl.meta.ByteValue number, org.openl.meta.ByteValue divisor) {
        if (number != null && divisor != null) {
            LongValue result = new LongValue(MathUtils.quotient(number.getValue(), divisor.getValue()));
            return new LongValue(result, NumberOperations.QUOTIENT, null);
        }
        return null;
    }
	
	// generated product function for types that are wrappers over primitives
	public static DoubleValue product(org.openl.meta.ByteValue[] values) {
		if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        byte[] primitiveArray = unwrap(values);
        double product = MathUtils.product(primitiveArray);
        // we loose the parameters, but not the result of computation.
        return new DoubleValue(new DoubleValue(product), NumberOperations.PRODUCT, null);
	}
	
	public static org.openl.meta.ByteValue mod(org.openl.meta.ByteValue number, org.openl.meta.ByteValue divisor) {
        if (number != null && divisor != null) {
            org.openl.meta.ByteValue result = new org.openl.meta.ByteValue(MathUtils.mod(number.getValue(), divisor.getValue()));
            return new org.openl.meta.ByteValue(result, NumberOperations.MOD, new org.openl.meta.ByteValue[]{number, divisor} );
        }
        return null;
    }
    
    public static org.openl.meta.ByteValue small(org.openl.meta.ByteValue[] values, int position) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        byte[] primitiveArray = unwrap(values);
        byte small = MathUtils.small(primitiveArray, position);
        return new org.openl.meta.ByteValue((org.openl.meta.ByteValue) getAppropriateValue(values, new org.openl.meta.ByteValue(small)), 
            NumberOperations.SMALL, values);
    }
    
    public static org.openl.meta.ByteValue big(org.openl.meta.ByteValue[] values, int position) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        byte[] primitiveArray = unwrap(values);
        byte big = MathUtils.big(primitiveArray, position);
        return new org.openl.meta.ByteValue((org.openl.meta.ByteValue) getAppropriateValue(values, new org.openl.meta.ByteValue(big)), 
            NumberOperations.BIG, values);
    }
    
    public static org.openl.meta.ByteValue pow(org.openl.meta.ByteValue value1, org.openl.meta.ByteValue value2) {
        validate(value1, value2, NumberOperations.POW);
        
        return new org.openl.meta.ByteValue(new org.openl.meta.ByteValue(Operators.pow(value1.getValue(), value2.getValue())), 
            NumberOperations.POW, new org.openl.meta.ByteValue[] { value1, value2 });
    }
    
    public static org.openl.meta.ByteValue abs(org.openl.meta.ByteValue value) {
        validate(value, NumberOperations.ABS);
        // evaluate result
        org.openl.meta.ByteValue result = new org.openl.meta.ByteValue(Operators.abs(value.getValue()));
        // create instance with information about last operation
        return new org.openl.meta.ByteValue(result, NumberOperations.ABS, new org.openl.meta.ByteValue[] { value });
    }
    
    public static org.openl.meta.ByteValue negative(org.openl.meta.ByteValue value) {
        return multiply(value, new org.openl.meta.ByteValue("-1"));
    }
    
    public static org.openl.meta.ByteValue inc(org.openl.meta.ByteValue value) {
        return add(value, new org.openl.meta.ByteValue("1"));
    }
    
    public static org.openl.meta.ByteValue positive(org.openl.meta.ByteValue value) {
        return value;
    }
    
    public static org.openl.meta.ByteValue dec(org.openl.meta.ByteValue value) {
        return subtract(value, new org.openl.meta.ByteValue("1"));
    }
    
    // Autocasts
    
	public static org.openl.meta.ByteValue autocast(byte x, org.openl.meta.ByteValue y) {
		return new org.openl.meta.ByteValue((byte) x);
	}		
	public static org.openl.meta.ByteValue autocast(short x, org.openl.meta.ByteValue y) {
		return new org.openl.meta.ByteValue((byte) x);
	}		
	public static org.openl.meta.ByteValue autocast(int x, org.openl.meta.ByteValue y) {
		return new org.openl.meta.ByteValue((byte) x);
	}		
	public static org.openl.meta.ByteValue autocast(long x, org.openl.meta.ByteValue y) {
		return new org.openl.meta.ByteValue((byte) x);
	}		
	public static org.openl.meta.ByteValue autocast(float x, org.openl.meta.ByteValue y) {
		return new org.openl.meta.ByteValue((byte) x);
	}		
	public static org.openl.meta.ByteValue autocast(double x, org.openl.meta.ByteValue y) {
		return new org.openl.meta.ByteValue((byte) x);
	}		
    
    // Constructors
    public ByteValue(byte value) {
        this.value = value;
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
    public ByteValue(org.openl.meta.ByteValue lv1, org.openl.meta.ByteValue lv2, byte value, Formulas operand) {
        super(lv1, lv2, operand);
        this.value = value;
    }    

    @Override
    public org.openl.meta.ByteValue copy(String name) {
        return copy(this, name);        
    }    
    
    public String printValue() {        
        return String.valueOf(value);
    }
    
    public byte getValue() {        
        return value;
    }
    
    public void setValue(byte value) {
        this.value = value;
    }
	
	
	


	
	 
      
                                        	 // <<< END INSERT Functions >>>
    
    // ******* Autocasts *************
    
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

    public ByteValue(String valueString) {        
        value = Byte.parseByte(valueString);
    }
    
    /**Function constructor**/
    public ByteValue(ByteValue result, NumberOperations function, ByteValue[] params) {
        super(result, function, params);
        this.value = result.byteValue();
    }
    
    @Override
    public double doubleValue() {        
        return (double) value;
    }

    @Override
    public float floatValue() {        
        return (float) value;
    }

    @Override
    public int intValue() {        
        return (int) value;
    }
    
    @Override
    public long longValue() {        
        return (long) value;
    }
    
    public int compareTo(Number o) {        
        return (int)(value - o.byteValue());
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
    
    private static byte[] unwrap(ByteValue[] values) {
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
