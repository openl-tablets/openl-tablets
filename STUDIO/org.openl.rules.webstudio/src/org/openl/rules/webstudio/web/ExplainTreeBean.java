package org.openl.rules.webstudio.web;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.ui.Explanator;
import org.openl.rules.ui.tree.richfaces.TreeNode;

/**
 * Request scope managed bean providing logic for explain tree page of OpenL
 * Studio.
 */
@ManagedBean
@RequestScoped
public class ExplainTreeBean {

    public TreeNode getTree() {
        String rootID = FacesUtils.getRequestParameter("rootID");
        return Explanator.getExplainTree(rootID);
    }

}
