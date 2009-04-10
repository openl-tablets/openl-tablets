package org.openl.rules.tableeditor.taglib;

import javax.faces.component.UIComponent;

import org.openl.rules.tableeditor.util.Constants;

public class TableViewerTag extends BaseTag  {

    private Object table = null;

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

    @Override
    public void setProperties(UIComponent component) {
        // always call the superclass method
        super.setProperties(component);
        setObject(component, Constants.ATTRIBUTE_TABLE, table);
    }

    @Override
    public void release() {
        // always call the superclass method
        super.release();
        table = null;
    }
}