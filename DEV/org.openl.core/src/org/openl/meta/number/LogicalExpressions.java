package org.openl.meta.number;

public enum LogicalExpressions {
    EQ("equal"),
    GE("greater or equal"),
    GT("greater"),
    LE("less or equal"),
    LT("less"),
    NE("not equal");
    
    private String description;
    
    private LogicalExpressions(String description) {
        this.description = description;
    }
    
    @Override
    public String toString() {
        return description;
    }
    
    public String getName() {
        return name().toLowerCase();
    }
    
    public String getFullName() {
        return this.getClass().getSimpleName() + "." + name();
    }

}
