package org.openl.rules.convertor;

import java.text.DecimalFormat;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
abstract class String2IntegersConvertor<T extends Number> extends String2NumberConverter<T> {

    private final long min;
    private final long max;

    @Override
    final T convert(Number number, String data) {
        if (!(number instanceof Long)) {
            throwException(number);
        }
        var dValue = number.longValue();
        if (dValue > max || dValue < min) {
            throwException(number);
        }
        return toNumber(dValue);
    }

    private void throwException(Number number) {
        throw new NumberFormatException(
                "The number '%s' is out of the range [%s]".formatted(number, min + "...+" + max));
    }

    abstract T toNumber(long number);

    @Override
    final DecimalFormat getFormatter(String format) {
        var formatter = super.getFormatter(format);
        formatter.setParseIntegerOnly(true);
        return formatter;
    }
}
