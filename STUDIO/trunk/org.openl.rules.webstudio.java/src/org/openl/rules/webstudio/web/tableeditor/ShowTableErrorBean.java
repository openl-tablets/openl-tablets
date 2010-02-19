package org.openl.rules.webstudio.web.tableeditor;

import org.openl.rules.ui.WebStudio;
import org.openl.rules.web.jsf.util.FacesUtils;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

public class ShowTableErrorBean {

    public ShowTableErrorBean() {
    }

    private String getElementKey() {
        String elementKey = FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_ID);
        return elementKey;
    }

    public String getError() {
        WebStudio webStudio = WebStudioUtils.getWebStudio();
        return (String) webStudio.getModel().showError(getElementKey());
    }

}
