package org.openl.rules.table.properties.expressions.match;

public class LTMatchingExpression extends AMatchingExpression {

    public static final String OPERATION_NAME = "LT";
    private static final String OPERATION = "<";

    @Override
    public boolean isContextAttributeExpression() {
        return false;
    }

    public LTMatchingExpression(String contextAttribute) {
        super(OPERATION_NAME, OPERATION, contextAttribute);
    }

}
