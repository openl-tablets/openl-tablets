package org.openl.rules.convertor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;

class String2BigIntegerConvertor extends String2NumberConverter<BigInteger> {

    @Override
    BigInteger convert(Number number, String data) {

        return ((BigDecimal) number).toBigIntegerExact();
    }

    @Override
    DecimalFormat getFormatter(String format) {
        DecimalFormat formatter = super.getFormatter(format);
        formatter.setParseIntegerOnly(true);
        formatter.setParseBigDecimal(true);
        return formatter;
    }
}
