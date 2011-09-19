package org.openl.rules.table.properties.expressions.match;

import org.openl.exception.OpenLRuntimeException;

public class MAXMatchingExpression extends AMatchingExpression {
        
    public static final String OPERATION_NAME = "MAX";
   
    public boolean isContextAttributeExpression() {
        return true;
    }
    
    public MAXMatchingExpression(IMatchingExpression matchingExpression) {
        super(OPERATION_NAME, matchingExpression);        
    }
    
    public String getOperation() {
        throw new OpenLRuntimeException("Operation is not supported");
    }
}
