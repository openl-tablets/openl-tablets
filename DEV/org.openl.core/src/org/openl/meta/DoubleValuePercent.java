package org.openl.meta;

@SuppressWarnings("deprecation")
public class DoubleValuePercent extends DoubleValue {

    private static final long serialVersionUID = 6543033363886217906L;

    public static final String PERCENT_FORMAT = "#.####%";

    public DoubleValuePercent(double d) {
        super(d);
        setFormat(PERCENT_FORMAT);
    }

    public DoubleValuePercent(String valueStr) {
        super(valueStr);
        setFormat(PERCENT_FORMAT);
    }

}
