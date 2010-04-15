package org.openl.rules.calc.element;

import org.openl.meta.DoubleValue;
import org.openl.meta.IMetaHolder;
import org.openl.meta.IMetaInfo;
import org.openl.meta.StringValue;
import org.openl.rules.convertor.String2DataConvertorFactory;

public class AnyCellValue implements IMetaHolder {

    private DoubleValue doubleValue;
    private StringValue stringValue;

    public AnyCellValue(DoubleValue doubleValue) {
        this.doubleValue = doubleValue;
    }

    public AnyCellValue(String source) {

        try {
            double value = (Double) new String2DataConvertorFactory.String2DoubleConvertor().parse(source, null, null);
            this.doubleValue = new DoubleValue(value);
        } catch (Throwable t) {
            this.stringValue = new StringValue(source);
        }
    }

    public DoubleValue getDoubleValue() {
        return doubleValue;
    }

    public IMetaInfo getMetaInfo() {
        return null;
    }

    public Object getValue() {
        return stringValue == null ? doubleValue : stringValue;
    }

    public void setMetaInfo(IMetaInfo info) {

        if (doubleValue != null) {
            doubleValue.setMetaInfo(info);
        } else if (stringValue != null) {
            stringValue.setMetaInfo(info);
        }
    }

    @Override
    public String toString() {
        return doubleValue != null ? doubleValue.toString() : String.valueOf(stringValue);
    }

}
