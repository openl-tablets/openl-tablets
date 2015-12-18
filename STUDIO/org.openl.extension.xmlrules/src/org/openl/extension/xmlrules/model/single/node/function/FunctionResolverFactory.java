package org.openl.extension.xmlrules.model.single.node.function;

import org.openl.extension.xmlrules.model.single.node.FunctionNode;

public final class FunctionResolverFactory {
    private FunctionResolverFactory() {
    }

    public static FunctionResolver getResolver(FunctionNode node) {
        String functionName = node.getName();
        if (functionName.equals("Out")) {
            return new OutFunctionResolver();
        }

        return new DefaultFunctionResolver();
    }
}
