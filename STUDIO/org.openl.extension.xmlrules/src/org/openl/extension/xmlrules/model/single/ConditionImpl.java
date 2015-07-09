package org.openl.extension.xmlrules.model.single;

import java.util.ArrayList;
import java.util.List;

import org.openl.extension.xmlrules.model.Condition;
import org.openl.extension.xmlrules.model.Expression;

public class ConditionImpl implements Condition {
    private List<Expression> expressions = new ArrayList<Expression>();

    @Override
    public List<Expression> getExpressions() {
        return expressions;
    }

    public void setExpressions(List<Expression> expressions) {
        this.expressions = expressions;
    }

}
