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

public class ShortValue extends ExplanationNumberValue<ShortValue> {

    private static final long serialVersionUID = 5259931539737847856L;
    
    // <<< INSERT Functions >>>
	// generate zero for types that are wrappers over primitives
	private static final org.openl.meta.ShortValue ZERO1 = new org.openl.meta.ShortValue((short)0);

	private short value;


	public static boolean eq(org.openl.meta.ShortValue value1, org.openl.meta.ShortValue value2) {
		validate(value1, value2, LogicalExpressions.EQ.toString());
		
		return Operators.eq(value1.getValue(), value2.getValue());		
	}
	public static boolean ge(org.openl.meta.ShortValue value1, org.openl.meta.ShortValue value2) {
		validate(value1, value2, LogicalExpressions.GE.toString());
		
		return Operators.ge(value1.getValue(), value2.getValue());		
	}
	public static boolean gt(org.openl.meta.ShortValue value1, org.openl.meta.ShortValue value2) {
		validate(value1, value2, LogicalExpressions.GT.toString());
		
		return Operators.gt(value1.getValue(), value2.getValue());		
	}
	public static boolean le(org.openl.meta.ShortValue value1, org.openl.meta.ShortValue value2) {
		validate(value1, value2, LogicalExpressions.LE.toString());
		
		return Operators.le(value1.getValue(), value2.getValue());		
	}
	public static boolean lt(org.openl.meta.ShortValue value1, org.openl.meta.ShortValue value2) {
		validate(value1, value2, LogicalExpressions.LT.toString());
		
		return Operators.lt(value1.getValue(), value2.getValue());		
	}
	public static boolean ne(org.openl.meta.ShortValue value1, org.openl.meta.ShortValue value2) {
		validate(value1, value2, LogicalExpressions.NE.toString());
		
		return Operators.ne(value1.getValue(), value2.getValue());		
	}

	public static org.openl.meta.ShortValue avg(org.openl.meta.ShortValue[] values) {
		if (ArrayUtils.isEmpty(values)) {
            return null;
        }
		short[] primitiveArray = unwrap(values);
		short avg = MathUtils.avg(primitiveArray);
		return new org.openl.meta.ShortValue(new org.openl.meta.ShortValue(avg), NumberOperations.AVG, values);
	}
	public static org.openl.meta.ShortValue sum(org.openl.meta.ShortValue[] values) {
		if (ArrayUtils.isEmpty(values)) {
            return null;
        }
		short[] primitiveArray = unwrap(values);
		short sum = MathUtils.sum(primitiveArray);
		return new org.openl.meta.ShortValue(new org.openl.meta.ShortValue(sum), NumberOperations.SUM, values);
	}
	public static org.openl.meta.ShortValue median(org.openl.meta.ShortValue[] values) {
		if (ArrayUtils.isEmpty(values)) {
            return null;
        }
		short[] primitiveArray = unwrap(values);
		short median = MathUtils.median(primitiveArray);
		return new org.openl.meta.ShortValue(new org.openl.meta.ShortValue(median), NumberOperations.MEDIAN, values);
	}

	public static org.openl.meta.ShortValue max(org.openl.meta.ShortValue value1, org.openl.meta.ShortValue value2) {
		validate(value1, value2, NumberOperations.MAX.toString());
		
		return new org.openl.meta.ShortValue(MathUtils.max(value1.getValue(), value2.getValue()) ? value1 : value2,
            NumberOperations.MAX,
            new org.openl.meta.ShortValue[] { value1, value2 });
	}
	public static org.openl.meta.ShortValue min(org.openl.meta.ShortValue value1, org.openl.meta.ShortValue value2) {
		validate(value1, value2, NumberOperations.MIN.toString());
		
		return new org.openl.meta.ShortValue(MathUtils.min(value1.getValue(), value2.getValue()) ? value1 : value2,
            NumberOperations.MIN,
            new org.openl.meta.ShortValue[] { value1, value2 });
	}

	public static org.openl.meta.ShortValue max(org.openl.meta.ShortValue[] values) {
		org.openl.meta.ShortValue result = (org.openl.meta.ShortValue) MathUtils.max(values); 		
		
		return new org.openl.meta.ShortValue((org.openl.meta.ShortValue) getAppropriateValue(values, result), 
            NumberOperations.MAX_IN_ARRAY, values);
	}
	public static org.openl.meta.ShortValue min(org.openl.meta.ShortValue[] values) {
		org.openl.meta.ShortValue result = (org.openl.meta.ShortValue) MathUtils.min(values); 		
		
		return new org.openl.meta.ShortValue((org.openl.meta.ShortValue) getAppropriateValue(values, result), 
            NumberOperations.MIN_IN_ARRAY, values);
	}

