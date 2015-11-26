package org.openl.extension.xmlrules.model.single.node.function;

import org.openl.extension.xmlrules.model.single.node.FunctionNode;

public interface FunctionResolver {
    String resolve(FunctionNode node);
}
