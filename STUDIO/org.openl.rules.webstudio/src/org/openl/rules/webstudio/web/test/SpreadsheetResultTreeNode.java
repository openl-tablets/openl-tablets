package org.openl.rules.webstudio.web.test;

import java.util.LinkedHashMap;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.ui.ObjectViewer;
import org.openl.types.IOpenClass;

public class SpreadsheetResultTreeNode extends ParameterDeclarationTreeNode {
    public static final String SPREADSHEET_RESULT_TYPE = "spreadsheet";
    private final boolean hasExplainLinks;
    private final String requestId;

    public SpreadsheetResultTreeNode(String fieldName, Object value, IOpenClass fieldType,
            ParameterDeclarationTreeNode parent, boolean hasExplainLinks, String requestId) {
        super(fieldName, value, fieldType, parent);
        this.hasExplainLinks = hasExplainLinks;
        this.requestId = requestId;
    }

    @Override
    public String getDisplayedValue() {
        // TODO Refactor code and don't use deprecated class ObjectViewer.
        // TODO Instead render a table using jsf components.
        SpreadsheetResult value = (SpreadsheetResult) getValue();
        return hasExplainLinks ? ObjectViewer.displaySpreadsheetResult(value, requestId) : ObjectViewer.displaySpreadsheetResultNoFilters(value);
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
        return new LinkedHashMap<>();
    }
}
