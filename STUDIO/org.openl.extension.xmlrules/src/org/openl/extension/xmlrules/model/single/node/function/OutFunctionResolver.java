package org.openl.extension.xmlrules.model.single.node.function;

import java.util.List;

import org.openl.extension.xmlrules.model.single.node.FunctionNode;
import org.openl.extension.xmlrules.model.single.node.Node;
import org.openl.extension.xmlrules.model.single.node.expression.ExpressionContext;

public class OutFunctionResolver implements FunctionResolver {
    @Override
    public String resolve(FunctionNode node) {
        StringBuilder builder = new StringBuilder();
        builder.append("Out(");

        ExpressionContext context = ExpressionContext.getInstance();
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
