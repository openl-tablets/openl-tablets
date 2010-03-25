package org.openl.rules.tableeditor.util;

import javax.faces.context.FacesContext;

public class WebUtil {

    private WebUtil() {
    }

    public static final String internalPath(String path) {
        return FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath()
                + "/faces" + Constants.TABLE_EDITOR_PATTERN + path;
    }

}
