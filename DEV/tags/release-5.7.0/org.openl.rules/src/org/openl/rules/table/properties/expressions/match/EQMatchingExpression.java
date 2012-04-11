package org.openl.rules.table.properties.expressions.match;

public class EQMatchingExpression extends AMatchingExpression {
    
    public static final String OPERATION_NAME = "EQ";
    public static final String OPERATION = "==";
    
    public EQMatchingExpression(String contextAttribute) {
        super(OPERATION_NAME, OPERATION, contextAttribute);        
    }
}
