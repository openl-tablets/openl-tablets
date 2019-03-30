package org.openl.rules.table.properties.expressions.match;

import org.openl.util.StringUtils;

public abstract class AMatchingExpression implements IMatchingExpression {
    
    private String contextAttribute;
    private String operation;
    private String operationName;
    private IMatchingExpression contextAttributeExpression;
    
    public IMatchingExpression getContextAttributeExpression() {
        return contextAttributeExpression;
    }
    
    public AMatchingExpression(String operationName, IMatchingExpression matchingExpression) {
        this.operationName = operationName;
              
        if (matchingExpression == null) {
            throw new IllegalArgumentException("Parameter 'contextAttributeExpression' can not be null");
        }
        this.contextAttributeExpression = matchingExpression;       
    }
    
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
            return new StringBuilder(64).append(param).append(' ').append(getOperation()).append(' ').append(contextAttribute).toString();
        }
        return null;
    }
    
    public String getContextAttribute() {
        if (!isContextAttributeExpression()){
            return contextAttribute;
        }else{
            return getContextAttributeExpression().getContextAttribute();
        }
    }
    
    public String getOperation() {        
        return operation;
    }

    public String getOperationName() {        
        return operationName;
    }

}
