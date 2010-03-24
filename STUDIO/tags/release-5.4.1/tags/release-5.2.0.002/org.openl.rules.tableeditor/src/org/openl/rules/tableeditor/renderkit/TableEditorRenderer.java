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

import org.openl.rules.table.IGridTable;
import org.openl.rules.tableeditor.model.EditorHelper;
import org.openl.rules.tableeditor.model.ui.ActionLink;

import org.openl.rules.tableeditor.util.Constants;

public class TableEditorRenderer extends TableViewerRenderer {

    @SuppressWarnings("unchecked")
    @Override
    public void encodeBegin(FacesContext context, UIComponent component)
            throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        Map editorParams = component.getAttributes();
        Boolean editable = getBooleanParam(editorParams.get(Constants.ATTRIBUTE_EDITABLE));
        ExternalContext externalContext = context.getExternalContext();
        Map<String, String> requestMap = externalContext.getRequestParameterMap();
        String mode = (String) editorParams.get(Constants.ATTRIBUTE_MODE);
        String editorId = component.getClientId(context);
        IGridTable table = (IGridTable) component.getAttributes().get(Constants.ATTRIBUTE_TABLE);
        List<ActionLink> actionLinks = getActionLinks(component);
        String cellToEdit = (String) requestMap.get(Constants.REQUEST_PARAM_CELL);
        if (editable) {
            initEditorHelper(externalContext, editorId, table);
        }
        writer.write(
                new HTMLRenderer().render(mode, table, actionLinks, editable, cellToEdit, false, editorId));
    }

    /**
     * Convert Object to Boolean.
     * Temporary method. Will be removed.
     * 
     * @deprecated
     * @param param Object param
     * @return Boolean param
     */
    private Boolean getBooleanParam(Object param) {
        Boolean bParam = null;
        if (param instanceof String) {
            bParam = new Boolean((String)param);
        } else if (param instanceof Boolean) {
            bParam = (Boolean) param;
        }
        return bParam;
    }

    @SuppressWarnings("unchecked")
    private void initEditorHelper(ExternalContext externalContext,
            String editorId, IGridTable table) {
        Map sessionMap = externalContext.getSessionMap();
        synchronized (sessionMap) {
            Map helperMap = (Map) sessionMap.get(
                    Constants.TABLE_EDITOR_HELPER_NAME);
            if (helperMap == null) {
                helperMap = new HashMap<String, EditorHelper>();
                sessionMap.put(Constants.TABLE_EDITOR_HELPER_NAME, helperMap);
            }
            EditorHelper editorHelper = new EditorHelper();
            editorHelper.init(table);
            helperMap.put(editorId, editorHelper);
        }
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
                if (name != null && !name.equals("")
                        && action != null && !action.equals("")) {
                    links.add(new ActionLink(name, action));
                }
            }
        }
        return links;
    }

}
