package org.openl.rules.webstudio.web.tableeditor;

import java.util.Map;

import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.web.jsf.util.FacesUtils;

/**
 * {@link TableEditorEnvironment} implementation for plain JSF.
 */
public class JSFTableEditorEnvironment implements TableEditorEnvironment {
    public WebStudio getWebstudio() {
        return WebStudioUtils.getWebStudio();
    }

    public int getElementId() {
        try {
            return Integer.valueOf(FacesUtils.getRequestParameter("elementID"));
        } catch (Exception e) {
            return getWebstudio().getTableID();
        }
    }

    public Map getParameterMap() {
        return FacesUtils.getRequestParameterMap();
    }

    public Object getSessionAttribute(String name) {
        return FacesUtils.getSessionMap().get(name);
    }

    public void setSessionAttribute(String name, Object value) {
        FacesUtils.getSessionMap().put(name, value);
    }

    public Object getSessionObject() {
        return FacesUtils.getSessionMap();
    }
}
