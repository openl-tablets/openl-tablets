package org.openl.rules.tableeditor.renderkit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlOutputLink;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.tableeditor.model.TableEditorModel;
import org.openl.rules.tableeditor.model.ui.ActionLink;

import org.openl.rules.tableeditor.util.Constants;

public class TableEditorRenderer extends TableViewerRenderer {
    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        TableEditor tableEditor = new TableEditor(context, component);
        if (tableEditor.getTable() == null) { // Required attribute
            return;
        }
        ExternalContext externalContext = context.getExternalContext();
        Map<String, String> requestMap = externalContext.getRequestParameterMap();
        String cellToEdit = requestMap.get(Constants.REQUEST_PARAM_CELL);
        String errorCell = requestMap.get(Constants.REQUEST_PARAM_ERROR_CELL);
        List<ActionLink> actionLinks = getActionLinks(component);
        if (tableEditor.isEditable()) {
            initEditorModel(externalContext, tableEditor);
        }
        beforeRender(tableEditor);
        writer.write(new HTMLRenderer().render(tableEditor, cellToEdit, actionLinks, errorCell));
    }

    private void beforeRender(TableEditor editor) {
        String mode = editor.getMode();
        boolean editing = Constants.MODE_EDIT.equals(mode);

        String action = editor.getBeforeEditAction();
        if (action != null) {
            if (editing) {
                Boolean successful = (Boolean) FacesUtils.invokeMethodExpression(action);

                if (!successful) {
                    editor.setEditable(false);
                    editor.setMode(Constants.MODE_VIEW);
                }
            }
        }
    }

    private List<ActionLink> getActionLinks(UIComponent component) {
        List<ActionLink> links = new ArrayList<>();
        List<UIComponent> children = component.getChildren();
        for (Object child : children) {
            if (child instanceof HtmlOutputLink) {
                HtmlOutputLink link = (HtmlOutputLink) child;
                boolean rendered = link.isRendered();
                if (rendered) {
                    List<UIComponent> linkChildren = link.getChildren();
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
        }
        return links;
    }

    @SuppressWarnings("unchecked")
    private void initEditorModel(ExternalContext externalContext, TableEditor tableEditor) {
        Map<String, Object> sessionMap = externalContext.getSessionMap();
        synchronized (sessionMap) {
            Map editorModelMap = (Map) sessionMap.get(Constants.TABLE_EDITOR_MODEL_NAME);
            if (editorModelMap == null) {
                editorModelMap = new HashMap<String, TableEditorModel>();
                sessionMap.put(Constants.TABLE_EDITOR_MODEL_NAME, editorModelMap);
            }
            TableEditorModel editorModel = (TableEditorModel) editorModelMap.get(tableEditor.getId());
            if (editorModel != null) {
                editorModel.cancel();
            }
            editorModel = new TableEditorModel(tableEditor);
            editorModel.setCollapseProps(tableEditor.isCollapseProps());
            editorModel.setBeforeEditAction(tableEditor.getBeforeEditAction());
            editorModel.setBeforeSaveAction(tableEditor.getBeforeSaveAction());
            editorModel.setAfterSaveAction(tableEditor.getAfterSaveAction());
            editorModelMap.put(tableEditor.getId(), editorModel);
        }
    }

}
