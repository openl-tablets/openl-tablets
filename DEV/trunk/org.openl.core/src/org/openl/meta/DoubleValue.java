package org.openl.meta;

import java.text.DecimalFormat;
import java.text.Format;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openl.base.INamedThing;
import org.openl.util.AOpenIterator;
import org.openl.util.tree.ITreeElement;

public class DoubleValue extends Number implements IMetaHolder, Comparable<Number>, ITreeElement<DoubleValue>, INamedThing {

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

    private static final long serialVersionUID = -4594250562069599646L;

    public static final int VALUE = 0x01, SHORT_NAME = 0x02, LONG_NAME = 0x04, URL = 0x08, EXPAND_FORMULA = 0x10,
            EXPAND_FUNCTION = 0x20, PRINT_VALUE_IN_EXPANDED = 0x40,
            EXPAND_ALL = EXPAND_FORMULA | EXPAND_FUNCTION | PRINT_VALUE_IN_EXPANDED,
            PRINT_ALL = EXPAND_ALL | LONG_NAME;

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

        return new DoubleValueFormula(dv1, dv2, dv1.getValue() + dv2.getValue(), "+", false);
    }

    public static DoubleValue autocast(double x, DoubleValue y) {
        return new DoubleValue(x);
    }

    public static DoubleValue autocast(int x, DoubleValue y) {
        return new DoubleValue(x);
    }

    public static DoubleValue autocast(long x, DoubleValue y) {
        return new DoubleValue(x);
    }

    public static DoubleValue copy(DoubleValue value, String name) {

        if (value.getName() == null) {
            value.setName(name);

            return value;
        } else if (!value.getName().equals(name)) {
            DoubleValue dv = new DoubleValueFunction(value.doubleValue(), "COPY", new DoubleValue[] { value });
            dv.setName(name);

            return dv;
        }

        return value;
    }

    public static DoubleValue divide(DoubleValue dv1, DoubleValue dv2) {
        return new DoubleValueFormula(dv1, dv2, dv1.getValue() / dv2.getValue(), "/", true);
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
        return new DoubleValueFunction(dv2.getValue() > dv1.getValue() ? dv2 : dv1,
            "max",
            new DoubleValue[] { dv1, dv2 });
    }

    public static DoubleValue min(DoubleValue dv1, DoubleValue dv2) {
        return new DoubleValueFunction(dv2.getValue() < dv1.getValue() ? dv2 : dv1,
            "min",
            new DoubleValue[] { dv1, dv2 });
    }

    public static DoubleValue multiply(DoubleValue dv1, DoubleValue dv2) {
        return new DoubleValueFormula(dv1, dv2, dv1.getValue() * dv2.getValue(), "*", true);
    }

    public static boolean ne(DoubleValue dv1, DoubleValue dv2) {
        return dv1.getValue() != dv2.getValue();
    }

    public static DoubleValue negative(DoubleValue dv) {
        DoubleValue neg = new DoubleValue(-dv.value);
        neg.metaInfo = dv.metaInfo;

        return neg;
    }

    public static DoubleValue pow(DoubleValue dv1, DoubleValue dv2) {
        return new DoubleValueFunction(Math.pow(dv1.value, dv2.value), "pow", new DoubleValue[] { dv1, dv2 });
    }

    public static DoubleValue round(DoubleValue dv1) {
        return new DoubleValueFunction(Math.round(dv1.getValue()), "round", new DoubleValue[] { dv1 });
    }

    public static DoubleValue round(DoubleValue d, DoubleValue p) {

        if (d == null) {
            return ZERO;
        }

        return new DoubleValueFunction(Math.round(d.doubleValue() / p.doubleValue()) * p.doubleValue(),
            "round",
            new DoubleValue[] { d, p });
    }

    public static DoubleValue subtract(DoubleValue dv1, DoubleValue dv2) {

        if (dv2 == null || dv2.getValue() == 0) {
            return dv1;
        }

        return new DoubleValueFormula(dv1, dv2, dv1.getValue() - dv2.getValue(), "-", false);
    }

    private String format = "#0.####";
    private IMetaInfo metaInfo;
    protected double value;

    public DoubleValue() {
    }

    public DoubleValue(double value) {
        this.value = value;
    }

    public DoubleValue(double value, IMetaInfo metaInfo, String format) {
        this.metaInfo = metaInfo;
        this.value = value;
        this.format = format;
    }

    public DoubleValue(double value, String name) {
        this.value = value;
        ValueMetaInfo mi = new ValueMetaInfo();
        mi.setShortName(name);
        metaInfo = mi;
    }

    public DoubleValue(String valueString) {
        value = Double.parseDouble(valueString);
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

    public Iterator<DoubleValue> getChildren() {
        return AOpenIterator.empty();
    }

    public String getDisplayName(int mode) {

        switch (mode) {
            case SHORT:
                return printValue();
            default:
                String name = metaInfo == null ? null : getMetaInfo().getDisplayName(mode);
                return name == null ? printValue() : name + "(" + printValue() + ")";
        }
    }

    public String getFormat() {
        return format;
    }

    public IMetaInfo getMetaInfo() {
        return metaInfo;
    }

    public String getName() {

        if (metaInfo == null) {
            return null;
        }

        return metaInfo.getDisplayName(IMetaInfo.LONG);
    }

    public DoubleValue getObject() {
        return this;
    }

    public String getType() {
        return "value";
    }

    public double getValue() {
        return value;
    }

    @Override
    public int intValue() {
        return (int) value;
    }

    public boolean isLeaf() {
        return true;
    }

    @Override
    public long longValue() {
        return (long) value;
    }

    protected String printContent(int mode, boolean fromMultiplicativeExpr, boolean inBrackets) {
        return printValue();
    }

    public String printExplanation(int mode, boolean fromMultiplicativeExpr, List<String> urls) {

        if (urls != null && metaInfo != null && metaInfo.getSourceUrl() != null) {
            urls.add("" + metaInfo.getDisplayName(IMetaInfo.LONG) + " -> " + metaInfo.getSourceUrl());
        }

        return printExplanationLocal(mode, fromMultiplicativeExpr);
    }

    protected String printExplanationLocal(int mode, boolean fromMultiplicativeExpr) {

        switch (mode & (~EXPAND_ALL)) {
            case VALUE:
                return printContent(mode, fromMultiplicativeExpr, false);
            case SHORT_NAME:
                return metaInfo == null ? printContent(mode, fromMultiplicativeExpr, false)
                                       : metaInfo.getDisplayName(IMetaInfo.LONG) + "(" + printContent(mode, false, true) + ")";
            case LONG_NAME:
                return metaInfo == null ? printContent(mode, fromMultiplicativeExpr, false)
                                       : metaInfo.getDisplayName(IMetaInfo.LONG) + "(" + printContent(mode, false, true) + ")";
            default:
        }

        throw new RuntimeException("Wrong print mode!!");
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

    public void setFullName(String name) {
        if (metaInfo == null) {
            metaInfo = new ValueMetaInfo();
        }
        if (metaInfo instanceof ValueMetaInfo) {
            ((ValueMetaInfo) metaInfo).setFullName(name);
        }
    }

    public void setMetaInfo(IMetaInfo metaInfo) {
        this.metaInfo = metaInfo;
    }

    public void setName(String name) {
        if (metaInfo == null) {
            metaInfo = new ValueMetaInfo();
        }
        if (metaInfo instanceof ValueMetaInfo) {
            ((ValueMetaInfo) metaInfo).setShortName(name);
        }
    }

    public void setValue(double value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return printValue();
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
