package org.openl.rules.tableeditor.taglib;

import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.faces.component.UIComponent;

import org.openl.rules.tableeditor.util.Constants;

public class TableEditorTag extends TableViewerTag {

    private ValueExpression mode = null;
    private ValueExpression editable = null;
    private MethodExpression beforeSaveAction = null;
    private MethodExpression afterSaveAction = null;

    @Override
    public String getComponentType() {
        return Constants.TABLE_EDITOR_TYPE;
    }

    public void setMode(ValueExpression mode) {
        this.mode = mode;
    }

    public void setEditable(ValueExpression editable) {
        this.editable = editable;
    }

    public void setBeforeSaveAction(MethodExpression beforeSaveAction) {
        this.beforeSaveAction = beforeSaveAction;
    }

    public void setAfterSaveAction(MethodExpression afterSaveAction) {
        this.afterSaveAction = afterSaveAction;
    }

    @Override
    public String getRendererType() {
        return Constants.TABLE_EDITOR_TYPE;
    }

    @Override
    public void setProperties(UIComponent component) {
        // always call the superclass method
        super.setProperties(component);
        component.setValueExpression(Constants.ATTRIBUTE_MODE, mode);
        component.setValueExpression(Constants.ATTRIBUTE_EDITABLE, editable);
        component.getAttributes().put(Constants.ATTRIBUTE_BEFORE_SAVE_ACTION, beforeSaveAction);
        component.getAttributes().put(Constants.ATTRIBUTE_AFTER_SAVE_ACTION, afterSaveAction);
    }

    @Override
    public void release() {
        // always call the superclass method
        super.release();
        mode = null;
        editable = null;
        beforeSaveAction = null;
        afterSaveAction = null;
    }

}