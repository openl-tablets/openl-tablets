package org.openl.rules.webstudio.web.tableeditor;

import org.openl.rules.ui.WebStudio;
import org.openl.rules.web.jsf.util.FacesUtils;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

public class ShowTableErrorBean {

    public ShowTableErrorBean() {
    }

    public int getErrorId() {
        return getElementId("elementID");
    }

    public int getTableId() {
        return getElementId("tableID");
    }

    private int getElementId(String name) {
        int elementId = -1;
        String elementIdStr = FacesUtils.getRequestParameter(name);
        if (elementIdStr != null) {
            elementId = Integer.parseInt(elementIdStr);
        }
        return elementId;
    }

    public String getError() {
        WebStudio webStudio = WebStudioUtils.getWebStudio();
        return (String) webStudio.getModel().showError(getErrorId());
    }

}
