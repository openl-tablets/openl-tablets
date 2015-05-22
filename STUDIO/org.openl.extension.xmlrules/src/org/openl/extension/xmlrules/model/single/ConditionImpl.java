package org.openl.extension.xmlrules.model.single;

import java.util.ArrayList;
import java.util.List;

import org.openl.extension.xmlrules.model.Condition;
import org.openl.extension.xmlrules.model.ConditionExpression;

public class ConditionImpl implements Condition {
    private List<ConditionExpression> expressions = new ArrayList<ConditionExpression>();

    @Override
    public List<ConditionExpression> getExpressions() {
        return expressions;
    }

    public void setExpressions(List<ConditionExpression> expressions) {
        this.expressions = expressions;
    }

}
