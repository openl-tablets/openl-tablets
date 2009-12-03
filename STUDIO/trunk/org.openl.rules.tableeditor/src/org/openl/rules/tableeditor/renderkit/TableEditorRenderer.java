package org.openl.rules.tableeditor.renderkit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlOutputLink;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.openl.rules.table.ITable;
import org.openl.rules.table.ui.IGridFilter;
import org.openl.rules.tableeditor.model.TableEditorModel;
import org.openl.rules.tableeditor.model.ui.ActionLink;

import org.openl.rules.tableeditor.util.Constants;

public class TableEditorRenderer extends TableViewerRenderer {

    @SuppressWarnings("unchecked")
    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        Map attributes = component.getAttributes();
        // TODO Encapsulate all params into bean
        ITable table = (ITable) attributes.get(Constants.ATTRIBUTE_TABLE);
        Boolean editable = (Boolean) attributes.get(Constants.ATTRIBUTE_EDITABLE);
        ExternalContext externalContext = context.getExternalContext();
        Map<String, String> requestMap = externalContext.getRequestParameterMap();
        String mode = (String) attributes.get(Constants.ATTRIBUTE_MODE);
        String view = (String) attributes.get(Constants.ATTRIBUTE_VIEW);
        boolean showFormulas = (Boolean) attributes.get(Constants.ATTRIBUTE_SHOW_FORMULAS);
        boolean collapseProps = (Boolean) attributes.get(Constants.ATTRIBUTE_COLLAPSE_PROPS);
        String editorId = component.getClientId(context);
        IGridFilter filter = (IGridFilter) component.getAttributes().get(Constants.ATTRIBUTE_FILTER);
        List<ActionLink> actionLinks = getActionLinks(component);
        String cellToEdit = requestMap.get(Constants.REQUEST_PARAM_CELL);
        String beforeSaveAction = getValueExpressionString(component, Constants.ATTRIBUTE_BEFORE_SAVE_ACTION);
        String afterSaveAction = getValueExpressionString(component, Constants.ATTRIBUTE_AFTER_SAVE_ACTION);
        if (editable) {
            initEditorModel(externalContext, editorId, table, view, showFormulas, collapseProps,
                    beforeSaveAction, afterSaveAction);
        }
        writer.write(new HTMLRenderer().render(mode, table, view, actionLinks, editable, cellToEdit,
                false, editorId, filter, showFormulas, collapseProps));
    }

    private String getValueExpressionString(UIComponent component, String attributeName) {
        ValueExpression valueExpression = component.getValueExpression(attributeName);
        if (valueExpression != null) {
            return valueExpression.getExpressionString();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<ActionLink> getActionLinks(UIComponent component) {
        List<ActionLink> links = new ArrayList<ActionLink>();
        List children = component.getChildren();
        for (Object child : children) {
            if (child instanceof HtmlOutputLink) {
                HtmlOutputLink link = (HtmlOutputLink) child;
                List linkChildren = link.getChildren();
                String name = null;
                String action = null;
                Object value = link.getValue();
                if (value != null) {
                    action = value.toString();
                }
                if (linkChildren != null && !linkChildren.isEmpty()) {
                    Object linkChild = linkChildren.get(0);
                    name = linkChild.toString();
                }
                if (name != null && !name.equals("") && action != null && !action.equals("")) {
                    links.add(new ActionLink(name, action));
                }
            }
        }
        return links;
    }

    @SuppressWarnings("unchecked")
    private void initEditorModel(ExternalContext externalContext, String editorId, ITable table, String view,
            boolean showFormulas, boolean collapseProps, String beforeSaveAction, String afterSaveAction) {
        Map sessionMap = externalContext.getSessionMap();
        synchronized (sessionMap) {
            Map editorModelMap = (Map) sessionMap.get(Constants.TABLE_EDITOR_MODEL_NAME);
            if (editorModelMap == null) {
                editorModelMap = new HashMap<String, TableEditorModel>();
                sessionMap.put(Constants.TABLE_EDITOR_MODEL_NAME, editorModelMap);
            }
            TableEditorModel editorModel = (TableEditorModel) editorModelMap.get(editorId);
            if (editorModel != null) {
                editorModel.cancel();
            }
            editorModel = new TableEditorModel(table, view, showFormulas);
            editorModel.setCollapseProps(collapseProps);
            editorModel.setBeforeSaveAction(beforeSaveAction);
            editorModel.setAfterSaveAction(afterSaveAction);
            editorModelMap.put(editorId, editorModel);
        }
    }

}
