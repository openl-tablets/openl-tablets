package org.openl.rules.convertor;

class String2DoubleConvertor extends String2NumberConverter<Double> {

    public String2DoubleConvertor() {
        super("0.##");
    }

    @Override
    Double convert(Number number, String data) {
        return number.doubleValue();
    }
}
