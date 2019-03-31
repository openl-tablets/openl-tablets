package org.openl.rules.webstudio.web;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.servlet.http.HttpServletResponse;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.ui.Explanator;
import org.openl.rules.ui.tree.richfaces.TreeNode;

/**
 * Request scope managed bean providing logic for explain tree page of OpenL Studio.
 */
@ManagedBean
@RequestScoped
public class ExplainTreeBean {

    public TreeNode getTree() {
        String requestId = FacesUtils.getRequestParameter("requestId");
        String rootID = FacesUtils.getRequestParameter("rootID");
        TreeNode explainTree = Explanator.getExplainTree(requestId, rootID);
        if (explainTree == null) {
            FacesUtils.getExternalContext().setResponseStatus(HttpServletResponse.SC_NOT_FOUND);
            FacesUtils.getFacesContext().responseComplete();
        }
        return explainTree;
    }

}
