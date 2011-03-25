package org.openl.meta;

import org.apache.commons.lang.ArrayUtils;
import org.openl.binding.impl.Operators;
import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.meta.number.Formulas;
import org.openl.meta.number.LogicalExpressions;
import org.openl.meta.number.NumberOperations;
import org.openl.util.ArrayTool;
import org.openl.util.math.MathUtils;

public class IntValue extends ExplanationNumberValue<IntValue> {
    
    private static final long serialVersionUID = -3821702883606493390L;    
    
    // <<< INSERT Functions >>>
	private int value;

	public static org.openl.meta.IntValue add(org.openl.meta.IntValue value1, org.openl.meta.IntValue value2) {
		validate(value1, value2, Formulas.ADD.toString());
		
		return new org.openl.meta.IntValue(value1, value2, Operators.add(value1.getValue(), value2.getValue()), 
			Formulas.ADD);		
	}
	public static org.openl.meta.IntValue multiply(org.openl.meta.IntValue value1, org.openl.meta.IntValue value2) {
		validate(value1, value2, Formulas.MULTIPLY.toString());
		
		return new org.openl.meta.IntValue(value1, value2, Operators.multiply(value1.getValue(), value2.getValue()), 
			Formulas.MULTIPLY);		
	}
	public static org.openl.meta.IntValue subtract(org.openl.meta.IntValue value1, org.openl.meta.IntValue value2) {
		validate(value1, value2, Formulas.SUBTRACT.toString());
		
		return new org.openl.meta.IntValue(value1, value2, Operators.subtract(value1.getValue(), value2.getValue()), 
			Formulas.SUBTRACT);		
	}
	public static org.openl.meta.IntValue divide(org.openl.meta.IntValue value1, org.openl.meta.IntValue value2) {
		validate(value1, value2, Formulas.DIVIDE.toString());
		
		return new org.openl.meta.IntValue(value1, value2, Operators.divide(value1.getValue(), value2.getValue()), 
			Formulas.DIVIDE);		
	}
	public static org.openl.meta.IntValue rem(org.openl.meta.IntValue value1, org.openl.meta.IntValue value2) {
		validate(value1, value2, Formulas.REM.toString());
		
		return new org.openl.meta.IntValue(value1, value2, Operators.rem(value1.getValue(), value2.getValue()), 
			Formulas.REM);		
	}

	public static boolean eq(org.openl.meta.IntValue value1, org.openl.meta.IntValue value2) {
		validate(value1, value2, LogicalExpressions.EQ.toString());
		
		return Operators.eq(value1.getValue(), value2.getValue());		
	}
	public static boolean ge(org.openl.meta.IntValue value1, org.openl.meta.IntValue value2) {
		validate(value1, value2, LogicalExpressions.GE.toString());
		
		return Operators.ge(value1.getValue(), value2.getValue());		
	}
	public static boolean gt(org.openl.meta.IntValue value1, org.openl.meta.IntValue value2) {
		validate(value1, value2, LogicalExpressions.GT.toString());
		
		return Operators.gt(value1.getValue(), value2.getValue());		
	}
	public static boolean le(org.openl.meta.IntValue value1, org.openl.meta.IntValue value2) {
		validate(value1, value2, LogicalExpressions.LE.toString());
		
		return Operators.le(value1.getValue(), value2.getValue());		
	}
	public static boolean lt(org.openl.meta.IntValue value1, org.openl.meta.IntValue value2) {
		validate(value1, value2, LogicalExpressions.LT.toString());
		
		return Operators.lt(value1.getValue(), value2.getValue());		
	}
	public static boolean ne(org.openl.meta.IntValue value1, org.openl.meta.IntValue value2) {
		validate(value1, value2, LogicalExpressions.NE.toString());
		
		return Operators.ne(value1.getValue(), value2.getValue());		
	}

	public static org.openl.meta.IntValue avg(org.openl.meta.IntValue[] values) {
		if (ArrayUtils.isEmpty(values)) {
            return null;
        }
		int[] primitiveArray = unwrap(values);
		int avg = MathUtils.avg(primitiveArray);
		return new org.openl.meta.IntValue(new org.openl.meta.IntValue(avg), NumberOperations.AVG, values);
	}
	public static org.openl.meta.IntValue sum(org.openl.meta.IntValue[] values) {
		if (ArrayUtils.isEmpty(values)) {
            return null;
        }
		int[] primitiveArray = unwrap(values);
		int sum = MathUtils.sum(primitiveArray);
		return new org.openl.meta.IntValue(new org.openl.meta.IntValue(sum), NumberOperations.SUM, values);
	}
	public static org.openl.meta.IntValue median(org.openl.meta.IntValue[] values) {
		if (ArrayUtils.isEmpty(values)) {
            return null;
        }
		int[] primitiveArray = unwrap(values);
		int median = MathUtils.median(primitiveArray);
		return new org.openl.meta.IntValue(new org.openl.meta.IntValue(median), NumberOperations.MEDIAN, values);
	}