	public static org.openl.meta.ShortValue copy(org.openl.meta.ShortValue value, String name) {
		if (value.getName() == null) {
            value.setName(name);

            return value;
        } else if (!value.getName().equals(name)) {
        	org.openl.meta.ShortValue result = new org.openl.meta.ShortValue (value, NumberOperations.COPY, 
        		new org.openl.meta.ShortValue[] { value });
        	result.setName(name);

            return result;
        }
        return value;
	}
	
	//REM
	public static org.openl.meta.ShortValue rem(org.openl.meta.ShortValue value1, org.openl.meta.ShortValue value2) {
		validate(value1, value2, Formulas.REM.toString());
		
		return new org.openl.meta.ShortValue(value1, value2, Operators.rem(value1.getValue(), value2.getValue()), 
			Formulas.REM);		
	}
	 	
	
	//ADD
	public static org.openl.meta.ShortValue add(org.openl.meta.ShortValue value1, org.openl.meta.ShortValue value2) {
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
        
		return new org.openl.meta.ShortValue(value1, value2, Operators.add(value1.getValue(), value2.getValue()), 
			Formulas.ADD);	
	}
	
	// MULTIPLY
	public static org.openl.meta.ShortValue multiply(org.openl.meta.ShortValue value1, org.openl.meta.ShortValue value2) {
		// temporary commented to support operations with nulls
		//
		//		validate(value1, value2, Formulas.MULTIPLY.toString());
		if (value1 == null) {
			return ZERO1;
		}
		
		if (value2 == null) {
			return ZERO1;
		}
		
		return new org.openl.meta.ShortValue(value1, value2, Operators.multiply(value1.getValue(), value2.getValue()), 
			Formulas.MULTIPLY);		
	}
	
	//SUBTRACT
	public static org.openl.meta.ShortValue subtract(org.openl.meta.ShortValue value1, org.openl.meta.ShortValue value2) {
		// temporary commented to support operations with nulls
		//
		//		validate(value1, value2, Formulas.SUBTRACT.toString());
		
		if (value1 == null) {
			return negative(value2);
		}
		
		if (value2 == null) {
			return value1;
		}
		
		return new org.openl.meta.ShortValue(value1, value2, Operators.subtract(value1.getValue(), value2.getValue()), 
			Formulas.SUBTRACT);		
	}
	
	public static org.openl.meta.ShortValue divide(org.openl.meta.ShortValue value1, org.openl.meta.ShortValue value2) {
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
		
		return new org.openl.meta.ShortValue(value1, value2, Operators.divide(value1.getValue(), value2.getValue()), 
			Formulas.DIVIDE);		
	}
	
	
	// QUAOTIENT
	public static LongValue quotient(org.openl.meta.ShortValue number, org.openl.meta.ShortValue divisor) {
        if (number != null && divisor != null) {
            LongValue result = new LongValue(MathUtils.quotient(number.getValue(), divisor.getValue()));
            return new LongValue(result, NumberOperations.QUOTIENT, null);
        }
        return null;
    }
	
	// generated product function for types that are wrappers over primitives
	public static DoubleValue product(org.openl.meta.ShortValue[] values) {
		if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        short[] primitiveArray = unwrap(values);
        double product = MathUtils.product(primitiveArray);
        // we loose the parameters, but not the result of computation.
        return new DoubleValue(new DoubleValue(product), NumberOperations.PRODUCT, null);
	}
	
	public static org.openl.meta.ShortValue mod(org.openl.meta.ShortValue number, org.openl.meta.ShortValue divisor) {
        if (number != null && divisor != null) {
            org.openl.meta.ShortValue result = new org.openl.meta.ShortValue(MathUtils.mod(number.getValue(), divisor.getValue()));
            return new org.openl.meta.ShortValue(result, NumberOperations.MOD, new org.openl.meta.ShortValue[]{number, divisor} );
        }
        return null;
    }
    
