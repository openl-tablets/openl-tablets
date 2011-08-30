package org.openl.meta;

import java.math.BigDecimal;

import org.apache.commons.lang.ArrayUtils;
import org.openl.binding.impl.Operators;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.meta.number.Formulas;
import org.openl.meta.number.LogicalExpressions;
import org.openl.meta.number.NumberOperations;
import org.openl.util.ArrayTool;
import org.openl.util.math.MathUtils;

public class BigDecimalValue extends ExplanationNumberValue<BigDecimalValue> {

    private static final long serialVersionUID = 1996508840075924034L;
    
    // <<< INSERT Functions >>>
	// generate zero for big types
	private static final org.openl.meta.BigDecimalValue ZERO1 = new org.openl.meta.BigDecimalValue("0");

	private java.math.BigDecimal value;


	public static boolean eq(org.openl.meta.BigDecimalValue value1, org.openl.meta.BigDecimalValue value2) {
		validate(value1, value2, LogicalExpressions.EQ.toString());
		
		return Operators.eq(value1.getValue(), value2.getValue());		
	}
	public static boolean ge(org.openl.meta.BigDecimalValue value1, org.openl.meta.BigDecimalValue value2) {
		validate(value1, value2, LogicalExpressions.GE.toString());
		
		return Operators.ge(value1.getValue(), value2.getValue());		
	}
	public static boolean gt(org.openl.meta.BigDecimalValue value1, org.openl.meta.BigDecimalValue value2) {
		validate(value1, value2, LogicalExpressions.GT.toString());
		
		return Operators.gt(value1.getValue(), value2.getValue());		
	}
	public static boolean le(org.openl.meta.BigDecimalValue value1, org.openl.meta.BigDecimalValue value2) {
		validate(value1, value2, LogicalExpressions.LE.toString());
		
		return Operators.le(value1.getValue(), value2.getValue());		
	}
	public static boolean lt(org.openl.meta.BigDecimalValue value1, org.openl.meta.BigDecimalValue value2) {
		validate(value1, value2, LogicalExpressions.LT.toString());
		
		return Operators.lt(value1.getValue(), value2.getValue());		
	}
	public static boolean ne(org.openl.meta.BigDecimalValue value1, org.openl.meta.BigDecimalValue value2) {
		validate(value1, value2, LogicalExpressions.NE.toString());
		
		return Operators.ne(value1.getValue(), value2.getValue());		
	}

	public static org.openl.meta.BigDecimalValue avg(org.openl.meta.BigDecimalValue[] values) {
		if (ArrayUtils.isEmpty(values)) {
            return null;
        }
		java.math.BigDecimal[] primitiveArray = unwrap(values);
		java.math.BigDecimal avg = MathUtils.avg(primitiveArray);
		return new org.openl.meta.BigDecimalValue(new org.openl.meta.BigDecimalValue(avg), NumberOperations.AVG, values);
	}
	public static org.openl.meta.BigDecimalValue sum(org.openl.meta.BigDecimalValue[] values) {
		if (ArrayUtils.isEmpty(values)) {
            return null;
        }
		java.math.BigDecimal[] primitiveArray = unwrap(values);
		java.math.BigDecimal sum = MathUtils.sum(primitiveArray);
		return new org.openl.meta.BigDecimalValue(new org.openl.meta.BigDecimalValue(sum), NumberOperations.SUM, values);
	}
	public static org.openl.meta.BigDecimalValue median(org.openl.meta.BigDecimalValue[] values) {
		if (ArrayUtils.isEmpty(values)) {
            return null;
        }
		java.math.BigDecimal[] primitiveArray = unwrap(values);
		java.math.BigDecimal median = MathUtils.median(primitiveArray);
		return new org.openl.meta.BigDecimalValue(new org.openl.meta.BigDecimalValue(median), NumberOperations.MEDIAN, values);
	}

