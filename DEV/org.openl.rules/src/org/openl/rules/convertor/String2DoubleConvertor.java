package org.openl.rules.convertor;

import java.text.DecimalFormat;
import java.text.ParsePosition;

class String2DoubleConvertor extends String2NumberConverter<Double> {

    @Override
    public Double parse(String data, String format) {
        var value = super.parse(data, format);
        if (value == null || value != 0) {
            return value;
        }
        // A BigDecimal has no negative zero, so a zero is read again as a double to keep the sign of its text
        var formatter = getFormatter(format);
        formatter.setParseBigDecimal(false);
        var number = data.endsWith("%") ? data.substring(0, data.length() - 1) : data;
        return formatter.parse(number, new ParsePosition(0)).doubleValue();
    }

    @Override
    Double convert(Number number, String data) {
        return number.doubleValue();
    }

    @Override
    DecimalFormat getFormatter(String format) {
        var formatter = super.getFormatter(format);
        // Always show .0 at the end for integer numbers
        formatter.setMinimumFractionDigits(1);
        // Divides a percent value, such as 0.07%, by 100 before it is rounded to a double
        formatter.setParseBigDecimal(true);
        return formatter;
    }
}
