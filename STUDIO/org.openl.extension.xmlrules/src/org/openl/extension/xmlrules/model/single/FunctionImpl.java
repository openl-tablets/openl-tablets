package org.openl.extension.xmlrules.model.single;

import java.util.ArrayList;
import java.util.List;

import org.openl.extension.xmlrules.model.Function;
import org.openl.extension.xmlrules.model.FunctionExpression;

public class FunctionImpl implements Function {
    private String name;
    private List<String> parameters = new ArrayList<String>();
    private List<FunctionExpression> expressions = new ArrayList<FunctionExpression>();
    private XlsRegionImpl region;

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public List<String> getParameters() {
        return parameters;
    }

    public void setParameters(List<String> parameters) {
        this.parameters = parameters;
    }

    @Override
    public List<FunctionExpression> getExpressions() {
        return expressions;
    }

    public void setExpressions(List<FunctionExpression> expressions) {
        this.expressions = expressions;
    }

    @Override
    public XlsRegionImpl getRegion() {
        return region;
    }

    public void setRegion(XlsRegionImpl region) {
        this.region = region;
    }
}
