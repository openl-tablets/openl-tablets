package org.openl.rules.convertor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;

public class String2BigIntegerConvertor extends String2NumberConverter<BigInteger> {

    @Override
    BigInteger convert(Number number, String data) {

        return ((BigDecimal) number).toBigIntegerExact();
    }

    @Override
    void configureFormatter(DecimalFormat df) {
        df.setParseIntegerOnly(true);
        df.setParseBigDecimal(true);
    }
}
