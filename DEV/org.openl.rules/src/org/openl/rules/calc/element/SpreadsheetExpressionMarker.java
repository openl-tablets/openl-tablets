package org.openl.rules.calc.element;

public enum SpreadsheetExpressionMarker {
    
    OPEN_CURLY_BRACKET("{"),
    CLOSED_CURLY_BRACKET("}"),
    EQUALS_SIGN("=");
    
    private String symbol;
    
    private SpreadsheetExpressionMarker(String marker) {
        this.symbol = marker;
    }
    
    @Override
    public String toString() {        
        return  name() + symbol;
    }
    
    public String getSymbol() {
        return symbol;
    }

}
