package org.openl.meta;

import org.apache.commons.lang.ArrayUtils;
import org.openl.binding.impl.Operators;
import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.meta.number.NumberOperations;
import org.openl.util.ArrayTool;
import org.openl.util.math.MathUtils;


public class DoubleValue extends ExplanationNumberValue<DoubleValue> {
    
    private static final long serialVersionUID = -4594250562069599646L;        
    
    private double value;    

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
    
    public static DoubleValue add(DoubleValue dv1, DoubleValue dv2) {
        validate(dv1, dv2, NumberOperations.ADD);
        
        return new DoubleValue(dv1, dv2, Operators.add(dv1.getValue(), dv2.getValue()), NumberOperations.ADD, 
            false);
    }
    
    public static DoubleValue rem(DoubleValue dv1, DoubleValue dv2) {
        validate(dv1, dv2, NumberOperations.REM);
        
        return new DoubleValue(dv1, dv2, Operators.rem(dv1.getValue(), dv2.getValue()), 
            NumberOperations.REM, true);
    }
    
    // ******* Autocasts *************
    
    public static DoubleValue autocast(byte x, DoubleValue y) {
        return new DoubleValue(x);
    }

    public static DoubleValue autocast(short x, DoubleValue y) {
        return new DoubleValue(x);
    }
    
    public static DoubleValue autocast(char x, DoubleValue y) {
        return new DoubleValue(x);    
    }

    public static DoubleValue autocast(int x, DoubleValue y) {
        return new DoubleValue(x);
    }

    public static DoubleValue autocast(long x, DoubleValue y) {
        return new DoubleValue(x);
    }

    public static DoubleValue autocast(float x, DoubleValue y) {
        return new DoubleValue(x);
    }

    public static DoubleValue autocast(double x, DoubleValue y) {
        return new DoubleValue(x);
    }

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
    
    public static DoubleValue copy(DoubleValue value, String name) {

        if (value.getName() == null) {
            value.setName(name);

            return value;
        } else if (!value.getName().equals(name)) {
            DoubleValue dv = new DoubleValue(value, NumberOperations.COPY, new DoubleValue[] { value });
            dv.setName(name);

            return dv;
        }

        return value;
    }

    public static DoubleValue divide(DoubleValue dv1, DoubleValue dv2) {
        validate(dv1, dv2, NumberOperations.DIVIDE);
        
        return new DoubleValue(dv1, dv2, Operators.divide(dv1.getValue(), dv2.getValue()), 
            NumberOperations.DIVIDE, true);
    }

    public static boolean eq(DoubleValue dv1, DoubleValue dv2) {
        validate(dv1, dv2, NumberOperations.EQ);
        
        return Operators.eq(dv1.getValue(), dv2.getValue());
    }

    public static boolean ge(DoubleValue dv1, DoubleValue dv2) {
        validate(dv1, dv2, NumberOperations.GE);
        
        return Operators.ge(dv1.getValue(), dv2.getValue());
    }

    public static boolean gt(DoubleValue dv1, DoubleValue dv2) {
        validate(dv1, dv2, NumberOperations.GT);
        
        return Operators.gt(dv1.getValue(), dv2.getValue());
    }

    public static boolean le(DoubleValue dv1, DoubleValue dv2) {
        validate(dv1, dv2, NumberOperations.LE);
        
        return Operators.le(dv1.getValue(), dv2.getValue());
    }

    public static boolean lt(DoubleValue dv1, DoubleValue dv2) {
        validate(dv1, dv2, NumberOperations.LT);
        
        return Operators.lt(dv1.getValue(), dv2.getValue());
    }

    public static DoubleValue max(DoubleValue dv1, DoubleValue dv2) {
        validate(dv1, dv2, NumberOperations.MAX);
        return new DoubleValue(dv2.getValue() > dv1.getValue() ? dv2 : dv1, NumberOperations.MAX, new DoubleValue[] { dv1, dv2 });
    }

    public static DoubleValue min(DoubleValue dv1, DoubleValue dv2) {
        validate(dv1, dv2, NumberOperations.MIN);
        
        return new DoubleValue(dv2.getValue() < dv1.getValue() ? dv2 : dv1,
            NumberOperations.MIN,
            new DoubleValue[] { dv1, dv2 });
    }

    public static DoubleValue multiply(DoubleValue dv1, DoubleValue dv2) {
        validate(dv1, dv2, NumberOperations.MULTIPLY);
        
        return new DoubleValue(dv1, dv2, Operators.multiply(dv1.getValue(), dv2.getValue()), 
            NumberOperations.MULTIPLY, true);
    }

    public static boolean ne(DoubleValue dv1, DoubleValue dv2) {
        validate(dv1, dv2, NumberOperations.NE);
        
        return Operators.ne(dv1.getValue(), dv2.getValue());
    }

    public static DoubleValue negative(DoubleValue value) {
        return multiply(value, new DoubleValue(-1D));
    }
    
    public static DoubleValue pow(DoubleValue dv1, DoubleValue dv2) {
        validate(dv1, dv2, NumberOperations.POW);
        
        return new DoubleValue(new DoubleValue(Operators.pow(dv1.getValue(), dv2.getValue())), 
            NumberOperations.POW, new DoubleValue[] { dv1, dv2 });
    }

    public static DoubleValue round(DoubleValue dv1) {
        validate(dv1, NumberOperations.ROUND);
        
        return new DoubleValue(new DoubleValue(Math.round(dv1.getValue())), NumberOperations.ROUND, new DoubleValue[] { dv1 });
    }

