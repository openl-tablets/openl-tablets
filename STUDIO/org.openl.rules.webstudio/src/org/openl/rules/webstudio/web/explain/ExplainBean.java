package org.openl.rules.webstudio.web.explain;

import java.util.List;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

import org.openl.rules.ui.Explanator;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Request scope managed bean for Explain page.
 */
@Service
@RequestScope
public class ExplainBean {

    public List<String[]> getExpandedValues() {
        String requestId = WebStudioUtils.getRequestParameter("requestId");
        String rootID = WebStudioUtils.getRequestParameter("rootID");
        List<String[]> explainList = Explanator.getExplainList(requestId, rootID);
        if (explainList == null) {
            WebStudioUtils.getExternalContext().setResponseStatus(HttpServletResponse.SC_NOT_FOUND);
            FacesContext.getCurrentInstance().responseComplete();
        }
        return explainList;
    }
}
