package org.openl.rules.convertor;

public class String2FloatConvertor extends String2NumberConverter<Float> {

    public String2FloatConvertor() {
        super("0.##");
    }

    @Override
    Float convert(Number number, String data) {
        float value = number.floatValue();
        double dValue = number.doubleValue();
        if (dValue != Double.NEGATIVE_INFINITY
                && dValue != Double.POSITIVE_INFINITY
                && (value == Float.NEGATIVE_INFINITY || value == Float.POSITIVE_INFINITY)) {
            throw new NumberFormatException("A number \"" + data + "\" is out of range.");
        }
        return value;
    }
}
