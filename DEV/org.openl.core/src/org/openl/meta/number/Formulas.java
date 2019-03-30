package org.openl.meta.number;

public enum Formulas {
    ADD("+", false),
    MULTIPLY("*", true),
    SUBTRACT("-", false),
    DIVIDE("/", true),
    REM("%", true);

    private String operand;
    private boolean isMultiplicative;

    Formulas(String operand, boolean isMultiplicative) {
        this.operand = operand;
        this.isMultiplicative = isMultiplicative;
    }

    public boolean isMultiplicative() {
        return isMultiplicative;
    }

    @Override
    public String toString() {
        return operand;
    }

    public String getName() {
        return name().toLowerCase();
    }
}
