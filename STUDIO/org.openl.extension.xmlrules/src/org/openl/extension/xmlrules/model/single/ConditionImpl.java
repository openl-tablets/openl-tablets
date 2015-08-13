package org.openl.extension.xmlrules.model.single;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

import org.openl.extension.xmlrules.model.Condition;
import org.openl.extension.xmlrules.model.Expression;

@XmlType(name = "condition")
public class ConditionImpl implements Condition {
    private List<ExpressionImpl> expressions = new ArrayList<ExpressionImpl>();

    @XmlElementWrapper(name="expressions", required = true)
    @XmlElement(name = "expression")
    @Override
    public List<ExpressionImpl> getExpressions() {
        return expressions;
    }

    public void setExpressions(List<ExpressionImpl> expressions) {
        this.expressions = expressions;
    }

}
