package org.openl.rules.tableeditor.renderkit;

import java.util.Collection;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.ui.filters.IGridFilter;
import org.openl.rules.tableeditor.model.ui.LinkBuilder;
import org.openl.rules.tableeditor.util.Constants;
import org.openl.util.BooleanUtils;

/**
 * @author Andrei Astrouski
 */
public class TableEditor {

    private String id;
    private IOpenLTable table;
    private boolean editable;
    private String mode;
    private String view;
    private boolean showFormulas;
    private boolean collapseProps;
    private IGridFilter[] filters;
    private String beforeEditAction;
    private String beforeSaveAction;
    private String afterSaveAction;
    private String onBeforeEdit;
    private String onBeforeSave;
    private String onAfterSave;
    private String onError;
    private String onRequestStart;
    private String onRequestEnd;
    private String excludeScripts;
    private LinkBuilder linkBuilder;
    private Integer rowIndex;

    public TableEditor() {
    }

    public TableEditor(FacesContext context, UIComponent component) {
        Map<String, Object> attributes = component.getAttributes();
        id = component.getClientId(context) + Constants.TABLE_EDITOR_PREFIX;
        table = (IOpenLTable) attributes.get(Constants.ATTRIBUTE_TABLE);
        editable = BooleanUtils.toBoolean(attributes.get(Constants.ATTRIBUTE_EDITABLE), true);
        mode = (String) attributes.get(Constants.ATTRIBUTE_MODE);
        view = (String) attributes.get(Constants.ATTRIBUTE_VIEW);
        linkBuilder = (LinkBuilder) attributes.get(Constants.ATTRIBUTE_LINK_BUILDER);
        showFormulas = BooleanUtils.toBoolean(attributes.get(Constants.ATTRIBUTE_SHOW_FORMULAS));
        collapseProps = BooleanUtils.toBoolean(attributes.get(Constants.ATTRIBUTE_COLLAPSE_PROPS));
        castToFilters(component.getAttributes().get(Constants.ATTRIBUTE_FILTERS));
        beforeEditAction = FacesUtils.getValueExpressionString(component, Constants.ATTRIBUTE_BEFORE_EDIT_ACTION);
        beforeSaveAction = FacesUtils.getValueExpressionString(component, Constants.ATTRIBUTE_BEFORE_SAVE_ACTION);
        afterSaveAction = FacesUtils.getValueExpressionString(component, Constants.ATTRIBUTE_AFTER_SAVE_ACTION);
        onBeforeEdit = (String) attributes.get(Constants.ATTRIBUTE_ON_BEFORE_EDIT);
        onBeforeSave = (String) attributes.get(Constants.ATTRIBUTE_ON_BEFORE_SAVE);
        onError = (String) attributes.get(Constants.ATTRIBUTE_ON_ERROR);
        onAfterSave = (String) attributes.get(Constants.ATTRIBUTE_ON_AFTER_SAVE);
        onRequestStart = (String) attributes.get(Constants.ATTRIBUTE_ON_REQUEST_START);
        onRequestEnd = (String) attributes.get(Constants.ATTRIBUTE_ON_REQUEST_END);
        excludeScripts = (String) attributes.get(Constants.ATTRIBUTE_EXCLUDE_SCRIPTS);
        rowIndex = (Integer) attributes.get(Constants.ATTRIBUTE_ROW_INDEX);
    }

    private void castToFilters(Object filtersParam) {
        if (filtersParam == null) {
            filters = null;
        } else if (filtersParam instanceof IGridFilter[]) {
            filters = (IGridFilter[]) filtersParam;
        } else if (filtersParam instanceof IGridFilter) {
            filters = new IGridFilter[] { (IGridFilter) filtersParam };
        } else if (filtersParam instanceof Collection) {
            @SuppressWarnings("unchecked")
            Collection<IGridFilter> collection = (Collection<IGridFilter>) filtersParam;
            filters = collection.toArray(new IGridFilter[0]);
        } else {
            throw new IllegalArgumentException(
                String.format("Unsupported type of parameter \"%s\"", Constants.ATTRIBUTE_FILTERS));
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public IOpenLTable getTable() {
        return table;
    }

    public void setTable(IOpenLTable table) {
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

    public IGridFilter[] getFilters() {
        return filters;
    }

    public void setFilters(IGridFilter[] filters) {
        this.filters = filters;
    }

    public String getBeforeEditAction() {
        return beforeEditAction;
    }

    public void setBeforeEditAction(String beforeEditAction) {
        this.beforeEditAction = beforeEditAction;
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

    public String getOnBeforeEdit() {
        return onBeforeEdit;
    }

    public void setOnBeforeEdit(String onBeforeEdit) {
        this.onBeforeEdit = onBeforeEdit;
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

    public String getOnError() {
        return onError;
    }

    public void setOnError(String onError) {
        this.onError = onError;
    }

    public String getOnRequestStart() {
        return onRequestStart;
    }

    public void setOnRequestStart(String onRequestStart) {
        this.onRequestStart = onRequestStart;
    }

    public String getOnRequestEnd() {
        return onRequestEnd;
    }

    public void setOnRequestEnd(String onRequestEnd) {
        this.onRequestEnd = onRequestEnd;
    }

    public LinkBuilder getLinkBuilder() {
        return linkBuilder;
    }

    public void setLinkBuilder(LinkBuilder linkBuilder) {
        this.linkBuilder = linkBuilder;
    }

    public void setRowIndex(Integer rowIndex) {
        this.rowIndex = rowIndex;
    }

    public Integer getRowIndex() {
        return rowIndex;
    }
}
