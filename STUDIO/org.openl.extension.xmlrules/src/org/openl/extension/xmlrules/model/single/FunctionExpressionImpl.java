package org.openl.extension.xmlrules.model.single;

import javax.xml.bind.annotation.XmlType;

import org.openl.extension.xmlrules.model.FunctionExpression;

@XmlType(name = "function-expression")
public class FunctionExpressionImpl implements FunctionExpression {
    private String stepName;
    private String stepType;
    private String expression;

    @Override
    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    @Override
    public String getStepType() {
        return stepType;
    }

    public void setStepType(String stepType) {
        this.stepType = stepType;
    }

    @Override
    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }
}
