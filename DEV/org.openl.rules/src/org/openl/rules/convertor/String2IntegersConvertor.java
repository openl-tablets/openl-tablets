package org.openl.rules.convertor;

import java.math.BigDecimal;
import java.text.DecimalFormat;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
abstract class String2IntegersConvertor<T extends Number> extends String2NumberConverter<T> {

    private final long min;
    private final long max;

    @Override
    final T convert(Number number, String data) {
        // NaN and infinities are parsed as Double
        if (!(number instanceof BigDecimal decimal) || decimal.compareTo(BigDecimal.valueOf(min)) < 0
                || decimal.compareTo(BigDecimal.valueOf(max)) > 0) {
            throw new NumberFormatException(
                    "The number '%s' is out of the range [%s]".formatted(number, min + "...+" + max));
        }
        return toNumber(decimal.longValue());
    }

    abstract T toNumber(long number);

    @Override
    final DecimalFormat getFormatter(String format) {
        var formatter = super.getFormatter(format);
        formatter.setParseIntegerOnly(true);
        // Keeps the fraction of a percent value, such as 250%, so that parse() rejects it
        formatter.setParseBigDecimal(true);
        return formatter;
    }
}
