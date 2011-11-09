package org.openl.rules.webstudio.web;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.TraceHelper;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.ui.tree.richfaces.TreeBuilder;
import org.openl.rules.ui.tree.richfaces.TraceTreeBuilder;
import org.openl.rules.ui.tree.richfaces.TreeNode;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.tree.ITreeElement;
import org.openl.vm.trace.Tracer;

/**
 * Request scope managed bean providing logic for trace tree page of OpenL Studio.
 */
@ManagedBean
@RequestScoped
public class TraceTreeBean {

    public TraceTreeBean() {
    }

    public TreeNode getTree() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        ProjectModel model = studio.getModel();
        if (model.hasLastTest()) {

            Tracer tracer = model.traceElement(model.popLastTest());

            TraceHelper traceHelper = studio.getTraceHelper();

            ITreeElement<?> tree = traceHelper.getTraceTree(tracer);
            if (tree != null) {
                TreeBuilder treeBuilder = new TraceTreeBuilder(tree, traceHelper);
                TreeNode rfTree = treeBuilder.build();
                return rfTree;
            }
        }
        return null;
    }

}