    public static DoubleValue round(DoubleValue d, DoubleValue p) {
        validate(d, p, NumberOperations.ROUND);
        
        return new DoubleValue(new DoubleValue(Math.round(d.doubleValue() / p.doubleValue()) * p.doubleValue()),
          NumberOperations.ROUND,
          new DoubleValue[] { d, p });
    }

    public static DoubleValue subtract(DoubleValue dv1, DoubleValue dv2) {
        validate(dv1, dv2, NumberOperations.SUBTRACT);
        
        return new DoubleValue(dv1, dv2, Operators.subtract(dv1.getValue(), dv2.getValue()), 
            NumberOperations.SUBTRACT, false);
    }   
    
    // Math functions
    
    public static DoubleValue max(DoubleValue[] values) {
        DoubleValue result = (DoubleValue) MathUtils.max(values);        
        return new DoubleValue((DoubleValue) getAppropriateValue(values, result), 
            NumberOperations.MAX_IN_ARRAY, values);
    }

    public static DoubleValue min(DoubleValue[] values) {
        DoubleValue result = (DoubleValue) MathUtils.min(values);
        return new DoubleValue((DoubleValue) getAppropriateValue(values, result), 
            NumberOperations.MIN_IN_ARRAY, values);
    }
    
    public static DoubleValue avg(DoubleValue[] values) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        double[] primitiveArray = unwrap(values);
        double avg = MathUtils.avg(primitiveArray);
        
        return new DoubleValue(new DoubleValue(avg), NumberOperations.AVG, values);
    }
    
    public static DoubleValue sum(DoubleValue[] values) { 
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        double[] primitiveArray = unwrap(values);
        double sum = MathUtils.sum(primitiveArray);
        return new DoubleValue(new DoubleValue(sum), NumberOperations.SUM, values);
    }
    
    public static DoubleValue median(DoubleValue[] values) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        double[] primitiveArray = unwrap(values);
        double median = MathUtils.median(primitiveArray);
        return new DoubleValue(new DoubleValue(median), NumberOperations.MEDIAN, values);
    }
    
    public static DoubleValue product(DoubleValue[] values) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        double[] primitiveArray = unwrap(values);
        double product = MathUtils.product(primitiveArray);
        
        // we loose the parameters, but not the result of computation.
        return new DoubleValue(new DoubleValue(product), NumberOperations.PRODUCT, null);
    }
    
    public static LongValue quaotient(DoubleValue number, DoubleValue divisor) {
        if (number != null && divisor != null) {
            LongValue result = new LongValue(MathUtils.quaotient(number.getValue(), divisor.getValue()));
            return new LongValue(result, NumberOperations.QUAOTIENT, null);
        }
        return null;
    }
    
    public static DoubleValue mod(DoubleValue number, DoubleValue divisor) {
        if (number != null && divisor != null) {
            DoubleValue result = new DoubleValue(MathUtils.mod(number.getValue(), divisor.getValue()));
            return new DoubleValue(result, NumberOperations.MOD, new DoubleValue[]{number, divisor} );
        }
        return null;
    }
    
    public static DoubleValue small(DoubleValue[] values, int position) {
        if (ArrayUtils.isEmpty(values)) {
            return null;
        }
        double[] primitiveArray = unwrap(values);
        double small = MathUtils.small(primitiveArray, position);
        return new DoubleValue((DoubleValue) getAppropriateValue(values, new DoubleValue(small)), 
            NumberOperations.SMALL, values);
    }
    
    /**
     * @deprecated double value shouldn`t be empty.
     */
    @Deprecated
    public DoubleValue() {
        super();
    }

    public DoubleValue(double value) {
        super();
        this.value = value;
    }

    @Deprecated
    /**
     * @deprecated format is not used inside Double value
     */
    public DoubleValue(double value, IMetaInfo metaInfo, String format) {
        super(metaInfo);
        this.value = value;
    }

    public DoubleValue(double value, String name) {
        super(name);
        this.value = value;
    }

    public DoubleValue(String valueString) {
        super();
        value = Double.parseDouble(valueString);
    }
    
    /**Formula constructor**/
    public DoubleValue(DoubleValue dv1, DoubleValue dv2, double value, NumberOperations operand, boolean isMultiplicative) {
        super(dv1, dv2, operand, isMultiplicative);
        this.value = value;
    }
    
    /**Function constructor**/
    public DoubleValue(DoubleValue result, NumberOperations function, DoubleValue[] params) {
        super(result, function, params);
        this.value = result.doubleValue();
    }

    public int compareTo(Number o) {
        return Double.compare(value, (o).doubleValue());
    }

    public DoubleValue copy(String name) {
        return copy(this, name);
    }

    @Override
    public double doubleValue() {
        return value;
    }

    @Override
    public float floatValue() {
        return (float) value;
    }

    @Deprecated
    public String getFormat() {
//        return format;
        return null;
    }

    public double getValue() {
        return value;
    }

    @Override
    public int intValue() {
        return (int) value;
    }

    @Override
    public long longValue() {
        return (long) value;
    }

    public String printValue() {
        return String.valueOf(value);
    }

    @Deprecated
    public void setFormat(String format) {
//        this.format = format;
    }

    public void setValue(double value) {
        this.value = value;
    }
    
    public static DoubleValue abs(DoubleValue value) {
        // evaluate result
        DoubleValue result = new DoubleValue(Operators.abs(value.getValue()));
        // create instance with information about last operation
        return new DoubleValue(result, NumberOperations.ABS, new DoubleValue[] { value });
    }
    
    public static DoubleValue inc(DoubleValue value) {
        return add(value, new DoubleValue(1D));
    }
    
    public static DoubleValue dec(DoubleValue value) {
        return subtract(value, new DoubleValue(1D));
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
    
    public static DoubleValue positive(DoubleValue value) {
        return value;
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
