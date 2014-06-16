package org.openl.rules.convertor;

import java.math.BigDecimal;
import java.text.DecimalFormat;

public class String2BigDecimalConvertor extends String2NumberConverter<BigDecimal> {

    @Override
    BigDecimal convert(Number number, String data) {

        return (BigDecimal) number;
    }

    @Override
    void configureFormatter(DecimalFormat df) {
        df.setParseBigDecimal(true);
    }
}
