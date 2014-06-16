package org.openl.rules.convertor;

import java.text.DecimalFormat;

public class String2LongConvertor extends String2NumberConverter<Long> {

    @Override
    Long convert(Number number, String data) {
        if (number instanceof Long) {
            return (Long) number;
        } else {
            throw new NumberFormatException("A number is out of range");
        }
    }

    @Override
    void configureFormatter(DecimalFormat df) {
        df.setParseIntegerOnly(true);
    }
}
