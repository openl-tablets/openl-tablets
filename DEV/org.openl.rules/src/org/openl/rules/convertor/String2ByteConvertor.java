package org.openl.rules.convertor;

public class String2ByteConvertor extends String2NumberConverter<Byte> {

    @Override
    Byte convert(Number number, String data) {
        double dValue = number.doubleValue();
        if (dValue > Byte.MAX_VALUE || dValue < Byte.MIN_VALUE) {
            throw new NumberFormatException("A number is out of range [-128...+127]");
        }
        return number.byteValue();
    }
}
