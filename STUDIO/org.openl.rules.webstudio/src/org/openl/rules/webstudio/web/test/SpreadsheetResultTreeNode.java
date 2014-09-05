package org.openl.rules.webstudio.web.test;

import java.util.LinkedHashMap;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.ui.ObjectViewer;
import org.openl.types.IOpenClass;

public class SpreadsheetResultTreeNode extends ParameterDeclarationTreeNode {
    public static final String SPREADSHEET_RESULT_TYPE = "spreadsheet";
    private final boolean hasExplainLinks;

    public SpreadsheetResultTreeNode(String fieldName, Object value, IOpenClass fieldType,
                                     ParameterDeclarationTreeNode parent, boolean hasExplainLinks) {
        super(fieldName, value, fieldType, parent);
        this.hasExplainLinks = hasExplainLinks;
    }

    @Override
    public String getDisplayedValue() {
        // TODO Refactor code and don't use deprecated class ObjectViewer.
        // TODO Instead render a table using jsf components.
        SpreadsheetResult value = (SpreadsheetResult) getValue();
        return hasExplainLinks ? ObjectViewer.displaySpreadsheetResult(value) : ObjectViewer.displaySpreadsheetResultNoFilters(value);
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
    protected LinkedHashMap<Object, ParameterDeclarationTreeNode> initChildrenMap() {
        return new LinkedHashMap<Object, ParameterDeclarationTreeNode>();
    }
}
