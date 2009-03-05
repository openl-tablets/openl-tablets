package org.openl.rules.tableeditor.util;

import javax.faces.context.FacesContext;

public class WebUtil {

    public static final String internalPath(String path) {
        return FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath()
                + Constants.TABLE_EDITOR_PATTERN + path;
    }

}
