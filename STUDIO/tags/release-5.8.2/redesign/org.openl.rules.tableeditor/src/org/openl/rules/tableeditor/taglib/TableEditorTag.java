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
    private MethodExpression saveFailureAction = null;
    private ValueExpression onBeforeSave = null;
    private ValueExpression onAfterSave = null;
    private ValueExpression onSaveFailure = null;

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
    
    public MethodExpression getSaveFailureAction() {
        return saveFailureAction;
    }

    public void setSaveFailureAction(MethodExpression saveFailureAction) {
        this.saveFailureAction = saveFailureAction;
    }

    public void setOnBeforeSave(ValueExpression onBeforeSave) {
        this.onBeforeSave = onBeforeSave;
    }

    public void setOnAfterSave(ValueExpression onAfterSave) {
        this.onAfterSave = onAfterSave;
    }
    
    public ValueExpression getOnSaveFailure() {
        return onSaveFailure;
    }

    public void setOnSaveFailure(ValueExpression onSaveFailure) {
        this.onSaveFailure = onSaveFailure;
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
        component.getAttributes().put(Constants.ATTRIBUTE_SAVE_FAILURE_ACTION, saveFailureAction);
        component.setValueExpression(Constants.ATTRIBUTE_ON_BEFORE_SAVE, onBeforeSave);
        component.setValueExpression(Constants.ATTRIBUTE_ON_AFTER_SAVE, onAfterSave);
        component.setValueExpression(Constants.ATTRIBUTE_ON_SAVE_FAILURE, onSaveFailure);
    }

    @Override
    public void release() {
        // always call the superclass method
        super.release();
        mode = null;
        editable = null;
        beforeSaveAction = null;
        saveFailureAction = null;
        afterSaveAction = null;
    }

}
