package org.openl.rules.webstudio.web.tableeditor;

import org.openl.rules.ui.WebStudio;
import org.openl.rules.web.jsf.util.FacesUtils;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

public class JSFTableViewController extends BaseTableViewController {
    protected WebStudio getWebstudio() {
        return WebStudioUtils.getWebStudio();
    }

    protected int getElementId() {
        try {
            return Integer.valueOf(FacesUtils.getRequestParameter("elementID"));
        } catch (Exception e) {
            return getWebstudio().getTableID();
        }
    }
}
