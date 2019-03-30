package org.openl.rules.table.properties.expressions.match;

public class ContainsMatchingExpression extends AMatchingExpression {

    public static final String OPERATION_NAME = "CONTAINS";

    @Override
    public boolean isContextAttributeExpression() {
        return false;
    }

    public ContainsMatchingExpression(String contextAttribute) {
        super(OPERATION_NAME, EQMatchingExpression.OPERATION, contextAttribute);
    }

}
