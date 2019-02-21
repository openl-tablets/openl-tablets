package org.openl.rules.convertor;

import org.openl.meta.BigDecimalValue;

import java.math.BigDecimal;
import java.text.DecimalFormat;

class String2BigDecimalValueConverter extends String2NumberConverter<BigDecimalValue> {

    @Override
    BigDecimalValue convert(Number number, String data) {
        if (number == null) {
            return null;
        }
        return new BigDecimalValue(((BigDecimal) number).stripTrailingZeros());
    }

    @Override
    public String format(BigDecimalValue data, String format) {
        if (data == null) {
            return null;
        }
        DecimalFormat df = getFormatter(format);
        return df.format(data.getValue());
    }

    @Override
    DecimalFormat getFormatter(String format) {
        DecimalFormat formatter = super.getFormatter(format);
        // Always show .0 at the end for integer numbers
        formatter.setMinimumFractionDigits(1);
        formatter.setParseBigDecimal(true);
        return formatter;
    }
}
