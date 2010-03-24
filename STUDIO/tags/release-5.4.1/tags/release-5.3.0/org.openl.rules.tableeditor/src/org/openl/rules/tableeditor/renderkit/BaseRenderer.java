package org.openl.rules.tableeditor.renderkit;

import javax.faces.render.Renderer;

public abstract class BaseRenderer extends Renderer {
    /**
     * Convert Object to Boolean.
     * Temporary method. Will be removed since JSF 1.2.
     *
     * @deprecated
     * @param param Object param
     * @return Boolean param
     */
    @Deprecated
    protected Boolean toBoolean(Object param) {
        Boolean bParam = false;
        if (param instanceof String) {
            bParam = new Boolean((String) param);
        } else if (param instanceof Boolean) {
            bParam = (Boolean) param;
        }
        return bParam;
    }
}
