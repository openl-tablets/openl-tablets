package org.openl.rules.tableeditor.taglib;

import javax.faces.component.UIComponent;

import org.openl.rules.tableeditor.util.Constants;

public class TableEditorTag extends TableViewerTag  {
    
    private String readonly = null;
    
    @Override
    public String getComponentType() {
        return Constants.TABLE_EDITOR_TYPE;
    }

    @Override
    public String getRendererType() {
        return Constants.TABLE_EDITOR_TYPE;
    }
    
    public String getReadonly() {
        return readonly;
    }

    public void setReadonly(String readonly) {
        this.readonly = readonly;
    }

    @Override
    public void setProperties(UIComponent component) {
        // always call the superclass method
        super.setProperties(component);
        setBoolean(component, "readonly", readonly);
    }

    @Override
    public void release() {
        // always call the superclass method
        super.release();
        readonly = null;
    }
}