package org.openl.rules.webstudio.web;

import org.openl.rules.ui.Explanation;
import org.openl.rules.ui.Explanator;
import org.openl.rules.ui.tree.ExplainRichFacesTreeBuilder;
import org.openl.rules.ui.tree.RichFacesTreeBuilder;
import org.openl.rules.web.jsf.util.FacesUtils;
import org.openl.util.tree.ITreeElement;
import org.richfaces.model.TreeNode;

/**
 * Request scope managed bean providing logic for explain tree page of OpenL Studio.
 */
public class ExplainTreeBean {

    public static final String EXPLANATOR_NAME = "explanator";

    public ExplainTreeBean() {
    }

    public TreeNode<?> getTree() {
        Explanator explanator = (Explanator) FacesUtils.getSessionParam(EXPLANATOR_NAME);
        String rootID = FacesUtils.getRequestParameter("rootID");
        Explanation explanation = explanator.getExplanation(rootID);
        ITreeElement<?> tree = explanation.getExplainTree();
        if (tree != null) {
            RichFacesTreeBuilder treeBuilder = new ExplainRichFacesTreeBuilder(tree);
            TreeNode<?> rfTree = treeBuilder.build(true);
            return rfTree;
        }
        return null;
    }

}
