package org.openl.extension.xmlrules.model.single.node.function;

import java.util.List;

import org.openl.extension.xmlrules.model.single.node.FunctionNode;
import org.openl.extension.xmlrules.model.single.node.Node;

public class DefaultFunctionResolver implements FunctionResolver {
    @Override
    public String resolve(FunctionNode node) {
        StringBuilder builder = new StringBuilder(node.getName());
        builder.append('(');
        List<Node> arguments = node.getArguments();
        for (int i = 0; i < arguments.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(arguments.get(i).toOpenLString());
        }
        builder.append(')');
        return builder.toString();

    }
}
