package org.openl.rules.table.properties.expressions.match;

import org.openl.exception.OpenLRuntimeException;
import org.openl.util.StringUtils;

public class MatchingExpressionFactory {

    private MatchingExpressionFactory() {
    }

    public static IMatchingExpression getMatchingExpression(String operationName, String contextAttribute) {
        IMatchingExpression matchExpression = null;
        operationName = operationName == null ? "" : operationName;

        if (StringUtils.isEmpty(contextAttribute)) {
            throw new OpenLRuntimeException("Cannot create matching expression with empty context attribute");
        }

        if (LTMatchingExpression.OPERATION_NAME.equalsIgnoreCase(operationName)) {
            matchExpression = new LTMatchingExpression(contextAttribute);
        } else if (LEMatchingExpression.OPERATION_NAME.equalsIgnoreCase(operationName)) {
            matchExpression = new LEMatchingExpression(contextAttribute);
        } else if (GTMatchingExpression.OPERATION_NAME.equalsIgnoreCase(operationName)) {
            matchExpression = new GTMatchingExpression(contextAttribute);
        } else if (GEMatchingExpression.OPERATION_NAME.equalsIgnoreCase(operationName)) {
            matchExpression = new GEMatchingExpression(contextAttribute);
        } else if (EQMatchingExpression.OPERATION_NAME.equalsIgnoreCase(operationName)) {
            matchExpression = new EQMatchingExpression(contextAttribute);
        } else if (ContainsMatchingExpression.OPERATION_NAME.equalsIgnoreCase(operationName)) {
            matchExpression = new ContainsMatchingExpression(contextAttribute);
        } else {
            throw new OpenLRuntimeException(String.format("Unknown match expression operation '%s'", operationName));
        }
        return matchExpression;
    }

}
