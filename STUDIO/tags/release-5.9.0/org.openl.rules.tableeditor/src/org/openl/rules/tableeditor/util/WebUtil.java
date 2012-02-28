package org.openl.rules.tableeditor.util;

import org.openl.commons.web.jsf.FacesUtils;

public class WebUtil {

    private WebUtil() {
    }

    public static final String internalPath(String path) {
        return FacesUtils.getContextPath() + "/faces" + Constants.TABLE_EDITOR_PATTERN + path;
    }

}
