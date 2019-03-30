package org.openl.rules.table.properties.expressions.match;

public class MatchingExpression {

    private String matchExpressionStr;

    private IMatchingExpression matchExpression;

    public MatchingExpression() {
    }

    public MatchingExpression(String matchExpressionStr) {
        this.matchExpressionStr = matchExpressionStr;
        this.matchExpression = MatchingExpressionsParser.parse(matchExpressionStr);
    }

    public String getMatchExpressionStr() {
        return matchExpressionStr;
    }

    public void setMatchExpressionStr(String matchExpressionStr) {
        this.matchExpressionStr = matchExpressionStr;
        this.matchExpression = MatchingExpressionsParser.parse(matchExpressionStr);
    }

    public IMatchingExpression getMatchExpression() {
        return matchExpression;
    }

    public void setMatchExpression(IMatchingExpression matchExpression) {
        this.matchExpression = matchExpression;
    }

}
