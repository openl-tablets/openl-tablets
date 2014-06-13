package org.openl.rules.convertor;

public class String2LongConvertor extends String2NumberConverter<Long> {

    @Override
    Long convert(Number number, String data) {
        double dValue = number.doubleValue();
        if (dValue > Long.MAX_VALUE || dValue < Long.MIN_VALUE) {
            throw new NumberFormatException("A number is out of range");
        }
        return number.longValue();
    }
}
