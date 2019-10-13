package org.openl.rules.table.properties.expressions.match;

import java.util.Objects;

import org.openl.util.StringUtils;

public abstract class AMatchingExpression implements IMatchingExpression {

    private String contextAttribute;
    private String operation;
    private String operationName;
    private IMatchingExpression contextAttributeExpression;

    @Override
    public IMatchingExpression getContextAttributeExpression() {
        return contextAttributeExpression;
    }

    public AMatchingExpression(String operationName, IMatchingExpression matchingExpression) {
        this.contextAttributeExpression = Objects.requireNonNull(matchingExpression,
            "matchingExpression can't be null.");
        this.operationName = operationName;
    }

    public AMatchingExpression(String operationName, String operation, String contextAttribute) {
        this.contextAttribute = Objects.requireNonNull(contextAttribute, "contextAttribute can't be null.");
        this.operationName = operationName;
        this.operation = operation;
    }

    public AMatchingExpression(String contextAttribute) {
        this.contextAttribute = Objects.requireNonNull(contextAttribute, "contextAttribute can't be null.");
    }

    @Override
    public String getCodeExpression(String param) {
        if (StringUtils.isNotEmpty(param)) {
            return new StringBuilder(64).append(param)
                .append(' ')
                .append(getOperation())
                .append(' ')
                .append(contextAttribute)
                .toString();
        }
        return null;
    }

    @Override
    public String getContextAttribute() {
        if (!isContextAttributeExpression()) {
            return contextAttribute;
        } else {
            return getContextAttributeExpression().getContextAttribute();
        }
    }

    public String getOperation() {
        return operation;
    }

    @Override
    public String getOperationName() {
        return operationName;
    }

}
