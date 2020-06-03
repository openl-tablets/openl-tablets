package org.openl.rules.webstudio.web.test;

import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.rules.ui.Explanator;
import org.richfaces.model.TreeNode;
import org.richfaces.model.TreeNodeImpl;
import org.springframework.stereotype.Controller;

/**
 * A helper class which contains utility methods.
 */
@Controller
public final class Helper {

    public Helper() {
        // THIS CONSTRUCTOR MUST BE EMPTY!!!
    }

    public TreeNode getRoot(ParameterDeclarationTreeNode parameter) {
        if (parameter == null) {
            return null;
        }
        TreeNodeImpl root = new TreeNodeImpl();
        root.addChild(parameter.getName(), parameter);
        return root;
    }

    public String format(Object value) {
        return FormattersManager.format(value);
    }

    public String formatText(Object value) {
        if (value instanceof Number) {
            return String.valueOf(value);
        } else {
            return format(value);
        }
    }

    public boolean isExplanationValue(Object value) {
        return value instanceof ExplanationNumberValue<?>;
    }

    public boolean isSpreadsheetResult(Object value) {
        return value instanceof SpreadsheetResult;
    }

    public int getExplanatorId(String requestId, Object actualResult) {
        // We expect there ExplanationNumberValue.
        return Explanator.getUniqueId(requestId, (ExplanationNumberValue<?>) actualResult);
    }
}
