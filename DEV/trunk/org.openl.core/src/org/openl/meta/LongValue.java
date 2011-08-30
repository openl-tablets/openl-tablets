package org.openl.meta;

import org.apache.commons.lang.ArrayUtils;
import org.openl.binding.impl.Operators;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.meta.number.Formulas;
import org.openl.meta.number.LogicalExpressions;
import org.openl.meta.number.NumberOperations;
import org.openl.util.ArrayTool;
import org.openl.util.math.MathUtils;

public class LongValue extends ExplanationNumberValue<LongValue> {
   
    private static final long serialVersionUID = -437788531108803012L;
    
    // <<< INSERT Functions >>>
	// generate zero for types that are wrappers over primitives
	private static final org.openl.meta.LongValue ZERO1 = new org.openl.meta.LongValue((long)0);

	private long value;


	public static boolean eq(org.openl.meta.LongValue value1, org.openl.meta.LongValue value2) {
		validate(value1, value2, LogicalExpressions.EQ.toString());
		
		return Operators.eq(value1.getValue(), value2.getValue());		
	}
	public static boolean ge(org.openl.meta.LongValue value1, org.openl.meta.LongValue value2) {
		validate(value1, value2, LogicalExpressions.GE.toString());
		
		return Operators.ge(value1.getValue(), value2.getValue());		
	}
	public static boolean gt(org.openl.meta.LongValue value1, org.openl.meta.LongValue value2) {
		validate(value1, value2, LogicalExpressions.GT.toString());
		
		return Operators.gt(value1.getValue(), value2.getValue());		
	}
	public static boolean le(org.openl.meta.LongValue value1, org.openl.meta.LongValue value2) {
		validate(value1, value2, LogicalExpressions.LE.toString());
		
		return Operators.le(value1.getValue(), value2.getValue());		
	}
	public static boolean lt(org.openl.meta.LongValue value1, org.openl.meta.LongValue value2) {
		validate(value1, value2, LogicalExpressions.LT.toString());
		
		return Operators.lt(value1.getValue(), value2.getValue());		
	}
	public static boolean ne(org.openl.meta.LongValue value1, org.openl.meta.LongValue value2) {
		validate(value1, value2, LogicalExpressions.NE.toString());
		
		return Operators.ne(value1.getValue(), value2.getValue());		
	}

	public static org.openl.meta.LongValue avg(org.openl.meta.LongValue[] values) {
		if (ArrayUtils.isEmpty(values)) {
            return null;
        }
		long[] primitiveArray = unwrap(values);
		long avg = MathUtils.avg(primitiveArray);
		return new org.openl.meta.LongValue(new org.openl.meta.LongValue(avg), NumberOperations.AVG, values);
	}
	public static org.openl.meta.LongValue sum(org.openl.meta.LongValue[] values) {
		if (ArrayUtils.isEmpty(values)) {
            return null;
        }
		long[] primitiveArray = unwrap(values);
		long sum = MathUtils.sum(primitiveArray);
		return new org.openl.meta.LongValue(new org.openl.meta.LongValue(sum), NumberOperations.SUM, values);
	}
	public static org.openl.meta.LongValue median(org.openl.meta.LongValue[] values) {
		if (ArrayUtils.isEmpty(values)) {
            return null;
        }
		long[] primitiveArray = unwrap(values);
		long median = MathUtils.median(primitiveArray);
		return new org.openl.meta.LongValue(new org.openl.meta.LongValue(median), NumberOperations.MEDIAN, values);
	}

	public static org.openl.meta.LongValue max(org.openl.meta.LongValue value1, org.openl.meta.LongValue value2) {
		validate(value1, value2, NumberOperations.MAX.toString());
		
		return new org.openl.meta.LongValue(MathUtils.max(value1.getValue(), value2.getValue()) ? value1 : value2,
            NumberOperations.MAX,
            new org.openl.meta.LongValue[] { value1, value2 });
	}
	public static org.openl.meta.LongValue min(org.openl.meta.LongValue value1, org.openl.meta.LongValue value2) {
		validate(value1, value2, NumberOperations.MIN.toString());
		
		return new org.openl.meta.LongValue(MathUtils.min(value1.getValue(), value2.getValue()) ? value1 : value2,
            NumberOperations.MIN,
            new org.openl.meta.LongValue[] { value1, value2 });
	}

	public static org.openl.meta.LongValue max(org.openl.meta.LongValue[] values) {
		org.openl.meta.LongValue result = (org.openl.meta.LongValue) MathUtils.max(values); 		
		
		return new org.openl.meta.LongValue((org.openl.meta.LongValue) getAppropriateValue(values, result), 
            NumberOperations.MAX_IN_ARRAY, values);
	}
	public static org.openl.meta.LongValue min(org.openl.meta.LongValue[] values) {
		org.openl.meta.LongValue result = (org.openl.meta.LongValue) MathUtils.min(values); 		
		
		return new org.openl.meta.LongValue((org.openl.meta.LongValue) getAppropriateValue(values, result), 
            NumberOperations.MIN_IN_ARRAY, values);
	}

