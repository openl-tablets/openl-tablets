package org.openl.rules.calc.element;

import org.openl.util.StringUtils;

public enum SpreadsheetExpressionMarker {

    OPEN_CURLY_BRACKET("{"),
    CLOSED_CURLY_BRACKET("}"),
    EQUALS_SIGN("=");

    private String symbol;

    private SpreadsheetExpressionMarker(String marker) {
        this.symbol = marker;
    }

    public static boolean isFormula(String src) {

        if (StringUtils.isBlank(src)) {
            return false;
        }

        if (src.startsWith(OPEN_CURLY_BRACKET.getSymbol()) && src.endsWith(CLOSED_CURLY_BRACKET.getSymbol())) {
            return true;
        }

        return src.startsWith(EQUALS_SIGN
            .getSymbol()) && (src.length() > 2 || src.length() == 2 && Character.isLetterOrDigit(src.charAt(1)));
    }

    @Override
    public String toString() {
        return name() + symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}
