package org.openl.rules.tableeditor.taglib;

import jakarta.el.ValueExpression;
import jakarta.faces.component.UIComponentBase;

import org.openl.rules.tableeditor.util.Constants;

public class TableViewerTag extends UIComponentBase {

    private ValueExpression table = null;
    private ValueExpression filters = null;
    private ValueExpression modifiedCells = null;
    private ValueExpression view = null;
    private ValueExpression showFormulas = null;
    private ValueExpression collapseProps = null;
    private ValueExpression excludeScripts = null;

    @Override
    public String getFamily() {
        return Constants.TABLE_VIEWER_TYPE;
    }

    @Override
    public String getRendererType() {
        return Constants.TABLE_VIEWER_TYPE;
    }

    public void setTable(ValueExpression table) {
        this.table = table;
    }

    public void setFilters(ValueExpression filters) {
        this.filters = filters;
    }

    public void setModifiedCells(ValueExpression modifiedCells) {
        this.modifiedCells = modifiedCells;
    }

    public void setView(ValueExpression view) {
        this.view = view;
    }

    public void setShowFormulas(ValueExpression showFormulas) {
        this.showFormulas = showFormulas;
    }

    public void setCollapseProps(ValueExpression collapseProps) {
        this.collapseProps = collapseProps;
    }

    public void setExcludeScripts(ValueExpression excludeScripts) {
        this.excludeScripts = excludeScripts;
    }

}