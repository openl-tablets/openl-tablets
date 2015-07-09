package org.openl.extension.xmlrules.model;

import java.util.List;

public interface Function {
    String getName();

    List<Parameter> getParameters();

    String getReturnType();

    List<FunctionExpression> getExpressions();
}
