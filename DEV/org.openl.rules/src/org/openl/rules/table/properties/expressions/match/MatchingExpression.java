package org.openl.rules.table.properties.expressions.match;

import lombok.Getter;
import lombok.Setter;

public class MatchingExpression {

    @Getter
    private String matchExpressionStr;

    @Getter
    @Setter
    private IMatchingExpression matchExpression;

    public MatchingExpression() {
    }

    public MatchingExpression(String matchExpressionStr) {
        this.matchExpressionStr = matchExpressionStr;
        this.matchExpression = MatchingExpressionsParser.parse(matchExpressionStr);
    }

    public void setMatchExpressionStr(String matchExpressionStr) {
        this.matchExpressionStr = matchExpressionStr;
        this.matchExpression = MatchingExpressionsParser.parse(matchExpressionStr);
    }

}
