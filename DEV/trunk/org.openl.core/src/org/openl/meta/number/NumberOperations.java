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
    EQ("equal"),
    GE("greater or equal"),
    GT("greater"),
    LE("less or equal"),
    LT("less"),
    NE("not equal"),
    
    
    COPY("COPY"),
    MAX("max"), 
    MAX_IN_ARRAY("max in array"),
    MIN("min"),
    MIN_IN_ARRAY("min in array"),
    ROUND("round"),
    POW("pow"),
    ABS("abs"),
    AVG("average"),
    SUM("sum"), 
    MEDIAN("median"), 
    PRODUCT("product"), 
    QUAOTIENT("quaotient"), 
    MOD("mod"), 
    SMALL("small");
    
    private String description;
    
    private NumberOperations(String description) {
        this.description = description;
    }
    
    @Override
    public String toString() {
        return description;
    }

}
