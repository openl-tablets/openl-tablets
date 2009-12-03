package org.openl.rules.tableeditor.util;

import javax.faces.context.FacesContext;

public class WebUtil {

    public static final String internalPath(String path) {
        return FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath()
                + "/faces" + Constants.TABLE_EDITOR_PATTERN + path;
    }

    public static String toJSString(String string) {
        if (string != null) {
            return string.replaceAll("'", "\\\\'").replaceAll("\"", "\\\\\"");
        }
        return null;
    }

}
