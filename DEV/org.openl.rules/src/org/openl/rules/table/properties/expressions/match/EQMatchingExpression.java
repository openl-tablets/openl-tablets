package org.openl.rules.table.properties.expressions.match;

public class EQMatchingExpression extends AMatchingExpression {

    public static final String OPERATION_NAME = "EQ";
    public static final String OPERATION = "==";

    @Override
    public boolean isContextAttributeExpression() {
        return false;
    }

    public EQMatchingExpression(String contextAttribute) {
        super(OPERATION_NAME, OPERATION, contextAttribute);
    }
}
