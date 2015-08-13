package org.openl.extension.xmlrules.model;

import java.util.List;

import org.openl.extension.xmlrules.model.single.ExpressionImpl;

public interface Condition {
    List<ExpressionImpl> getExpressions();
}
