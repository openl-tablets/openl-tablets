package org.openl.rules.convertor;

public abstract class NumberConvertor {

    public double numberModifier(String s) {
        if (s.endsWith("%")) {
            return 0.01;
        }

        return 1;
    }

    public String numberStringWithoutModifier(String s) {
        if (s.endsWith("%")) {
            return s.substring(0, s.length() - 1);
        }

        return s;
    }

}
