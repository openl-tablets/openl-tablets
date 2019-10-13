package org.openl.rules.table.properties.expressions.match;

import org.openl.exception.OpenLRuntimeException;
import org.openl.util.StringUtils;

public class MatchingExpressionsParser {

    private MatchingExpressionsParser() {
    }

    public static IMatchingExpression parse(String matchingExpressionStr) {

        String operationName = null;
        String contextAttribute = null;
        if (StringUtils.isNotEmpty(matchingExpressionStr)) {
            int openBracketIndex = matchingExpressionStr.indexOf("(");
            int closeBracketIndex = matchingExpressionStr.lastIndexOf(")");

            if (openBracketIndex < 0 || closeBracketIndex < 0) {
                throw new OpenLRuntimeException("Matching expression string is not valid.");
            }

            operationName = matchingExpressionStr.substring(0, openBracketIndex).toUpperCase();
            contextAttribute = matchingExpressionStr.substring(openBracketIndex + 1, closeBracketIndex);
        } else {
            throw new OpenLRuntimeException("Matching expression string is null.");
        }

        if (StringUtils.isEmpty(operationName) || StringUtils.isEmpty(contextAttribute)) {
            throw new OpenLRuntimeException(
                "Wrong matching expression format. Expected: <operationName>(<contextAttribute>) or MAX|MIN(LE|GE(<contextAttribute>))");
        }

        return MatchingExpressionFactory.getMatchingExpression(operationName, contextAttribute);
    }

}
