package org.openl.extension.xmlrules.model;

import java.util.List;

public interface Function {
    String getName();

    List<String> getParameters();

    List<FunctionExpression> getExpressions();

    XlsRegion getRegion();
}
