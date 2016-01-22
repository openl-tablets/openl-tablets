package org.openl.rules.webstudio.web;


import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;

import org.openl.rules.dtx.trace.DTRuleTracerLeaf;
import org.openl.rules.dtx.trace.DecisionTableTraceObject;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.TraceHelper;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.ui.tree.richfaces.TraceTreeBuilder;
import org.openl.rules.ui.tree.richfaces.TreeNode;
import org.openl.rules.webstudio.web.test.RunTestHelper;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.tree.ITreeElement;
import org.openl.vm.trace.ITracerObject;

/**
 * Request scope managed bean providing logic for trace tree page of OpenL Studio.
 */
@ManagedBean
@SessionScoped
public class TraceTreeBean {

    @ManagedProperty("#{runTestHelper}")
    private RunTestHelper runTestHelper;

    private boolean detailedTraceTree = true;
    private ITreeElement<ITracerObject> root;

    public void setRunTestHelper(RunTestHelper runTestHelper) {
        this.runTestHelper = runTestHelper;
    }

    public void init() {
        runTestHelper.catchParams();
        TestSuite testSuite = runTestHelper.getTestSuite();

        WebStudio studio = WebStudioUtils.getWebStudio();
        ProjectModel model = studio.getModel();
        TraceHelper traceHelper = studio.getTraceHelper();
        root = model.traceElement(testSuite);
        traceHelper.cacheTraceTree(root);// Register
    }

    public TreeNode getTree() {
        if (root != null) {
            return new TraceTreeBuilder(detailedTraceTree).build(root);
        }
        return null;
    }

    public boolean isDetailedTraceTree() {
        return detailedTraceTree;
    }

    public void setDetailedTraceTree(boolean detailedTraceTree) {
        this.detailedTraceTree = detailedTraceTree;
    }

    public boolean hasDecisionTables() {
        if (root != null) {
            return hasDecisionTables(root);
        } else {
            return false;
        }
    }

    private boolean hasDecisionTables(ITreeElement<ITracerObject> node) {
        Iterable<? extends ITreeElement<ITracerObject>> children = node.getChildren();
        for (ITreeElement<ITracerObject> child : children) {
            if (child instanceof DecisionTableTraceObject || child instanceof DTRuleTracerLeaf) {
                return true;
            }
            if (hasDecisionTables(child)) {
                return true;
            }
        }
        return false;
    }
}
