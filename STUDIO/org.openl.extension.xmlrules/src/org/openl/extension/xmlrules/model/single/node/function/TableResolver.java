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

        Table table = projectData.getTable(node.getName());
        if (table != null) {
            return table.getParameters();
        }

        return null;
    }

    @Override
    protected String getArgumentString(List<ParameterImpl> parameters, List<Node> arguments, int i) {
        Node argument = arguments.get(i);

        String argumentString = argument.toOpenLString();

        if (parameters != null && parameters.size() > i) {
            if (argument instanceof FunctionNode && ((FunctionNode) argument).getName().equals("Out")) {
                argumentString += "[0][0]";
            }
        }
        return argumentString;
    }
}