	public static org.openl.meta.BigDecimalValue max(org.openl.meta.BigDecimalValue value1, org.openl.meta.BigDecimalValue value2) {
		validate(value1, value2, NumberOperations.MAX.toString());
		
		return new org.openl.meta.BigDecimalValue(MathUtils.max(value1.getValue(), value2.getValue()) ? value1 : value2,
            NumberOperations.MAX,
            new org.openl.meta.BigDecimalValue[] { value1, value2 });
	}
	public static org.openl.meta.BigDecimalValue min(org.openl.meta.BigDecimalValue value1, org.openl.meta.BigDecimalValue value2) {
		validate(value1, value2, NumberOperations.MIN.toString());
		
		return new org.openl.meta.BigDecimalValue(MathUtils.min(value1.getValue(), value2.getValue()) ? value1 : value2,
            NumberOperations.MIN,
            new org.openl.meta.BigDecimalValue[] { value1, value2 });
	}

	public static org.openl.meta.BigDecimalValue max(org.openl.meta.BigDecimalValue[] values) {
		org.openl.meta.BigDecimalValue result = (org.openl.meta.BigDecimalValue) MathUtils.max(values); 		
		
		return new org.openl.meta.BigDecimalValue((org.openl.meta.BigDecimalValue) getAppropriateValue(values, result), 
            NumberOperations.MAX_IN_ARRAY, values);
	}
	public static org.openl.meta.BigDecimalValue min(org.openl.meta.BigDecimalValue[] values) {
		org.openl.meta.BigDecimalValue result = (org.openl.meta.BigDecimalValue) MathUtils.min(values); 		
		
		return new org.openl.meta.BigDecimalValue((org.openl.meta.BigDecimalValue) getAppropriateValue(values, result), 
            NumberOperations.MIN_IN_ARRAY, values);
	}

	public static org.openl.meta.BigDecimalValue copy(org.openl.meta.BigDecimalValue value, String name) {
		if (value.getName() == null) {
            value.setName(name);

            return value;
        } else if (!value.getName().equals(name)) {
        	org.openl.meta.BigDecimalValue result = new org.openl.meta.BigDecimalValue (value, NumberOperations.COPY, 
        		new org.openl.meta.BigDecimalValue[] { value });
        	result.setName(name);

            return result;
        }
        return value;
	}
	
	//REM
	public static org.openl.meta.BigDecimalValue rem(org.openl.meta.BigDecimalValue value1, org.openl.meta.BigDecimalValue value2) {
		validate(value1, value2, Formulas.REM.toString());
		
		return new org.openl.meta.BigDecimalValue(value1, value2, Operators.rem(value1.getValue(), value2.getValue()), 
			Formulas.REM);		
	}
	 	
	
	//ADD
	public static org.openl.meta.BigDecimalValue add(org.openl.meta.BigDecimalValue value1, org.openl.meta.BigDecimalValue value2) {
		// temporary commented to support operations with nulls
		//
		//		validate(value1, value2, Formulas.ADD.toString());
		//conditions big types
		if (value1 == null || value1.getValue() == java.math.BigDecimal.ZERO) {
            return value2;
        }

        if (value2 == null || value2.getValue() == java.math.BigDecimal.ZERO) {
            return value1;
        }
        
		return new org.openl.meta.BigDecimalValue(value1, value2, Operators.add(value1.getValue(), value2.getValue()), 
			Formulas.ADD);	
	}
	
	// MULTIPLY
	public static org.openl.meta.BigDecimalValue multiply(org.openl.meta.BigDecimalValue value1, org.openl.meta.BigDecimalValue value2) {
		// temporary commented to support operations with nulls
		//
		//		validate(value1, value2, Formulas.MULTIPLY.toString());
		if (value1 == null) {
			return ZERO1;
		}
		
		if (value2 == null) {
			return ZERO1;
		}
		
		return new org.openl.meta.BigDecimalValue(value1, value2, Operators.multiply(value1.getValue(), value2.getValue()), 
			Formulas.MULTIPLY);		
	}
	
