package org.openl.rules.convertor;

import java.text.DecimalFormat;

class String2IntConvertor extends String2NumberConverter<Integer> {

    @Override
    Integer convert(Number number, String data) {
        double dValue = number.doubleValue();
        if (dValue > Integer.MAX_VALUE || dValue < Integer.MIN_VALUE) {
            throw new NumberFormatException("A number is out of range [-2147483648...+2147483647]");
        }
        return number.intValue();
    }

    @Override
    DecimalFormat getFormatter(String format) {
        DecimalFormat formatter = super.getFormatter(format);
        formatter.setParseIntegerOnly(true);
        return formatter;
    }
}
