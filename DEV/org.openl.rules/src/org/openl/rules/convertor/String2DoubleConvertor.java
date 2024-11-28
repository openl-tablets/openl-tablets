package org.openl.rules.convertor;

import java.text.DecimalFormat;

class String2DoubleConvertor extends String2NumberConverter<Double> {

    @Override
    Double convert(Number number, String data) {
        return number.doubleValue();
    }

    @Override
    DecimalFormat getFormatter(String format) {
        DecimalFormat formatter = super.getFormatter(format);
        // Always show .0 at the end for integer numbers
        formatter.setMinimumFractionDigits(1);
        return formatter;
    }
}