	//SUBTRACT
	public static org.openl.meta.BigDecimalValue subtract(org.openl.meta.BigDecimalValue value1, org.openl.meta.BigDecimalValue value2) {
		// temporary commented to support operations with nulls
		//
		//		validate(value1, value2, Formulas.SUBTRACT.toString());
		
		if (value1 == null) {
			return negative(value2);
		}
		
		if (value2 == null) {
			return value1;
		}
		
		return new org.openl.meta.BigDecimalValue(value1, value2, Operators.subtract(value1.getValue(), value2.getValue()), 
			Formulas.SUBTRACT);		
	}
	
	public static org.openl.meta.BigDecimalValue divide(org.openl.meta.BigDecimalValue value1, org.openl.meta.BigDecimalValue value2) {
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
		
		return new org.openl.meta.BigDecimalValue(value1, value2, Operators.divide(value1.getValue(), value2.getValue()), 
			Formulas.DIVIDE);		
	}
	
	
	// QUAOTIENT
	public static LongValue quotient(org.openl.meta.BigDecimalValue number, org.openl.meta.BigDecimalValue divisor) {
        if (number != null && divisor != null) {
            LongValue result = new LongValue(MathUtils.quotient(number.getValue(), divisor.getValue()));
            return new LongValue(result, NumberOperations.QUOTIENT, null);
        }
        return null;
    }
	
	// generated product function for big types
	public static org.openl.meta.BigDecimalValue product(org.openl.meta.BigDecimalValue[] values) {
		if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        java.math.BigDecimal[] primitiveArray = unwrap(values);
        java.math.BigDecimal product = MathUtils.product(primitiveArray);
        // we loose the parameters, but not the result of computation.
        return new org.openl.meta.BigDecimalValue(new org.openl.meta.BigDecimalValue(product), NumberOperations.PRODUCT, null);
	}
	
	public static org.openl.meta.BigDecimalValue mod(org.openl.meta.BigDecimalValue number, org.openl.meta.BigDecimalValue divisor) {
        if (number != null && divisor != null) {
            org.openl.meta.BigDecimalValue result = new org.openl.meta.BigDecimalValue(MathUtils.mod(number.getValue(), divisor.getValue()));
            return new org.openl.meta.BigDecimalValue(result, NumberOperations.MOD, new org.openl.meta.BigDecimalValue[]{number, divisor} );
        }
        return null;
    }
    
