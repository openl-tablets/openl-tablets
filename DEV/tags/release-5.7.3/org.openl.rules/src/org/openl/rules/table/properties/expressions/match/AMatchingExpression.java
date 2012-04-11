package org.openl.rules.table.properties.expressions.match;

import org.apache.commons.lang.StringUtils;

public abstract class AMatchingExpression implements IMatchingExpression {
    
    private String contextAttribute;
    private String operation;
    private String operationName;
    
    public AMatchingExpression(String operationName, String operation, String contextAttribute) {
        this.operationName = operationName;
        this.operation = operation;
        
        if (contextAttribute == null) {
            throw new IllegalArgumentException("Parameter 'contextAttribute' can not be null");
        }
        this.contextAttribute = contextAttribute;       
    }
    
    public AMatchingExpression(String contextAttribute) {
        if (contextAttribute == null) {
            throw new IllegalArgumentException("Parameter 'contextAttribute' can not be null");
        }
        this.contextAttribute = contextAttribute;
    }
    
    public String getCodeExpression(String param) {
        if (StringUtils.isNotEmpty(param)) {
            return String.format("%s %s %s", param, getOperation(), contextAttribute); 
        }
        return null;
    }
    
    public String getContextAtribute() {
        return contextAttribute;
    }
    
    public String getOperation() {        
        return operation;
    }

    public String getOperationName() {        
        return operationName;
    }

}
