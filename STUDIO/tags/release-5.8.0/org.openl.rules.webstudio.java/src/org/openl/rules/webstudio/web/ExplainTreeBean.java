package org.openl.rules.webstudio.web;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.ui.Explanation;
import org.openl.rules.ui.Explanator;
import org.openl.rules.ui.tree.richfaces.TreeBuilder;
import org.openl.rules.ui.tree.richfaces.ExplainTreeBuilder;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.util.tree.ITreeElement;
import org.richfaces.model.TreeNode;

/**
 * Request scope managed bean providing logic for explain tree page of OpenL Studio.
 */
public class ExplainTreeBean {

    public ExplainTreeBean() {
    }

    public TreeNode<?> getTree() {
        Explanator explanator = (Explanator) FacesUtils.getSessionParam(Constants.SESSION_PARAM_EXPLANATOR);
        String rootID = FacesUtils.getRequestParameter("rootID");
        Explanation explanation = explanator.getExplanation(rootID);
        ITreeElement<?> tree = explanation.getExplainTree();
        if (tree != null) {
            TreeBuilder treeBuilder = new ExplainTreeBuilder(tree);
            TreeNode<?> rfTree = treeBuilder.build(true);
            return rfTree;
        }
        return null;
    }

}
