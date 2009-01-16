package org.openl.rules.webstudio.web.tableeditor;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

import org.openl.rules.ui.WebStudio;

/**
 * {@link TableEditorEnvironment} implementation for plain JSP.  
 */
public class JSPTableEditorEnvironment implements TableEditorEnvironment {
    private HttpServletRequest request;

    public JSPTableEditorEnvironment(HttpServletRequest request) {
        this.request = request;
    }

    public WebStudio getWebstudio() {
        return (WebStudio) request.getSession().getAttribute("studio");
    }

    public int getElementId() {
        try {
            return Integer.parseInt(request.getParameter("elementID"));
        } catch (NumberFormatException e) {
            return getWebstudio().getTableID();
        }
    }

    public Map getParameterMap() {
        Map result = new HashMap();
        for (Map.Entry entry : ((Set<Map.Entry>) request.getParameterMap().entrySet())) {
            if (entry.getValue() instanceof String[]) {
                String[] s = (String[]) entry.getValue();
                if (s.length == 1) {
                    result.put(entry.getKey(), s[0]);
                    continue;
                }
            }
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public Object getSessionAttribute(String name) {
        return request.getSession().getAttribute(name);
    }

    public void setSessionAttribute(String name, Object value) {
        request.getSession().setAttribute(name, value);
    }

    public Object getSessionObject() {
        return request.getSession();
    }
    
}
