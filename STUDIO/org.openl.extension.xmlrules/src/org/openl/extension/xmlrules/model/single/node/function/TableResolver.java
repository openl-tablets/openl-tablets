package org.openl.extension.xmlrules.model.single.node.function;

import java.util.List;

import org.openl.extension.xmlrules.ProjectData;
import org.openl.extension.xmlrules.model.Table;
import org.openl.extension.xmlrules.model.single.ParameterImpl;
import org.openl.extension.xmlrules.model.single.node.FunctionNode;
import org.openl.extension.xmlrules.model.single.node.Node;

public class TableResolver extends DefaultFunctionResolver {
    @Override
    protected List<ParameterImpl> getParameters(FunctionNode node) {
        ProjectData projectData = ProjectData.getCurrentInstance();

        for (Table table : projectData.getTables()) {
            if (node.getName().equals(table.getName())) {
                return table.getParameters();
            }
        }

        return null;
    }

    @Override
    protected String getArgumentString(List<ParameterImpl> parameters, List<Node> arguments, int i) {
        Node argument = arguments.get(i);

        String argumentString = argument.toOpenLString();

        if (parameters != null) {
            ParameterImpl parameter = parameters.get(i);
            if (argument instanceof FunctionNode && ((FunctionNode) argument).getName().equals("Out")) {
                argumentString += "[0][0]";
            }
            String parameterType = parameter.getType();
            if (parameterType == null) {
                parameterType = "String";
            }

            argumentString = "(" + parameterType + ")(" + argumentString + ")";
        }
        return argumentString;
    }
}
