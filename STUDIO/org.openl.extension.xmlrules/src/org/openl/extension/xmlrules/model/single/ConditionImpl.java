package org.openl.extension.xmlrules.model.single;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

import org.openl.extension.xmlrules.model.Condition;

@XmlType(name = "condition")
public class ConditionImpl implements Condition {
    private Integer parameterIndex;
    private List<ExpressionImpl> expressions = new ArrayList<ExpressionImpl>();

    @XmlElement(name = "parameter-index", required = true)
    public Integer getParameterIndex() {
        return parameterIndex;
    }

    public void setParameterIndex(Integer parameterIndex) {
        this.parameterIndex = parameterIndex;
    }

    @XmlElementWrapper(name="expressions", required = true)
    @XmlElement(name = "expression", required = true)
    @Override
    public List<ExpressionImpl> getExpressions() {
        return expressions;
    }

    public void setExpressions(List<ExpressionImpl> expressions) {
        this.expressions = expressions;
    }

}
