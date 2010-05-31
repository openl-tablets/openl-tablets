package org.openl.rules.tableeditor.renderkit;

import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.table.ITable;
import org.openl.rules.table.ui.filters.IGridFilter;
import org.openl.rules.tableeditor.util.Constants;
import org.openl.util.BooleanUtils;

/**
 * @author Andrei Astrouski
 */
public class TableEditor {

    private String id;
    private ITable table;
    private boolean editable;
    private String mode;
    private String view;
    private boolean showFormulas;
    private boolean collapseProps;
    private IGridFilter filter;
    private String beforeSaveAction;
    private String afterSaveAction;
    private String saveFailureAction;
    private String onBeforeSave;
    private String onAfterSave;
    private String onSaveFailure;
    private String excludeScripts;

    public TableEditor() {
    }

    public TableEditor(FacesContext context, UIComponent component) {
        Map<String, Object> attributes = component.getAttributes();
        id = component.getClientId(context);
        table = (ITable) attributes.get(Constants.ATTRIBUTE_TABLE);
        Boolean editableObj = BooleanUtils.toBooleanObject(attributes.get(Constants.ATTRIBUTE_EDITABLE));
        if (editableObj == null) {
            editable = true; // The default is 'true'
        } else {
            editable = editableObj;
        }
        mode = (String) attributes.get(Constants.ATTRIBUTE_MODE);
        view = (String) attributes.get(Constants.ATTRIBUTE_VIEW);
        showFormulas = BooleanUtils.toBoolean(attributes.get(Constants.ATTRIBUTE_SHOW_FORMULAS));
        collapseProps = BooleanUtils.toBoolean(attributes.get(Constants.ATTRIBUTE_COLLAPSE_PROPS));
        filter = (IGridFilter) component.getAttributes().get(Constants.ATTRIBUTE_FILTER);
        beforeSaveAction = FacesUtils.getValueExpressionString(component, Constants.ATTRIBUTE_BEFORE_SAVE_ACTION);
        afterSaveAction = FacesUtils.getValueExpressionString(component, Constants.ATTRIBUTE_AFTER_SAVE_ACTION);
        saveFailureAction = FacesUtils.getValueExpressionString(component, Constants.ATTRIBUTE_SAVE_FAILURE_ACTION);
        onBeforeSave = (String) attributes.get(Constants.ATTRIBUTE_ON_BEFORE_SAVE);
        onSaveFailure = (String) attributes.get(Constants.ATTRIBUTE_ON_SAVE_FAILURE);
        onAfterSave = (String) attributes.get(Constants.ATTRIBUTE_ON_AFTER_SAVE);
        excludeScripts = (String) attributes.get(Constants.ATTRIBUTE_EXCLUDE_SCRIPTS);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ITable getTable() {
        return table;
    }

    public void setTable(ITable table) {
        this.table = table;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
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

    public IGridFilter getFilter() {
        return filter;
    }

    public void setFilter(IGridFilter filter) {
        this.filter = filter;
    }

    public String getBeforeSaveAction() {
        return beforeSaveAction;
    }

    public void setBeforeSaveAction(String beforeSaveAction) {
        this.beforeSaveAction = beforeSaveAction;
    }

    public String getAfterSaveAction() {
        return afterSaveAction;
    }

    public void setAfterSaveAction(String afterSaveAction) {
        this.afterSaveAction = afterSaveAction;
    }

    public String getOnBeforeSave() {
        return onBeforeSave;
    }

    public void setOnBeforeSave(String onBeforeSave) {
        this.onBeforeSave = onBeforeSave;
    }

    public String getOnAfterSave() {
        return onAfterSave;
    }

    public void setOnAfterSave(String onAfterSave) {
        this.onAfterSave = onAfterSave;
    }

    public void setExcludeScripts(String excludeScripts) {
        this.excludeScripts = excludeScripts;
    }

    public String getExcludeScripts() {
        return excludeScripts;
    }

    public String getSaveFailureAction() {
        return saveFailureAction;
    }

    public void setSaveFailureAction(String saveFailureAction) {
        this.saveFailureAction = saveFailureAction;
    }

    public String getOnSaveFailure() {
        return onSaveFailure;
    }

    public void setOnSaveFailure(String onSaveFailure) {
        this.onSaveFailure = onSaveFailure;
    }

}

