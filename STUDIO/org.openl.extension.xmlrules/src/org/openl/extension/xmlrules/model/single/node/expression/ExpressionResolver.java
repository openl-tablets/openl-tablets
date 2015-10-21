package org.openl.extension.xmlrules.model.single.node.expression;

import org.openl.extension.xmlrules.model.single.node.Node;

public interface ExpressionResolver {
    String resolve(Node leftNode, Node rightNode, Operator operator);
}
