package org.openl.rules.tableeditor.taglib;

import javax.el.ValueExpression;
import javax.faces.component.UIComponent;

import org.openl.rules.tableeditor.util.Constants;

public class TableViewerTag extends BaseTag {

    private ValueExpression table = null;
    private ValueExpression filter = null;
    private ValueExpression view = null;
    private ValueExpression showFormulas = null;
    private ValueExpression collapseProps = null;
    private ValueExpression excludeScripts = null;

    @Override
    public String getComponentType() {
        return Constants.TABLE_VIEWER_TYPE;
    }

    @Override
    public String getRendererType() {
        return Constants.TABLE_VIEWER_TYPE;
    }

    public void setTable(ValueExpression table) {
        this.table = table;
    }

    public void setFilter(ValueExpression filter) {
        this.filter = filter;
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

    @Override
    public void setProperties(UIComponent component) {
        // always call the superclass method
        super.setProperties(component);
        component.setValueExpression(Constants.ATTRIBUTE_TABLE, table);
        component.setValueExpression(Constants.ATTRIBUTE_VIEW, view);
        component.setValueExpression(Constants.ATTRIBUTE_FILTER, filter);
        component.setValueExpression(Constants.ATTRIBUTE_SHOW_FORMULAS, showFormulas);
        component.setValueExpression(Constants.ATTRIBUTE_COLLAPSE_PROPS, collapseProps);
        component.setValueExpression(Constants.ATTRIBUTE_EXCLUDE_SCRIPTS, excludeScripts);
    }

    @Override
    public void release() {
        // always call the superclass method
        super.release();
        table = null;
        filter = null;
        view = null;
        showFormulas = null;
        collapseProps = null;
        excludeScripts = null;
    }

}