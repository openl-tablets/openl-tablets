package org.openl.rules.webstudio.web.test;

import java.util.LinkedHashMap;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.ui.ObjectViewer;
import org.openl.types.IOpenClass;

public class SpreadsheetResultTreeNode extends ParameterDeclarationTreeNode {
    public static final String SPREADSHEET_RESULT_TYPE = "spreadsheet";

    public SpreadsheetResultTreeNode(String fieldName, Object value, IOpenClass fieldType,
            ParameterDeclarationTreeNode parent) {
        super(fieldName, value, fieldType, parent);
    }

    @Override
    public String getDisplayedValue() {
        // TODO Refactor code and don't use deprecated class ObjectViewer.
        // TODO Instead render a table using jsf components.
        return new ObjectViewer().displaySpreadsheetResult((SpreadsheetResult) getValue());
    }

    @Override
    protected Object constructValueInternal() {
        return getValue();
    }

    @Override
    public String getNodeType() {
        return SPREADSHEET_RESULT_TYPE;
    }

    @Override
    protected LinkedHashMap<Object, ParameterDeclarationTreeNode> initChildernMap() {
        return new LinkedHashMap<Object, ParameterDeclarationTreeNode>();
    }
}
