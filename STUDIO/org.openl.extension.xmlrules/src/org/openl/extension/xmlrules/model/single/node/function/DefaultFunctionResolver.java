package org.openl.extension.xmlrules.model.single.node.function;

import java.util.List;

import org.openl.extension.xmlrules.ProjectData;
import org.openl.extension.xmlrules.model.Function;
import org.openl.extension.xmlrules.model.single.ParameterImpl;
import org.openl.extension.xmlrules.model.single.node.FunctionNode;
import org.openl.extension.xmlrules.model.single.node.Node;
import org.openl.extension.xmlrules.model.single.node.expression.ExpressionContext;

public class DefaultFunctionResolver implements FunctionResolver {
    @Override
    public String resolve(FunctionNode node) {
        ExpressionContext instance = ExpressionContext.getInstance();
        boolean canHandleArrayOperators = instance.isCanHandleArrayOperators();
        try {
            instance.setCanHandleArrayOperators(true);

            List<ParameterImpl> parameters = getParameters(node);

            StringBuilder builder = new StringBuilder(node.getName());
            builder.append('(');
            List<Node> arguments = node.getArguments();
            for (int i = 0; i < arguments.size(); i++) {
                if (i > 0) {
                    builder.append(", ");
                }

                String argumentString = getArgumentString(parameters, arguments, i);
                builder.append(argumentString);
            }
            builder.append(')');
            return builder.toString();
        } finally {
            instance.setCanHandleArrayOperators(canHandleArrayOperators);
        }

    }

    protected List<ParameterImpl> getParameters(FunctionNode node) {
        ProjectData projectData = ProjectData.getCurrentInstance();

        for (Function function : projectData.getFunctions()) {
            if (node.getName().equals(function.getName())) {
                return function.getParameters();
            }
        }

        return null;
    }

    protected String getArgumentString(List<ParameterImpl> parameters, List<Node> arguments, int i) {
        Node argument = arguments.get(i);

        String argumentString = argument.toOpenLString();

        if (parameters != null && parameters.size() > i) {
            ParameterImpl parameter = parameters.get(i);
            if (argument instanceof FunctionNode && ((FunctionNode) argument).getName().equals("Out")) {
                if (parameter.getType() != null && !parameter.getType().endsWith("]")) {
                    argumentString += "[0][0]";
                }
            }
            if (parameter.getType() != null) {
                argumentString = "(" + parameter.getType() + ")(" + argumentString + ")";
            }
        }
        return argumentString;
    }
}
