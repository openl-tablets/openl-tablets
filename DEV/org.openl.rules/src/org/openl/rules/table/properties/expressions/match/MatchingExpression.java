package org.openl.rules.table.properties.expressions.match;

import lombok.Getter;

public class MatchingExpression {

    @Getter
    private final String matchExpressionStr;

    @Getter
    private final IMatchingExpression matchExpression;

    public MatchingExpression(String matchExpressionStr) {
        this.matchExpressionStr = matchExpressionStr;
        this.matchExpression = MatchingExpressionsParser.parse(matchExpressionStr);
    }

}
