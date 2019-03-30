package org.openl.rules.convertor;

import java.text.DecimalFormat;

abstract class String2IntegersConvertor<T extends Number> extends String2NumberConverter<T> {

    private final long min;
    private final long max;

    String2IntegersConvertor(long min, long max) {
        this.min = min;
        this.max = max;
    }

    @Override
    final T convert(Number number, String data) {
        if (!(number instanceof Long)) {
            throwException(number);
        }
        long dValue = number.longValue();
        if (dValue > max || dValue < min) {
            throwException(number);
        }
        return toNumber(dValue);
    }

    private void throwException(Number number) {
        throw new NumberFormatException("The number '" + number + "' is out of the range [" + min + "...+" + max + "]");
    }

    abstract T toNumber(long number);

    @Override
    final DecimalFormat getFormatter(String format) {
        DecimalFormat formatter = super.getFormatter(format);
        formatter.setParseIntegerOnly(true);
        return formatter;
    }
}
