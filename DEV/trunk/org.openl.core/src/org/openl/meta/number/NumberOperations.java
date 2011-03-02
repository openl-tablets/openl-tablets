package org.openl.meta.number;

/**
 * Operations with descriptions for {@link NumberValue}.<br>
 * Including formulas and functions.
 * 
 * @author DLiauchuk
 *
 */
public enum NumberOperations {
    ADD("+"),
    MULTIPLY("*"),
    SUBTRACT("-"),
    DIVIDE("/"),  
    REM("%"),
    
    COPY("COPY"),
    MAX("max"),    
    MIN("min"),
    ROUND("round"),
    POW("pow"),
    ABS("abs");
    
    private String description;
    
    private NumberOperations(String description) {
        this.description = description;
    }
    
    @Override
    public String toString() {
        return description;
    }

}
