package org.openl.rules.tableeditor.taglib;

import java.util.List;
import jakarta.el.MethodExpression;
import jakarta.el.ValueExpression;
import jakarta.faces.component.UIComponentBase;

import org.openl.rules.table.ICell;
import org.openl.rules.tableeditor.util.Constants;

public class TableEditorTag extends UIComponentBase {

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
    private List<ICell> modifiedCells = null;

    @Override
    public String getFamily() {
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

    public List<ICell> getModifiedCells() {
        return modifiedCells;
    }

    public void setModifiedCells(List<ICell> modifiedCells) {
        this.modifiedCells = modifiedCells;
    }

    @Override
    public String getRendererType() {
        return Constants.TABLE_EDITOR_TYPE;
    }

}
