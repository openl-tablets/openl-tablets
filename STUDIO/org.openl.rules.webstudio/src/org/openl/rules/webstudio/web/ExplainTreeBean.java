package org.openl.rules.webstudio.web;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

import org.openl.rules.ui.Explanator;
import org.openl.rules.ui.tree.richfaces.TreeNode;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

/**
 * Request scope managed bean providing logic for explain tree page of OpenL Studio.
 */
@ManagedBean
@RequestScoped
public class ExplainTreeBean {

    public TreeNode getTree() {
        String requestId = WebStudioUtils.getRequestParameter("requestId");
        String rootID = WebStudioUtils.getRequestParameter("rootID");
        TreeNode explainTree = Explanator.getExplainTree(requestId, rootID);
        if (explainTree == null) {
            WebStudioUtils.getExternalContext().setResponseStatus(HttpServletResponse.SC_NOT_FOUND);
            FacesContext.getCurrentInstance().responseComplete();
        }
        return explainTree;
    }

}