	public static org.openl.meta.IntValue max(org.openl.meta.IntValue value1, org.openl.meta.IntValue value2) {
		validate(value1, value2, NumberOperations.MAX.toString());
		
		return new org.openl.meta.IntValue(MathUtils.max(value1.getValue(), value2.getValue()) ? value1 : value2,
            NumberOperations.MAX,
            new org.openl.meta.IntValue[] { value1, value2 });
	}
	public static org.openl.meta.IntValue min(org.openl.meta.IntValue value1, org.openl.meta.IntValue value2) {
		validate(value1, value2, NumberOperations.MIN.toString());
		
		return new org.openl.meta.IntValue(MathUtils.min(value1.getValue(), value2.getValue()) ? value1 : value2,
            NumberOperations.MIN,
            new org.openl.meta.IntValue[] { value1, value2 });
	}

	public static org.openl.meta.IntValue max(org.openl.meta.IntValue[] values) {
		org.openl.meta.IntValue result = (org.openl.meta.IntValue) MathUtils.max(values); 		
		
		return new org.openl.meta.IntValue((org.openl.meta.IntValue) getAppropriateValue(values, result), 
            NumberOperations.MAX_IN_ARRAY, values);
	}
	public static org.openl.meta.IntValue min(org.openl.meta.IntValue[] values) {
		org.openl.meta.IntValue result = (org.openl.meta.IntValue) MathUtils.min(values); 		
		
		return new org.openl.meta.IntValue((org.openl.meta.IntValue) getAppropriateValue(values, result), 
            NumberOperations.MIN_IN_ARRAY, values);
	}

	public static org.openl.meta.IntValue copy(org.openl.meta.IntValue value, String name) {
		if (value.getName() == null) {
            value.setName(name);

            return value;
        } else if (!value.getName().equals(name)) {
        	org.openl.meta.IntValue result = new org.openl.meta.IntValue (value, NumberOperations.COPY, 
        		new org.openl.meta.IntValue[] { value });
        	result.setName(name);

            return result;
        }
        return value;
	}
	
	// generated product function for types that are wrappers over primitives
	public static DoubleValue product(org.openl.meta.IntValue[] values) {
		if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        int[] primitiveArray = unwrap(values);
        double product = MathUtils.product(primitiveArray);
        // we loose the parameters, but not the result of computation.
        return new DoubleValue(new DoubleValue(product), NumberOperations.PRODUCT, null);
	}
	
	public static org.openl.meta.IntValue mod(org.openl.meta.IntValue number, org.openl.meta.IntValue divisor) {
        if (number != null && divisor != null) {
            org.openl.meta.IntValue result = new org.openl.meta.IntValue(MathUtils.mod(number.getValue(), divisor.getValue()));
            return new org.openl.meta.IntValue(result, NumberOperations.MOD, new org.openl.meta.IntValue[]{number, divisor} );
        }
        return null;
    }
    
