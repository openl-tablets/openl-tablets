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


public class DoubleValue extends ExplanationNumberValue<DoubleValue> {
    
    private static final long serialVersionUID = -4594250562069599646L;        

    public static class DoubleValueOne extends DoubleValue {

        private static final long serialVersionUID = 6347462002516785250L;

        @Override
        public double getValue() {
            return 1;
        }

        public DoubleValue multiply(DoubleValue dv) {
            return dv;
        }
    }

    public static class DoubleValueZero extends DoubleValue {

        private static final long serialVersionUID = 3329865368482848868L;

        public DoubleValue add(DoubleValue dv) {
            return dv;
        }

        public DoubleValue divide(DoubleValue dv) {
            return this;
        }

        @Override
        public double getValue() {
            return 0;
        }

        public DoubleValue multiply(DoubleValue dv) {
            return this;
        }
    }    

    public static final DoubleValue ZERO = new DoubleValueZero();
    public static final DoubleValue ONE = new DoubleValueOne();
    
    // <<< INSERT Functions >>>
	// generate zero for types that are wrappers over primitives
	private static final org.openl.meta.DoubleValue ZERO1 = new org.openl.meta.DoubleValue((double)0);

	private double value;


	public static boolean eq(org.openl.meta.DoubleValue value1, org.openl.meta.DoubleValue value2) {
		validate(value1, value2, LogicalExpressions.EQ.toString());
		
		return Operators.eq(value1.getValue(), value2.getValue());		
	}
	public static boolean ge(org.openl.meta.DoubleValue value1, org.openl.meta.DoubleValue value2) {
		validate(value1, value2, LogicalExpressions.GE.toString());
		
		return Operators.ge(value1.getValue(), value2.getValue());		
	}
	public static boolean gt(org.openl.meta.DoubleValue value1, org.openl.meta.DoubleValue value2) {
		validate(value1, value2, LogicalExpressions.GT.toString());
		
		return Operators.gt(value1.getValue(), value2.getValue());		
	}
	public static boolean le(org.openl.meta.DoubleValue value1, org.openl.meta.DoubleValue value2) {
		validate(value1, value2, LogicalExpressions.LE.toString());
		
		return Operators.le(value1.getValue(), value2.getValue());		
	}
	public static boolean lt(org.openl.meta.DoubleValue value1, org.openl.meta.DoubleValue value2) {
		validate(value1, value2, LogicalExpressions.LT.toString());
		
		return Operators.lt(value1.getValue(), value2.getValue());		
	}
	public static boolean ne(org.openl.meta.DoubleValue value1, org.openl.meta.DoubleValue value2) {
		validate(value1, value2, LogicalExpressions.NE.toString());
		
		return Operators.ne(value1.getValue(), value2.getValue());		
	}

	public static org.openl.meta.DoubleValue avg(org.openl.meta.DoubleValue[] values) {
		if (ArrayUtils.isEmpty(values)) {
            return null;
        }
		double[] primitiveArray = unwrap(values);
		double avg = MathUtils.avg(primitiveArray);
		return new org.openl.meta.DoubleValue(new org.openl.meta.DoubleValue(avg), NumberOperations.AVG, values);
	}
	public static org.openl.meta.DoubleValue sum(org.openl.meta.DoubleValue[] values) {
		if (ArrayUtils.isEmpty(values)) {
            return null;
        }
		double[] primitiveArray = unwrap(values);
		double sum = MathUtils.sum(primitiveArray);
		return new org.openl.meta.DoubleValue(new org.openl.meta.DoubleValue(sum), NumberOperations.SUM, values);
	}
	public static org.openl.meta.DoubleValue median(org.openl.meta.DoubleValue[] values) {
		if (ArrayUtils.isEmpty(values)) {
            return null;
        }
		double[] primitiveArray = unwrap(values);
		double median = MathUtils.median(primitiveArray);
		return new org.openl.meta.DoubleValue(new org.openl.meta.DoubleValue(median), NumberOperations.MEDIAN, values);
	}

	public static org.openl.meta.DoubleValue max(org.openl.meta.DoubleValue value1, org.openl.meta.DoubleValue value2) {
		validate(value1, value2, NumberOperations.MAX.toString());
		
		return new org.openl.meta.DoubleValue(MathUtils.max(value1.getValue(), value2.getValue()) ? value1 : value2,
            NumberOperations.MAX,
            new org.openl.meta.DoubleValue[] { value1, value2 });
	}
	public static org.openl.meta.DoubleValue min(org.openl.meta.DoubleValue value1, org.openl.meta.DoubleValue value2) {
		validate(value1, value2, NumberOperations.MIN.toString());
		
		return new org.openl.meta.DoubleValue(MathUtils.min(value1.getValue(), value2.getValue()) ? value1 : value2,
            NumberOperations.MIN,
            new org.openl.meta.DoubleValue[] { value1, value2 });
	}

