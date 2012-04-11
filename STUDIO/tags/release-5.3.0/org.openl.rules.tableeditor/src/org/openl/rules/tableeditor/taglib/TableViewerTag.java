package org.openl.rules.tableeditor.taglib;

import javax.faces.component.UIComponent;

import org.openl.rules.tableeditor.util.Constants;

public class TableViewerTag extends BaseTag {

    private Object table = null;
    private String view = null;
    private Object filter = null;
    private boolean showFormulas = false;
    private boolean collapseProps = false;

    @Override
    public String getComponentType() {
        return Constants.TABLE_VIEWER_TYPE;
    }

    @Override
    public String getRendererType() {
        return Constants.TABLE_VIEWER_TYPE;
    }

    public Object getTable() {
        return table;
    }

    public void setTable(Object table) {
        this.table = table;
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }

    public Object getFilter() {
        return filter;
    }

    public void setFilter(Object filter) {
        this.filter = filter;
    }

    public boolean isShowFormulas() {
        return showFormulas;
    }

    public void setShowFormulas(boolean showFormulas) {
        this.showFormulas = showFormulas;
    }

    public boolean isCollapseProps() {
        return collapseProps;
    }

    public void setCollapseProps(boolean collapseProps) {
        this.collapseProps = collapseProps;
    }

    @Override
    public void release() {
        // always call the superclass method
        super.release();
        table = null;
        filter = null;
    }

    @Override
    public void setProperties(UIComponent component) {
        // always call the superclass method
        super.setProperties(component);
        setObject(component, Constants.ATTRIBUTE_TABLE, table);
        setObject(component, Constants.ATTRIBUTE_VIEW, view);
        setObject(component, Constants.ATTRIBUTE_FILTER, filter);
        setObject(component, Constants.ATTRIBUTE_SHOW_FORMULAS, showFormulas);
        setObject(component, Constants.ATTRIBUTE_SHOW_FORMULAS, collapseProps);
    }


}