package org.openl.rules.webstudio.web.test;

import java.util.LinkedHashMap;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.ui.ObjectViewer;

public class SpreadsheetResultTreeNode extends ParameterDeclarationTreeNode {
    private static final String SPREADSHEET_RESULT_TYPE = "spreadsheet";
    private final ParameterRenderConfig config;

    public SpreadsheetResultTreeNode(ParameterRenderConfig config) {
        super(config.getFieldNameInParent(), config.getValue(), config.getType(), config.getParent());
        this.config = config;
    }

    @Override
    public String getDisplayedValue() {
        // TODO Refactor code and don't use deprecated class ObjectViewer.
        // TODO Instead render a table using jsf components.
        SpreadsheetResult value = (SpreadsheetResult) getValue();
        return config.isHasExplainLinks() ? ObjectViewer.displaySpreadsheetResult(value, config.getRequestId())
                                          : ObjectViewer.displaySpreadsheetResultNoFilters(value);
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