	public static org.openl.meta.DoubleValue max(org.openl.meta.DoubleValue[] values) {
		org.openl.meta.DoubleValue result = (org.openl.meta.DoubleValue) MathUtils.max(values); 		
		
		return new org.openl.meta.DoubleValue((org.openl.meta.DoubleValue) getAppropriateValue(values, result), 
            NumberOperations.MAX_IN_ARRAY, values);
	}
	public static org.openl.meta.DoubleValue min(org.openl.meta.DoubleValue[] values) {
		org.openl.meta.DoubleValue result = (org.openl.meta.DoubleValue) MathUtils.min(values); 		
		
		return new org.openl.meta.DoubleValue((org.openl.meta.DoubleValue) getAppropriateValue(values, result), 
            NumberOperations.MIN_IN_ARRAY, values);
	}

	public static org.openl.meta.DoubleValue copy(org.openl.meta.DoubleValue value, String name) {
		if (value.getName() == null) {
            value.setName(name);

            return value;
        } else if (!value.getName().equals(name)) {
        	org.openl.meta.DoubleValue result = new org.openl.meta.DoubleValue (value, NumberOperations.COPY, 
        		new org.openl.meta.DoubleValue[] { value });
        	result.setName(name);

            return result;
        }
        return value;
	}
	
	//REM
	public static org.openl.meta.DoubleValue rem(org.openl.meta.DoubleValue value1, org.openl.meta.DoubleValue value2) {
		validate(value1, value2, Formulas.REM.toString());
		
		return new org.openl.meta.DoubleValue(value1, value2, Operators.rem(value1.getValue(), value2.getValue()), 
			Formulas.REM);		
	}
	 	
	
	//ADD
	public static org.openl.meta.DoubleValue add(org.openl.meta.DoubleValue value1, org.openl.meta.DoubleValue value2) {
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
        
		return new org.openl.meta.DoubleValue(value1, value2, Operators.add(value1.getValue(), value2.getValue()), 
			Formulas.ADD);	
	}
	
	// MULTIPLY
	public static org.openl.meta.DoubleValue multiply(org.openl.meta.DoubleValue value1, org.openl.meta.DoubleValue value2) {
		// temporary commented to support operations with nulls
		//
		//		validate(value1, value2, Formulas.MULTIPLY.toString());
		if (value1 == null) {
			return ZERO1;
		}
		
		if (value2 == null) {
			return ZERO1;
		}
		
		return new org.openl.meta.DoubleValue(value1, value2, Operators.multiply(value1.getValue(), value2.getValue()), 
			Formulas.MULTIPLY);		
	}
	
	//SUBTRACT
	public static org.openl.meta.DoubleValue subtract(org.openl.meta.DoubleValue value1, org.openl.meta.DoubleValue value2) {
		// temporary commented to support operations with nulls
		//
		//		validate(value1, value2, Formulas.SUBTRACT.toString());
		
		if (value1 == null) {
			return negative(value2);
		}
		
		if (value2 == null) {
			return value1;
		}
		
		return new org.openl.meta.DoubleValue(value1, value2, Operators.subtract(value1.getValue(), value2.getValue()), 
			Formulas.SUBTRACT);		
	}
	
	public static org.openl.meta.DoubleValue divide(org.openl.meta.DoubleValue value1, org.openl.meta.DoubleValue value2) {
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
		
		return new org.openl.meta.DoubleValue(value1, value2, Operators.divide(value1.getValue(), value2.getValue()), 
			Formulas.DIVIDE);		
	}
	
	
	// QUAOTIENT
	public static LongValue quotient(org.openl.meta.DoubleValue number, org.openl.meta.DoubleValue divisor) {
        if (number != null && divisor != null) {
            LongValue result = new LongValue(MathUtils.quotient(number.getValue(), divisor.getValue()));
            return new LongValue(result, NumberOperations.QUOTIENT, null);
        }
        return null;
    }
	
	// generated product function for types that are wrappers over primitives
	public static DoubleValue product(org.openl.meta.DoubleValue[] values) {
		if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        double[] primitiveArray = unwrap(values);
        double product = MathUtils.product(primitiveArray);
        // we loose the parameters, but not the result of computation.
        return new DoubleValue(new DoubleValue(product), NumberOperations.PRODUCT, null);
	}
	
