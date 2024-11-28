package org.openl.rules.table.properties.expressions.match;

public interface IMatchingExpression {

    String getOperationName();

    String getCodeExpression(String param);

    String getContextAttribute();

    IMatchingExpression getContextAttributeExpression();

    boolean isContextAttributeExpression();

}