    public static org.openl.meta.ShortValue small(org.openl.meta.ShortValue[] values, int position) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        short[] primitiveArray = unwrap(values);
        short small = MathUtils.small(primitiveArray, position);
        return new org.openl.meta.ShortValue((org.openl.meta.ShortValue) getAppropriateValue(values, new org.openl.meta.ShortValue(small)), 
            NumberOperations.SMALL, values);
    }
    
    public static org.openl.meta.ShortValue big(org.openl.meta.ShortValue[] values, int position) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        short[] primitiveArray = unwrap(values);
        short big = MathUtils.big(primitiveArray, position);
        return new org.openl.meta.ShortValue((org.openl.meta.ShortValue) getAppropriateValue(values, new org.openl.meta.ShortValue(big)), 
            NumberOperations.BIG, values);
    }
    
    public static org.openl.meta.ShortValue pow(org.openl.meta.ShortValue value1, org.openl.meta.ShortValue value2) {
        validate(value1, value2, NumberOperations.POW);
        
        return new org.openl.meta.ShortValue(new org.openl.meta.ShortValue(Operators.pow(value1.getValue(), value2.getValue())), 
            NumberOperations.POW, new org.openl.meta.ShortValue[] { value1, value2 });
    }
    
    public static org.openl.meta.ShortValue abs(org.openl.meta.ShortValue value) {
        validate(value, NumberOperations.ABS);
        // evaluate result
        org.openl.meta.ShortValue result = new org.openl.meta.ShortValue(Operators.abs(value.getValue()));
        // create instance with information about last operation
        return new org.openl.meta.ShortValue(result, NumberOperations.ABS, new org.openl.meta.ShortValue[] { value });
    }
    
    public static org.openl.meta.ShortValue negative(org.openl.meta.ShortValue value) {
        return multiply(value, new org.openl.meta.ShortValue("-1"));
    }
    
    public static org.openl.meta.ShortValue inc(org.openl.meta.ShortValue value) {
        return add(value, new org.openl.meta.ShortValue("1"));
    }
    
    public static org.openl.meta.ShortValue positive(org.openl.meta.ShortValue value) {
        return value;
    }
    
    public static org.openl.meta.ShortValue dec(org.openl.meta.ShortValue value) {
        return subtract(value, new org.openl.meta.ShortValue("1"));
    }
    
    // Autocasts
    
	public static org.openl.meta.ShortValue autocast(byte x, org.openl.meta.ShortValue y) {
		return new org.openl.meta.ShortValue((short) x);
	}		
	public static org.openl.meta.ShortValue autocast(short x, org.openl.meta.ShortValue y) {
		return new org.openl.meta.ShortValue((short) x);
	}		
	public static org.openl.meta.ShortValue autocast(int x, org.openl.meta.ShortValue y) {
		return new org.openl.meta.ShortValue((short) x);
	}		
	public static org.openl.meta.ShortValue autocast(long x, org.openl.meta.ShortValue y) {
		return new org.openl.meta.ShortValue((short) x);
	}		
	public static org.openl.meta.ShortValue autocast(float x, org.openl.meta.ShortValue y) {
		return new org.openl.meta.ShortValue((short) x);
	}		
	public static org.openl.meta.ShortValue autocast(double x, org.openl.meta.ShortValue y) {
		return new org.openl.meta.ShortValue((short) x);
	}		
    
    // Constructors
    public ShortValue(short value) {
        this.value = value;
    }    

    public ShortValue(short value, String name) {
        super(name);
        this.value = value;
    }

    public ShortValue(short value, IMetaInfo metaInfo) {
        super(metaInfo);
        this.value = value;        
    }    

    /**Formula constructor**/
    public ShortValue(org.openl.meta.ShortValue lv1, org.openl.meta.ShortValue lv2, short value, Formulas operand) {
        super(lv1, lv2, operand);
        this.value = value;
    }    

    @Override
    public org.openl.meta.ShortValue copy(String name) {
        return copy(this, name);        
    }    
    
    public String printValue() {        
        return String.valueOf(value);
    }
    
    public short getValue() {        
        return value;
    }
    
    public void setValue(short value) {
        this.value = value;
    }
	
	
	


	
	 
      
                                                                                            // <<< END INSERT Functions >>>
    
    // ******* Autocasts*************    

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

    public ShortValue(String valueString) {
        value = Short.parseShort(valueString);
    }    

    /** Function constructor **/
    public ShortValue(ShortValue result, NumberOperations function, ShortValue[] params) {
        super(result, function, params);
        this.value = result.shortValue();
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
        return value - o.shortValue();
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
    
    private static short[] unwrap(ShortValue[] values) {
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
