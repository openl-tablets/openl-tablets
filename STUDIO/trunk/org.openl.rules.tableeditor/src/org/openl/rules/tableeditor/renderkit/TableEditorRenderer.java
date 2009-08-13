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

import org.openl.rules.table.ITable;
import org.openl.rules.table.ui.IGridFilter;
import org.openl.rules.tableeditor.model.EditorHelper;
import org.openl.rules.tableeditor.model.ui.ActionLink;

import org.openl.rules.tableeditor.util.Constants;

public class TableEditorRenderer extends TableViewerRenderer {

    @SuppressWarnings("unchecked")
    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        ITable table = (ITable) component.getAttributes().get(Constants.ATTRIBUTE_TABLE);
        ResponseWriter writer = context.getResponseWriter();
        Map editorParams = component.getAttributes();
        Boolean editable = getBooleanParam(editorParams.get(Constants.ATTRIBUTE_EDITABLE));
        ExternalContext externalContext = context.getExternalContext();
        Map<String, String> requestMap = externalContext.getRequestParameterMap();
        String mode = (String) editorParams.get(Constants.ATTRIBUTE_MODE);
        String view = (String) editorParams.get(Constants.ATTRIBUTE_VIEW);
        boolean showFormulas = (Boolean) editorParams.get(Constants.ATTRIBUTE_SHOW_FORMULAS);
        String editorId = component.getClientId(context);
        IGridFilter filter = (IGridFilter) component.getAttributes().get(Constants.ATTRIBUTE_FILTER);
        List<ActionLink> actionLinks = getActionLinks(component);
        String cellToEdit = requestMap.get(Constants.REQUEST_PARAM_CELL);
        if (editable) {
            initEditorHelper(externalContext, editorId, table, view, showFormulas);
        }
        writer.write(new HTMLRenderer().render(mode, table, view, actionLinks, editable, cellToEdit,
                false, editorId, filter, showFormulas));
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

    /**
     * Convert Object to Boolean. Temporary method. Will be removed.
     *
     * @deprecated
     * @param param Object param
     * @return Boolean param
     */
    @Deprecated
    private Boolean getBooleanParam(Object param) {
        Boolean bParam = false;
        if (param instanceof String) {
            bParam = new Boolean((String) param);
        } else if (param instanceof Boolean) {
            bParam = (Boolean) param;
        }
        return bParam;
    }

    @SuppressWarnings("unchecked")
    private void initEditorHelper(ExternalContext externalContext, String editorId, ITable table, String view, boolean showFormulas) {
        Map sessionMap = externalContext.getSessionMap();
        synchronized (sessionMap) {
            Map helperMap = (Map) sessionMap.get(Constants.TABLE_EDITOR_HELPER_NAME);
            if (helperMap == null) {
                helperMap = new HashMap<String, EditorHelper>();
                sessionMap.put(Constants.TABLE_EDITOR_HELPER_NAME, helperMap);
            }
            EditorHelper editorHelper = new EditorHelper();
            editorHelper.init(table,view, showFormulas);
            helperMap.put(editorId, editorHelper);
        }
    }

}
