package org.openl.rules.webstudio.web.tableeditor;

import java.util.Map;

import org.openl.rules.ui.WebStudio;

/**
 * Strategy for logic classes to access session and request objects in different environments, e.g. JSF and plain JSP.
 */
public interface TableEditorEnvironment {
    WebStudio getWebstudio();

    int getElementId();

    Map getParameterMap();

    Object getSessionAttribute(String name);

    void setSessionAttribute(String name, Object value);

    Object getSessionObject();
}