	public static org.openl.meta.DoubleValue mod(org.openl.meta.DoubleValue number, org.openl.meta.DoubleValue divisor) {
        if (number != null && divisor != null) {
            org.openl.meta.DoubleValue result = new org.openl.meta.DoubleValue(MathUtils.mod(number.getValue(), divisor.getValue()));
            return new org.openl.meta.DoubleValue(result, NumberOperations.MOD, new org.openl.meta.DoubleValue[]{number, divisor} );
        }
        return null;
    }
    
    public static org.openl.meta.DoubleValue small(org.openl.meta.DoubleValue[] values, int position) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        double[] primitiveArray = unwrap(values);
        double small = MathUtils.small(primitiveArray, position);
        return new org.openl.meta.DoubleValue((org.openl.meta.DoubleValue) getAppropriateValue(values, new org.openl.meta.DoubleValue(small)), 
            NumberOperations.SMALL, values);
    }
    
    public static org.openl.meta.DoubleValue big(org.openl.meta.DoubleValue[] values, int position) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        double[] primitiveArray = unwrap(values);
        double big = MathUtils.big(primitiveArray, position);
        return new org.openl.meta.DoubleValue((org.openl.meta.DoubleValue) getAppropriateValue(values, new org.openl.meta.DoubleValue(big)), 
            NumberOperations.BIG, values);
    }
    
    public static org.openl.meta.DoubleValue pow(org.openl.meta.DoubleValue value1, org.openl.meta.DoubleValue value2) {
        validate(value1, value2, NumberOperations.POW);
        
        return new org.openl.meta.DoubleValue(new org.openl.meta.DoubleValue(Operators.pow(value1.getValue(), value2.getValue())), 
            NumberOperations.POW, new org.openl.meta.DoubleValue[] { value1, value2 });
    }
    
    public static org.openl.meta.DoubleValue abs(org.openl.meta.DoubleValue value) {
        validate(value, NumberOperations.ABS);
        // evaluate result
        org.openl.meta.DoubleValue result = new org.openl.meta.DoubleValue(Operators.abs(value.getValue()));
        // create instance with information about last operation
        return new org.openl.meta.DoubleValue(result, NumberOperations.ABS, new org.openl.meta.DoubleValue[] { value });
    }
    
    public static org.openl.meta.DoubleValue negative(org.openl.meta.DoubleValue value) {
        return multiply(value, new org.openl.meta.DoubleValue("-1"));
    }
    
    public static org.openl.meta.DoubleValue inc(org.openl.meta.DoubleValue value) {
        return add(value, new org.openl.meta.DoubleValue("1"));
    }
    
    public static org.openl.meta.DoubleValue positive(org.openl.meta.DoubleValue value) {
        return value;
    }
    
    public static org.openl.meta.DoubleValue dec(org.openl.meta.DoubleValue value) {
        return subtract(value, new org.openl.meta.DoubleValue("1"));
    }
    
    // Autocasts
    
	public static org.openl.meta.DoubleValue autocast(byte x, org.openl.meta.DoubleValue y) {
		return new org.openl.meta.DoubleValue((double) x);
	}		
	public static org.openl.meta.DoubleValue autocast(short x, org.openl.meta.DoubleValue y) {
		return new org.openl.meta.DoubleValue((double) x);
	}		
	public static org.openl.meta.DoubleValue autocast(int x, org.openl.meta.DoubleValue y) {
		return new org.openl.meta.DoubleValue((double) x);
	}		
	public static org.openl.meta.DoubleValue autocast(long x, org.openl.meta.DoubleValue y) {
		return new org.openl.meta.DoubleValue((double) x);
	}		
	public static org.openl.meta.DoubleValue autocast(float x, org.openl.meta.DoubleValue y) {
		return new org.openl.meta.DoubleValue((double) x);
	}		
	public static org.openl.meta.DoubleValue autocast(double x, org.openl.meta.DoubleValue y) {
		return new org.openl.meta.DoubleValue((double) x);
	}		
    
    // Constructors
    public DoubleValue(double value) {
        this.value = value;
    }    

    public DoubleValue(double value, String name) {
        super(name);
        this.value = value;
    }

    public DoubleValue(double value, IMetaInfo metaInfo) {
        super(metaInfo);
        this.value = value;        
    }    

    /**Formula constructor**/
    public DoubleValue(org.openl.meta.DoubleValue lv1, org.openl.meta.DoubleValue lv2, double value, Formulas operand) {
        super(lv1, lv2, operand);
        this.value = value;
    }    

    @Override
    public org.openl.meta.DoubleValue copy(String name) {
        return copy(this, name);        
    }    
    
    public String printValue() {        
        return String.valueOf(value);
    }
    
    public double getValue() {        
        return value;
    }
    
    public void setValue(double value) {
        this.value = value;
    }
	
	
	


	
	 
      
                                                    // <<< END INSERT Functions >>>    
    
    // ******* Autocasts *************

    public static DoubleValue autocast(Double x, DoubleValue y) {
        if (x == null) {
            return null;
        }

        return new DoubleValue(x);
    }
    
    public static BigDecimalValue autocast(DoubleValue x, BigDecimalValue y) {
        if (x == null) {
            return null;
        }
        return new BigDecimalValue(String.valueOf(x.getValue()));
    }
    
    // ******* Casts *************

    public static byte cast(DoubleValue x, byte y) {
        return x.byteValue();
    }

    public static short cast(DoubleValue x, short y) {
        return x.shortValue();
    }
    
    public static char cast(DoubleValue x, char y) {
        return (char) x.doubleValue();
    }

    public static int cast(DoubleValue x, int y) {
        return x.intValue();
    }

    public static long cast(DoubleValue x, long y) {
        return x.longValue();
    }

    public static float cast(DoubleValue x, float y) {
        return x.floatValue();
    }
    
    public static double cast(DoubleValue x, double y) {
        return x.doubleValue();
    }

    public static Double cast(DoubleValue x, Double y) {
        if (x == null) {
            return null;
        }

        return x.doubleValue();
    }
    
    public static ByteValue cast(DoubleValue x, ByteValue y) {
        if (x == null) {
            return null;
        }
        return new ByteValue(x.byteValue());
    }

    public static ShortValue cast(DoubleValue x, ShortValue y) {
        if (x == null) {
            return null;
        }
        return new ShortValue(x.shortValue());
    }
        
    public static IntValue cast(DoubleValue x, IntValue y) {
        if (x == null) {
            return null;
        }
        return new IntValue(x.intValue());
    }

    public static LongValue cast(DoubleValue x, LongValue y) {
        if (x == null) {
            return null;
        }
        return new LongValue(x.longValue());
    }
    
    public static FloatValue cast(DoubleValue x, FloatValue y) {
        if (x == null) {
            return null;
        }
        return new FloatValue(x.floatValue());
    }

    public static DoubleValue round(DoubleValue d, DoubleValue p) {
        validate(d, p, NumberOperations.ROUND);
        
        return new DoubleValue(new DoubleValue(Math.round(d.doubleValue() / p.doubleValue()) * p.doubleValue()),
          NumberOperations.ROUND,
          new DoubleValue[] { d, p });
    }
    
    public static org.openl.meta.DoubleValue round(org.openl.meta.DoubleValue value) {
        validate(value, NumberOperations.ROUND);
        
        return new org.openl.meta.DoubleValue(new org.openl.meta.DoubleValue((double) Math.round(value.getValue())), 
            NumberOperations.ROUND, new org.openl.meta.DoubleValue[] { value });
    }
        
    /**
     * @deprecated double value shouldn`t be empty.
     */
    @Deprecated
    public DoubleValue() {
        super();
    }

    @Deprecated
    /**
     * @deprecated format is not used inside Double value
     */
    public DoubleValue(double value, IMetaInfo metaInfo, String format) {
        super(metaInfo);
        this.value = value;
    }

    public DoubleValue(String valueString) {
        super();
        value = Double.parseDouble(valueString);
    }
   
    /**Function constructor**/
    public DoubleValue(DoubleValue result, NumberOperations function, DoubleValue[] params) {
        super(result, function, params);
        this.value = result.doubleValue();
    }

    public int compareTo(Number o) {
        return Double.compare(value, (o).doubleValue());
    }

    @Override
    public double doubleValue() {
        return value;
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
    
    @Deprecated
    public String getFormat() {
//        return format;
        return null;
    }

    @Deprecated
    public void setFormat(String format) {
//        this.format = format;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DoubleValue) {
            DoubleValue secondObj = (DoubleValue) obj;
            return value == secondObj.doubleValue();
        }

        return false;
    }

    @Override
    public int hashCode() {
        return ((Double) value).hashCode();
    }
    
    private static double[] unwrap(DoubleValue[] values) {
        if (ArrayTool.noNulls(values)) {
            double[] primitiveArray = new double[values.length];
            for (int i = 0; i < values.length; i++) {
                primitiveArray[i] = values[i].getValue();
            }
            return primitiveArray;
        }
        return ArrayUtils.EMPTY_DOUBLE_ARRAY;
    }

}
