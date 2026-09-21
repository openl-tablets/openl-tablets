package org.openl.rules.table.properties.expressions.match;

import java.util.Objects;

import lombok.Getter;

import org.openl.util.StringUtils;

public abstract class AMatchingExpression implements IMatchingExpression {

    private String contextAttribute;
    @Getter
    private String operation;
    @Getter
    private String operationName;
    @Getter
    private IMatchingExpression contextAttributeExpression;

    protected AMatchingExpression(String operationName, IMatchingExpression matchingExpression) {
        this.contextAttributeExpression = Objects.requireNonNull(matchingExpression,
                "matchingExpression cannot be null");
        this.operationName = operationName;
    }

    protected AMatchingExpression(String operationName, String operation, String contextAttribute) {
        this.contextAttribute = Objects.requireNonNull(contextAttribute, "contextAttribute cannot be null");
        this.operationName = operationName;
        this.operation = operation;
    }

    protected AMatchingExpression(String contextAttribute) {
        this.contextAttribute = Objects.requireNonNull(contextAttribute, "contextAttribute cannot be null");
    }

    @Override
    public String getCodeExpression(String param) {
        if (StringUtils.isNotEmpty(param)) {
            return param + ' ' + getOperation() + ' ' + contextAttribute;
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

}
