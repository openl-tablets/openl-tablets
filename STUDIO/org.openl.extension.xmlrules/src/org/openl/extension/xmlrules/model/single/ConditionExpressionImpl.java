package org.openl.extension.xmlrules.model.single;

import org.openl.extension.xmlrules.model.ConditionExpression;

public class ConditionExpressionImpl implements ConditionExpression {
    private String expression;
    private XlsRegionImpl region;

    @Override
    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    @Override
    public XlsRegionImpl getRegion() {
        return region;
    }

    public void setRegion(XlsRegionImpl region) {
        this.region = region;
    }
}
