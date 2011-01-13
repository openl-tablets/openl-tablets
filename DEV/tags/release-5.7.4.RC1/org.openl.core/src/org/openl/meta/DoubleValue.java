package org.openl.meta;

import java.text.DecimalFormat;
import java.text.Format;
import java.util.HashMap;
import java.util.Map;

import org.openl.meta.explanation.ExplanationNumberValue;

public class DoubleValue extends ExplanationNumberValue<DoubleValue> {
    
    private static final long serialVersionUID = -4594250562069599646L;
    private String format = "#0.####";
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

    private static Map<String, Format> formats = new HashMap<String, Format>();
    
    public static DoubleValue add(DoubleValue dv1, DoubleValue dv2) {

        if (dv1 == null || dv1.getValue() == 0) {
            return dv2;
        }

        if (dv2 == null || dv2.getValue() == 0) {
            return dv1;
        }

        return new DoubleValue(dv1, dv2, dv1.getValue() + dv2.getValue(), "+", false);
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
            DoubleValue dv = new DoubleValue(value, "COPY", new DoubleValue[] { value });
            dv.setName(name);

            return dv;
        }

        return value;
    }

    public static DoubleValue divide(DoubleValue dv1, DoubleValue dv2) {
        return new DoubleValue(dv1, dv2, dv1.getValue() / dv2.getValue(), "/", true);
    }

    public static boolean eq(DoubleValue dv1, DoubleValue dv2) {
        return dv1.getValue() == dv2.getValue();
    }

    public static boolean ge(DoubleValue dv1, DoubleValue dv2) {
        return dv1.getValue() >= dv2.getValue();
    }

    public static synchronized Format getFormat(String fmt) {

        Format format = formats.get(fmt);

        if (format == null) {
            format = new DecimalFormat(fmt);
            formats.put(fmt, format);
        }

        return format;
    }

    public static boolean gt(DoubleValue dv1, DoubleValue dv2) {
        return dv1.getValue() > dv2.getValue();
    }

    public static boolean le(DoubleValue dv1, DoubleValue dv2) {
        return dv1.getValue() <= dv2.getValue();
    }

    public static boolean lt(DoubleValue dv1, DoubleValue dv2) {
        return dv1.getValue() < dv2.getValue();
    }

    public static DoubleValue max(DoubleValue dv1, DoubleValue dv2) {
        return new DoubleValue(dv2.getValue() > dv1.getValue() ? dv2 : dv1, "max", new DoubleValue[] { dv1, dv2 });

    }

    public static DoubleValue min(DoubleValue dv1, DoubleValue dv2) {
        return new DoubleValue(dv2.getValue() < dv1.getValue() ? dv2 : dv1,
            "min",
            new DoubleValue[] { dv1, dv2 });
    }

    public static DoubleValue multiply(DoubleValue dv1, DoubleValue dv2) {
        return new DoubleValue(dv1, dv2, dv1.getValue() * dv2.getValue(), "*", true);
    }

    public static boolean ne(DoubleValue dv1, DoubleValue dv2) {
        return dv1.getValue() != dv2.getValue();
    }

    public static DoubleValue negative(DoubleValue dv) {
        DoubleValue neg = new DoubleValue(-dv.getValue());
        neg.setMetaInfo(dv.getMetaInfo());

        return neg;
    }

    public static DoubleValue pow(DoubleValue dv1, DoubleValue dv2) {
        return new DoubleValue(new DoubleValue(Math.pow(dv1.getValue(), dv2.getValue())), "pow", new DoubleValue[] { dv1, dv2 });
    }

    public static DoubleValue round(DoubleValue dv1) {
        return new DoubleValue(new DoubleValue(Math.round(dv1.getValue())), "round", new DoubleValue[] { dv1 });
    }

    public static DoubleValue round(DoubleValue d, DoubleValue p) {
        if (d == null) {
            return ZERO;
        }
        return new DoubleValue(new DoubleValue(Math.round(d.doubleValue() / p.doubleValue()) * p.doubleValue()),
          "round",
          new DoubleValue[] { d, p });
    }

    public static DoubleValue subtract(DoubleValue dv1, DoubleValue dv2) {
        if (dv2 == null || dv2.getValue() == 0) {
            return dv1;
        }
        return new DoubleValue(dv1, dv2, dv1.getValue() - dv2.getValue(), "-", false);
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

    public DoubleValue(double value, IMetaInfo metaInfo, String format) {
        super(metaInfo);
        this.value = value;
        this.format = format;
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
    public DoubleValue(DoubleValue dv1, DoubleValue dv2, double value, String operand, boolean isMultiplicative) {
        super(dv1, dv2, operand, isMultiplicative);
        this.value = value;
    }
    
    /**Function constructor**/
    public DoubleValue(DoubleValue result, String functionName, DoubleValue[] params) {
        super(result, functionName, params);
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

    public String getFormat() {
        return format;
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
        return printValue(format);
    }

    public String printValue(String format) {
        return getFormat(format).format(value);
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public void setValue(double value) {
        this.value = value;
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
}