    public static org.openl.meta.IntValue small(org.openl.meta.IntValue[] values, int position) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        int[] primitiveArray = unwrap(values);
        int small = MathUtils.small(primitiveArray, position);
        return new org.openl.meta.IntValue((org.openl.meta.IntValue) getAppropriateValue(values, new org.openl.meta.IntValue(small)), 
            NumberOperations.SMALL, values);
    }
    
    public static org.openl.meta.IntValue pow(org.openl.meta.IntValue value1, org.openl.meta.IntValue value2) {
        validate(value1, value2, NumberOperations.POW);
        
        return new org.openl.meta.IntValue(new org.openl.meta.IntValue(Operators.pow(value1.getValue(), value2.getValue())), 
            NumberOperations.POW, new org.openl.meta.IntValue[] { value1, value2 });
    }
    
    public static org.openl.meta.IntValue abs(org.openl.meta.IntValue value) {
        validate(value, NumberOperations.ABS);
        // evaluate result
        org.openl.meta.IntValue result = new org.openl.meta.IntValue(Operators.abs(value.getValue()));
        // create instance with information about last operation
        return new org.openl.meta.IntValue(result, NumberOperations.ABS, new org.openl.meta.IntValue[] { value });
    }
    
    public static org.openl.meta.IntValue negative(org.openl.meta.IntValue value) {
        return multiply(value, new org.openl.meta.IntValue("-1"));
    }
    
    public static org.openl.meta.IntValue inc(org.openl.meta.IntValue value) {
        return add(value, new org.openl.meta.IntValue("1"));
    }
    
    public static org.openl.meta.IntValue positive(org.openl.meta.IntValue value) {
        return value;
    }
    
    public static org.openl.meta.IntValue dec(org.openl.meta.IntValue value) {
        return subtract(value, new org.openl.meta.IntValue("1"));
    }
    
    // Autocasts
    
	public static org.openl.meta.IntValue autocast(byte x, org.openl.meta.IntValue y) {
		return new org.openl.meta.IntValue((int) x);
	}		
	public static org.openl.meta.IntValue autocast(short x, org.openl.meta.IntValue y) {
		return new org.openl.meta.IntValue((int) x);
	}		
	public static org.openl.meta.IntValue autocast(int x, org.openl.meta.IntValue y) {
		return new org.openl.meta.IntValue((int) x);
	}		
	public static org.openl.meta.IntValue autocast(long x, org.openl.meta.IntValue y) {
		return new org.openl.meta.IntValue((int) x);
	}		
	public static org.openl.meta.IntValue autocast(float x, org.openl.meta.IntValue y) {
		return new org.openl.meta.IntValue((int) x);
	}		
	public static org.openl.meta.IntValue autocast(double x, org.openl.meta.IntValue y) {
		return new org.openl.meta.IntValue((int) x);
	}		
    
    // Constructors
    public IntValue(int value) {
        this.value = value;
    }    

    public IntValue(int value, String name) {
        super(name);
        this.value = value;
    }

    public IntValue(int value, IMetaInfo metaInfo) {
        super(metaInfo);
        this.value = value;        
    }    

    /**Formula constructor**/
    public IntValue(org.openl.meta.IntValue lv1, org.openl.meta.IntValue lv2, int value, Formulas operand) {
        super(lv1, lv2, operand);
        this.value = value;
    }    

    @Override
    public org.openl.meta.IntValue copy(String name) {
        return copy(this, name);        
    }    
    
    public String printValue() {        
        return String.valueOf(value);
    }
    
    public int getValue() {        
        return value;
    }
    
    public void setValue(int value) {
        this.value = value;
    }
    // <<< END INSERT Functions >>>
    
    // ******* Autocasts*************

    public static IntValue autocast(Integer x, IntValue y) {
        if (x == null) {
            return null;
        }

        return new IntValue(x);
    }
    
    public static LongValue autocast(IntValue x, LongValue y) {
        if (x == null) {
            return null;
        }

        return new LongValue(x.getValue());
    }
    
    public static FloatValue autocast(IntValue x, FloatValue y) {
        if (x == null) {
            return null;
        }

        return new FloatValue(x.getValue());
    }
    
    public static DoubleValue autocast(IntValue x, DoubleValue y) {
        if (x == null) {
            return null;
        }

        return new DoubleValue(x.getValue());
    }
    
    public static BigIntegerValue autocast(IntValue x, BigIntegerValue y) {
        if (x == null) {
            return null;
        }
        return new BigIntegerValue(String.valueOf(x.getValue()));
    }
    
    public static BigDecimalValue autocast(IntValue x, BigDecimalValue y) {
        if (x == null) {
            return null;
        }
        return new BigDecimalValue(String.valueOf(x.getValue()));
    }
    
    // ******* Casts*************

    public static byte cast(IntValue x, byte y) {
        return x.byteValue();
    }

    public static short cast(IntValue x, short y) {
        return x.shortValue();
    }
    
    public static char cast(IntValue x, char y) {
        return (char) x.intValue();
    }

    public static int cast(IntValue x, int y) {
        return x.intValue();
    }

    public static long cast(IntValue x, long y) {
        return x.longValue();
    }

    public static float cast(IntValue x, float y) {
        return x.floatValue();
    }
    
    public static double cast(IntValue x, double y) {
        return x.doubleValue();
    }

    public static Integer cast(IntValue x, Integer y) {
        if (x == null) {
            return null;
        }

        return x.intValue();
    }
    
    public static ByteValue cast(IntValue x, ByteValue y) {
        if (x == null) {
            return null;
        }
        return new ByteValue(x.byteValue());
    }
        
    public static ShortValue cast(IntValue x, ShortValue y) {
        if (x == null) {
            return null;
        }
        return new ShortValue(x.shortValue());
    }        
    
    // Math functions
    
    public static IntValue quaotient(IntValue number, IntValue divisor) {
        if (number != null && divisor != null) {
            IntValue result = new IntValue(MathUtils.quaotient(number.getValue(), divisor.getValue()));
            return new IntValue(result, NumberOperations.QUAOTIENT, new IntValue[]{number, divisor} );
        }
        return null;
    }

    public IntValue(String valueString) {        
        value = Integer.parseInt(valueString);
    }

    /**Function constructor**/
    public IntValue(IntValue result, NumberOperations function, IntValue[] params) {
        super(result, function, params);
        this.value = result.intValue();
    }
    
    @Override
    public double doubleValue() {        
        return (double) value;
    }

    @Override
    public float floatValue() {        
        return (float )value;
    }

    @Override
    public int intValue() {        
        return value;
    }
    
    @Override
    public long longValue() {        
        return (long) value;
    }    

    public int compareTo(Number o) {        
        return value < o.intValue() ? -1 : (value == o.intValue() ? 0 : 1);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IntValue) {
            IntValue secondObj = (IntValue) obj;
            return value == secondObj.intValue();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return ((Integer) value).hashCode();
    }   
    
    private static int[] unwrap(IntValue[] values) {
        if (ArrayTool.noNulls(values)) {
            int[] intArray = new int[values.length];
            for (int i = 0; i < values.length; i++) {
                intArray[i] = values[i].getValue();
            }
            return intArray;
        }
        return ArrayUtils.EMPTY_INT_ARRAY;
    }

}