	public static org.openl.meta.LongValue copy(org.openl.meta.LongValue value, String name) {
		if (value.getName() == null) {
            value.setName(name);

            return value;
        } else if (!value.getName().equals(name)) {
        	org.openl.meta.LongValue result = new org.openl.meta.LongValue (value, NumberOperations.COPY, 
        		new org.openl.meta.LongValue[] { value });
        	result.setName(name);

            return result;
        }
        return value;
	}
	
	//REM
	public static org.openl.meta.LongValue rem(org.openl.meta.LongValue value1, org.openl.meta.LongValue value2) {
		validate(value1, value2, Formulas.REM.toString());
		
		return new org.openl.meta.LongValue(value1, value2, Operators.rem(value1.getValue(), value2.getValue()), 
			Formulas.REM);		
	}
	 	
	
	//ADD
	public static org.openl.meta.LongValue add(org.openl.meta.LongValue value1, org.openl.meta.LongValue value2) {
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
        
		return new org.openl.meta.LongValue(value1, value2, Operators.add(value1.getValue(), value2.getValue()), 
			Formulas.ADD);	
	}
	
	// MULTIPLY
	public static org.openl.meta.LongValue multiply(org.openl.meta.LongValue value1, org.openl.meta.LongValue value2) {
		// temporary commented to support operations with nulls
		//
		//		validate(value1, value2, Formulas.MULTIPLY.toString());
		if (value1 == null) {
			return ZERO1;
		}
		
		if (value2 == null) {
			return ZERO1;
		}
		
		return new org.openl.meta.LongValue(value1, value2, Operators.multiply(value1.getValue(), value2.getValue()), 
			Formulas.MULTIPLY);		
	}
	
	//SUBTRACT
	public static org.openl.meta.LongValue subtract(org.openl.meta.LongValue value1, org.openl.meta.LongValue value2) {
		// temporary commented to support operations with nulls
		//
		//		validate(value1, value2, Formulas.SUBTRACT.toString());
		
		if (value1 == null) {
			return negative(value2);
		}
		
		if (value2 == null) {
			return value1;
		}
		
		return new org.openl.meta.LongValue(value1, value2, Operators.subtract(value1.getValue(), value2.getValue()), 
			Formulas.SUBTRACT);		
	}
	
	public static org.openl.meta.LongValue divide(org.openl.meta.LongValue value1, org.openl.meta.LongValue value2) {
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
		
		return new org.openl.meta.LongValue(value1, value2, Operators.divide(value1.getValue(), value2.getValue()), 
			Formulas.DIVIDE);		
	}
	
	
	// QUAOTIENT
	public static LongValue quotient(org.openl.meta.LongValue number, org.openl.meta.LongValue divisor) {
        if (number != null && divisor != null) {
            LongValue result = new LongValue(MathUtils.quotient(number.getValue(), divisor.getValue()));
            return new LongValue(result, NumberOperations.QUOTIENT, null);
        }
        return null;
    }
	
	// generated product function for types that are wrappers over primitives
	public static DoubleValue product(org.openl.meta.LongValue[] values) {
		if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        long[] primitiveArray = unwrap(values);
        double product = MathUtils.product(primitiveArray);
        // we loose the parameters, but not the result of computation.
        return new DoubleValue(new DoubleValue(product), NumberOperations.PRODUCT, null);
	}
	
	public static org.openl.meta.LongValue mod(org.openl.meta.LongValue number, org.openl.meta.LongValue divisor) {
        if (number != null && divisor != null) {
            org.openl.meta.LongValue result = new org.openl.meta.LongValue(MathUtils.mod(number.getValue(), divisor.getValue()));
            return new org.openl.meta.LongValue(result, NumberOperations.MOD, new org.openl.meta.LongValue[]{number, divisor} );
        }
        return null;
    }
    