    public static org.openl.meta.BigDecimalValue small(org.openl.meta.BigDecimalValue[] values, int position) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        java.math.BigDecimal[] primitiveArray = unwrap(values);
        java.math.BigDecimal small = MathUtils.small(primitiveArray, position);
        return new org.openl.meta.BigDecimalValue((org.openl.meta.BigDecimalValue) getAppropriateValue(values, new org.openl.meta.BigDecimalValue(small)), 
            NumberOperations.SMALL, values);
    }
    
    public static org.openl.meta.BigDecimalValue big(org.openl.meta.BigDecimalValue[] values, int position) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        java.math.BigDecimal[] primitiveArray = unwrap(values);
        java.math.BigDecimal big = MathUtils.big(primitiveArray, position);
        return new org.openl.meta.BigDecimalValue((org.openl.meta.BigDecimalValue) getAppropriateValue(values, new org.openl.meta.BigDecimalValue(big)), 
            NumberOperations.BIG, values);
    }
    
    public static org.openl.meta.BigDecimalValue pow(org.openl.meta.BigDecimalValue value1, org.openl.meta.BigDecimalValue value2) {
        validate(value1, value2, NumberOperations.POW);
        
        return new org.openl.meta.BigDecimalValue(new org.openl.meta.BigDecimalValue(Operators.pow(value1.getValue(), value2.getValue())), 
            NumberOperations.POW, new org.openl.meta.BigDecimalValue[] { value1, value2 });
    }
    
    public static org.openl.meta.BigDecimalValue abs(org.openl.meta.BigDecimalValue value) {
        validate(value, NumberOperations.ABS);
        // evaluate result
        org.openl.meta.BigDecimalValue result = new org.openl.meta.BigDecimalValue(Operators.abs(value.getValue()));
        // create instance with information about last operation
        return new org.openl.meta.BigDecimalValue(result, NumberOperations.ABS, new org.openl.meta.BigDecimalValue[] { value });
    }
    
    public static org.openl.meta.BigDecimalValue negative(org.openl.meta.BigDecimalValue value) {
        return multiply(value, new org.openl.meta.BigDecimalValue("-1"));
    }
    
    public static org.openl.meta.BigDecimalValue inc(org.openl.meta.BigDecimalValue value) {
        return add(value, new org.openl.meta.BigDecimalValue("1"));
    }
    
    public static org.openl.meta.BigDecimalValue positive(org.openl.meta.BigDecimalValue value) {
        return value;
    }
    
    public static org.openl.meta.BigDecimalValue dec(org.openl.meta.BigDecimalValue value) {
        return subtract(value, new org.openl.meta.BigDecimalValue("1"));
    }
    
    // Autocasts
    
	public static org.openl.meta.BigDecimalValue autocast(byte x, org.openl.meta.BigDecimalValue y) {
		return new org.openl.meta.BigDecimalValue(String.valueOf(x));
	}		
	public static org.openl.meta.BigDecimalValue autocast(short x, org.openl.meta.BigDecimalValue y) {
		return new org.openl.meta.BigDecimalValue(String.valueOf(x));
	}		
	public static org.openl.meta.BigDecimalValue autocast(int x, org.openl.meta.BigDecimalValue y) {
		return new org.openl.meta.BigDecimalValue(String.valueOf(x));
	}		
	public static org.openl.meta.BigDecimalValue autocast(long x, org.openl.meta.BigDecimalValue y) {
		return new org.openl.meta.BigDecimalValue(String.valueOf(x));
	}		
	public static org.openl.meta.BigDecimalValue autocast(float x, org.openl.meta.BigDecimalValue y) {
		return new org.openl.meta.BigDecimalValue(String.valueOf(x));
	}		
	public static org.openl.meta.BigDecimalValue autocast(double x, org.openl.meta.BigDecimalValue y) {
		return new org.openl.meta.BigDecimalValue(String.valueOf(x));
	}		
    
    // Constructors
    public BigDecimalValue(java.math.BigDecimal value) {
        this.value = value;
    }    

    public BigDecimalValue(java.math.BigDecimal value, String name) {
        super(name);
        this.value = value;
    }

    public BigDecimalValue(java.math.BigDecimal value, IMetaInfo metaInfo) {
        super(metaInfo);
        this.value = value;        
    }    

    /**Formula constructor**/
    public BigDecimalValue(org.openl.meta.BigDecimalValue lv1, org.openl.meta.BigDecimalValue lv2, java.math.BigDecimal value, Formulas operand) {
        super(lv1, lv2, operand);
        this.value = value;
    }    

    @Override
    public org.openl.meta.BigDecimalValue copy(String name) {
        return copy(this, name);        
    }    
    
    public String printValue() {        
        return String.valueOf(value);
    }
    
    public java.math.BigDecimal getValue() {        
        return value;
    }
    
    public void setValue(java.math.BigDecimal value) {
        this.value = value;
    }
	
	
	


	
	 
      
                                                                                            // <<< END INSERT Functions >>>

    // ******* Autocasts *************

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

    public BigDecimalValue(String valueString) {
        value = new BigDecimal(valueString);
    }
    
    public BigDecimalValue(String value, String name) {
        super(name);
        this.value = new BigDecimal(value);
    }

    public BigDecimalValue(String value, IMetaInfo metaInfo) {
        super(metaInfo);
        this.value = new BigDecimal(value);
    }    

    /** Function constructor **/
    public BigDecimalValue(BigDecimalValue result, NumberOperations function, BigDecimalValue[] params) {
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
        } else if (o instanceof BigDecimalValue) {
            return value.compareTo(((BigDecimalValue) o).getValue());
        } else {
            throw new OpenlNotCheckedException("Can`t compare BigDecimalValue with unknown type.");
        }
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
