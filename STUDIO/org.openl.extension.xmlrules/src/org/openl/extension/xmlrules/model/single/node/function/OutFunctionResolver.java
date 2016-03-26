package org.openl.extension.xmlrules.model.single.node.function;

import java.util.List;

import org.openl.extension.xmlrules.model.single.node.BooleanNode;
import org.openl.extension.xmlrules.model.single.node.FunctionNode;
import org.openl.extension.xmlrules.model.single.node.Node;
import org.openl.extension.xmlrules.model.single.node.expression.ExpressionContext;

public class OutFunctionResolver implements FunctionResolver {
    @Override
    public String resolve(FunctionNode node) {
        StringBuilder builder = new StringBuilder();

        ExpressionContext context = ExpressionContext.getInstance();

        boolean rootNode = node.isRootNode();
        if (rootNode) {
            builder.append("Print(");
            if (context.isArrayExpression()) {
                int rowShift = context.getCurrentRow() - context.getStartRow();
                int columnShift = context.getCurrentColumn() - context.getStartColumn();

                builder.append(rowShift);
                builder.append(", ");
                builder.append(columnShift);
                builder.append(", ");
            } else {
                builder.append(0);
                builder.append(", ");
                builder.append(0);
                builder.append(", ");
            }
        } else {
            builder.append("Out(");
        }

        List<Node> arguments = node.getArguments();
        for (int i = 0; i < arguments.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            if (context.isOutArray() && i == 0) {
                builder.append("$Calculation$Result");
            } else {
                Node argNode = arguments.get(i);
                String argString = argNode.toOpenLString();
                if (i > 0 && !(argNode instanceof BooleanNode)) {
                    argString = "Boolean.valueOf((String) " + argString + ")";
                }
                builder.append(argString);
            }
        }
        builder.append(')');
        return builder.toString();
    }
}
