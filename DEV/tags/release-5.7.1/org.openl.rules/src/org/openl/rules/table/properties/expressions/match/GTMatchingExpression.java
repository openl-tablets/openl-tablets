package org.openl.rules.table.properties.expressions.match;

public class GTMatchingExpression extends AMatchingExpression {
    
    public static final String OPERATION_NAME = "GT";
    private static final String OPERATION = ">=";
    
    public GTMatchingExpression(String contextAttribute) {
        super(OPERATION_NAME, OPERATION, contextAttribute);        
    }
}