    public static org.openl.meta.LongValue small(org.openl.meta.LongValue[] values, int position) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        long[] primitiveArray = unwrap(values);
        long small = MathUtils.small(primitiveArray, position);
        return new org.openl.meta.LongValue((org.openl.meta.LongValue) getAppropriateValue(values, new org.openl.meta.LongValue(small)), 
            NumberOperations.SMALL, values);
    }
    
    public static org.openl.meta.LongValue big(org.openl.meta.LongValue[] values, int position) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        long[] primitiveArray = unwrap(values);
        long big = MathUtils.big(primitiveArray, position);
        return new org.openl.meta.LongValue((org.openl.meta.LongValue) getAppropriateValue(values, new org.openl.meta.LongValue(big)), 
            NumberOperations.BIG, values);
    }
    
    public static org.openl.meta.LongValue pow(org.openl.meta.LongValue value1, org.openl.meta.LongValue value2) {
        validate(value1, value2, NumberOperations.POW);
        
        return new org.openl.meta.LongValue(new org.openl.meta.LongValue(Operators.pow(value1.getValue(), value2.getValue())), 
            NumberOperations.POW, new org.openl.meta.LongValue[] { value1, value2 });
    }
    
    public static org.openl.meta.LongValue abs(org.openl.meta.LongValue value) {
        validate(value, NumberOperations.ABS);
        // evaluate result
        org.openl.meta.LongValue result = new org.openl.meta.LongValue(Operators.abs(value.getValue()));
        // create instance with information about last operation
        return new org.openl.meta.LongValue(result, NumberOperations.ABS, new org.openl.meta.LongValue[] { value });
    }
    
    public static org.openl.meta.LongValue negative(org.openl.meta.LongValue value) {
        return multiply(value, new org.openl.meta.LongValue("-1"));
    }
    
    public static org.openl.meta.LongValue inc(org.openl.meta.LongValue value) {
        return add(value, new org.openl.meta.LongValue("1"));
    }
    
    public static org.openl.meta.LongValue positive(org.openl.meta.LongValue value) {
        return value;
    }
    
    public static org.openl.meta.LongValue dec(org.openl.meta.LongValue value) {
        return subtract(value, new org.openl.meta.LongValue("1"));
    }
    
    // Autocasts
    
	public static org.openl.meta.LongValue autocast(byte x, org.openl.meta.LongValue y) {
		return new org.openl.meta.LongValue((long) x);
	}		
	public static org.openl.meta.LongValue autocast(short x, org.openl.meta.LongValue y) {
		return new org.openl.meta.LongValue((long) x);
	}		
	public static org.openl.meta.LongValue autocast(int x, org.openl.meta.LongValue y) {
		return new org.openl.meta.LongValue((long) x);
	}		
	public static org.openl.meta.LongValue autocast(long x, org.openl.meta.LongValue y) {
		return new org.openl.meta.LongValue((long) x);
	}		
	public static org.openl.meta.LongValue autocast(float x, org.openl.meta.LongValue y) {
		return new org.openl.meta.LongValue((long) x);
	}		
	public static org.openl.meta.LongValue autocast(double x, org.openl.meta.LongValue y) {
		return new org.openl.meta.LongValue((long) x);
	}		
    
    // Constructors
    public LongValue(long value) {
        this.value = value;
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
    public LongValue(org.openl.meta.LongValue lv1, org.openl.meta.LongValue lv2, long value, Formulas operand) {
        super(lv1, lv2, operand);
        this.value = value;
    }    

    @Override
    public org.openl.meta.LongValue copy(String name) {
        return copy(this, name);        
    }    
    
    public String printValue() {        
        return String.valueOf(value);
    }
    
    public long getValue() {        
        return value;
    }
    
    public void setValue(long value) {
        this.value = value;
    }
	
	
	


	
	 
      
                                                                                            // <<< END INSERT Functions >>>    
    
    // ******* Autocasts*************

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
    
    public static BigIntegerValue autocast(LongValue x, BigIntegerValue y) {
        if (x == null) {
            return null;
        }
        return new BigIntegerValue(String.valueOf(x.getValue()));
    }
    
    public static BigDecimalValue autocast(LongValue x, BigDecimalValue y) {
        if (x == null) {
            return null;
        }
        return new BigDecimalValue(String.valueOf(x.getValue()));
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
        if (x == null) {
            return null;
        }
        return new ByteValue(x.byteValue());
    }
        
    public static ShortValue cast(LongValue x, ShortValue y) {
        if (x == null) {
            return null;
        }
        return new ShortValue(x.shortValue());
    }

    public static IntValue cast(LongValue x, IntValue y) {
        if (x == null) {
            return null;
        }
        return new IntValue(x.intValue());
    }  

    public LongValue(String valueString) {        
        value = Long.parseLong(valueString);
    }   

    /**Function constructor**/
    public LongValue(LongValue result, NumberOperations function, LongValue[] params) {
        super(result, function, params);
        this.value = result.longValue();
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
        return value;
    }  

    public int compareTo(Number o) {
        return value < o.longValue() ? -1 : (value == o.longValue() ? 0 : 1);        
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
    
    private static long[] unwrap(LongValue[] values) {
        if (ArrayTool.noNulls(values)) {
            long[] longArray = new long[values.length];
            for (int i = 0; i < values.length; i++) {
                longArray[i] = values[i].getValue();
            }
            return longArray;
        }
        return ArrayUtils.EMPTY_LONG_ARRAY;
    }

}
