package org.openl.rules.tableeditor.taglib;

import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.faces.component.UIComponent;

import org.openl.rules.tableeditor.util.Constants;

public class TableEditorTag extends TableViewerTag {

    private ValueExpression mode = null;
    private ValueExpression editable = null;
    private MethodExpression beforeEditAction = null;
    private MethodExpression beforeSaveAction = null;
    private MethodExpression afterSaveAction = null;
    private ValueExpression onBeforeEdit = null;
    private ValueExpression onBeforeSave = null;
    private ValueExpression onAfterSave = null;
    private ValueExpression onError = null;
    private ValueExpression onRequestStart = null;
    private ValueExpression onRequestEnd = null;

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

    public void setBeforeEditAction(MethodExpression beforeEditAction) {
        this.beforeEditAction = beforeEditAction;
    }

    public void setBeforeSaveAction(MethodExpression beforeSaveAction) {
        this.beforeSaveAction = beforeSaveAction;
    }

    public void setAfterSaveAction(MethodExpression afterSaveAction) {
        this.afterSaveAction = afterSaveAction;
    }

    public void setOnBeforeEdit(ValueExpression onBeforeEdit) {
        this.onBeforeEdit = onBeforeEdit;
    }

    public void setOnBeforeSave(ValueExpression onBeforeSave) {
        this.onBeforeSave = onBeforeSave;
    }

    public void setOnAfterSave(ValueExpression onAfterSave) {
        this.onAfterSave = onAfterSave;
    }

    public ValueExpression getOnError() {
        return onError;
    }

    public void setOnError(ValueExpression onError) {
        this.onError = onError;
    }

    public void setOnRequestStart(ValueExpression onRequestStart) {
        this.onRequestStart = onRequestStart;
    }

    public void setOnRequestEnd(ValueExpression onRequestEnd) {
        this.onRequestEnd = onRequestEnd;
    }

    @Override
    public String getRendererType() {
        return Constants.TABLE_EDITOR_TYPE;
    }

    @Override
    public void setProperties(UIComponent component) {
        // Always call the superclass method
        super.setProperties(component);
        component.setValueExpression(Constants.ATTRIBUTE_MODE, mode);
        component.setValueExpression(Constants.ATTRIBUTE_EDITABLE, editable);
        component.getAttributes().put(Constants.ATTRIBUTE_BEFORE_EDIT_ACTION, beforeEditAction);
        component.getAttributes().put(Constants.ATTRIBUTE_BEFORE_SAVE_ACTION, beforeSaveAction);
        component.getAttributes().put(Constants.ATTRIBUTE_AFTER_SAVE_ACTION, afterSaveAction);
        component.setValueExpression(Constants.ATTRIBUTE_ON_BEFORE_EDIT, onBeforeEdit);
        component.setValueExpression(Constants.ATTRIBUTE_ON_BEFORE_SAVE, onBeforeSave);
        component.setValueExpression(Constants.ATTRIBUTE_ON_AFTER_SAVE, onAfterSave);
        component.setValueExpression(Constants.ATTRIBUTE_ON_ERROR, onError);
        component.setValueExpression(Constants.ATTRIBUTE_ON_REQUEST_START, onRequestStart);
        component.setValueExpression(Constants.ATTRIBUTE_ON_REQUEST_END, onRequestEnd);
    }

    @Override
    public void release() {
        // always call the superclass method
        super.release();
        mode = null;
        editable = null;
        beforeEditAction = null;
        beforeSaveAction = null;
        afterSaveAction = null;
    }

}
