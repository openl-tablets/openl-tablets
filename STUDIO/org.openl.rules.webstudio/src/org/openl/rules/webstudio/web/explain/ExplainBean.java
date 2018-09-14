package org.openl.rules.webstudio.web.explain;

import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.servlet.http.HttpServletResponse;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.ui.Explanator;

/**
 * Request scope managed bean for Explain page.
 */
@ManagedBean
@RequestScoped
public class ExplainBean {

    public List<String[]> getExpandedValues() {
        String requestId = FacesUtils.getRequestParameter("requestId");
        String rootID = FacesUtils.getRequestParameter("rootID");
        List<String[]> explainList = Explanator.getExplainList(requestId, rootID);
        if (explainList == null) {
            FacesUtils.getExternalContext().setResponseStatus(HttpServletResponse.SC_NOT_FOUND);
            FacesUtils.getFacesContext().responseComplete();
        }
        return explainList;
    }
}
