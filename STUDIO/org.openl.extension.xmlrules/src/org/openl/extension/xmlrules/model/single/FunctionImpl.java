package org.openl.extension.xmlrules.model.single;

import java.util.ArrayList;
import java.util.List;

import org.openl.extension.xmlrules.model.Function;
import org.openl.extension.xmlrules.model.FunctionExpression;
import org.openl.extension.xmlrules.model.Parameter;

public class FunctionImpl implements Function {
    private String name;
    private String returnType;
    private List<Parameter> parameters = new ArrayList<Parameter>();
    private List<FunctionExpression> expressions = new ArrayList<FunctionExpression>();

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    @Override
    public String getReturnType() {
        return returnType;
    }

    @Override
    public List<Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    @Override
    public List<FunctionExpression> getExpressions() {
        return expressions;
    }

    public void setExpressions(List<FunctionExpression> expressions) {
        this.expressions = expressions;
    }

}
