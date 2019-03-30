package org.openl.rules.table.properties.expressions.match;

public class GEMatchingExpression extends AMatchingExpression {
        
    public static final String OPERATION_NAME = "GE";
    private static final String OPERATION = ">=";
 
    @Override
    public boolean isContextAttributeExpression() {
        return false;
    }
    
    public GEMatchingExpression(String contextAttribute) {
        super(OPERATION_NAME, OPERATION, contextAttribute);        
    }
}
