package org.openl.rules.webstudio.web;

import java.util.Iterator;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.openl.rules.dt.trace.DTRuleTracerLeaf;
import org.openl.rules.dt.trace.DecisionTableTraceObject;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.TraceHelper;
import org.openl.rules.ui.WebStudio;
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
        TraceHelper traceHelper = studio.getTraceHelper();

        if (model.hasTestSuitesToRun()) {

            Tracer tracer = model.traceElement(model.popLastTest());

            ITreeElement<?> tree = traceHelper.getTraceTree(tracer);
            if (tree != null) {
                return buildTreeNode(traceHelper, tree);
            }
        } else {
            if (traceHelper.getTreeRoot() != null) {
                return buildTreeNode(traceHelper, traceHelper.getTreeRoot());
            }
        }
        return null;
    }
    
    public boolean isDetailedTraceTree() {
        return WebStudioUtils.getWebStudio().getTraceHelper().isDetailedTraceTree();
    }
    
    public void setDetailedTraceTree(boolean detailedTraceTree) {
        WebStudioUtils.getWebStudio().getTraceHelper().setDetailedTraceTree(detailedTraceTree);
    }
    
    public boolean hasDecisionTables() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        ProjectModel model = studio.getModel();
        TraceHelper traceHelper = studio.getTraceHelper();

        if (model.hasTestSuitesToRun()) {
            Tracer tracer = model.traceElement(model.popLastTest());

            ITreeElement<?> tree = traceHelper.getTraceTree(tracer);
            return tree != null && hasDecisionTables(tree);
        } else {
            if (traceHelper.getTreeRoot() != null) {
                return hasDecisionTables(traceHelper.getTreeRoot());
            } else {
                return false;
            }
        }
    }

    private boolean hasDecisionTables(ITreeElement<?> node) {
        Iterator<? extends ITreeElement<?>> children = node.getChildren();

        while (children.hasNext()) {
            ITreeElement<?> child = children.next();
            if (child instanceof DecisionTableTraceObject || child instanceof DTRuleTracerLeaf) {
                return true;
            }
            if (hasDecisionTables(child)) {
                return true;
            }
        }

        return false;
    }

    private TreeNode buildTreeNode(TraceHelper traceHelper, ITreeElement<?> tree) {
        return new TraceTreeBuilder(tree, traceHelper).build();
    }

}
