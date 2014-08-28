package org.openl.rules.webstudio.web;


import org.openl.rules.dt.trace.DTRuleTracerLeaf;
import org.openl.rules.dt.trace.DecisionTableTraceObject;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.TraceHelper;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.ui.tree.richfaces.TraceTreeBuilder;
import org.openl.rules.ui.tree.richfaces.TreeNode;
import org.openl.rules.webstudio.web.test.RunTestHelper;
import org.openl.rules.webstudio.web.trace.TraceIntoFileBean;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.tree.ITreeElement;
import org.openl.vm.trace.ITracerObject;
import org.openl.vm.trace.Tracer;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;

/**
 * Request scope managed bean providing logic for trace tree page of OpenL Studio.
 */
@ManagedBean
@RequestScoped
public class TraceTreeBean {

    @ManagedProperty("#{runTestHelper}")
    private RunTestHelper runTestHelper;

    public void setRunTestHelper(RunTestHelper runTestHelper) {
        this.runTestHelper = runTestHelper;
    }

    public void init() {
        runTestHelper.catchParams();
        TestSuite testSuite = runTestHelper.getTestSuite();

        WebStudio studio = WebStudioUtils.getWebStudio();
        ProjectModel model = studio.getModel();
        TraceHelper traceHelper = studio.getTraceHelper();
        Tracer tracer = model.traceElement(testSuite);

        traceHelper.getTraceTree(tracer);// Register
    }

    public void traceIntoFile() {
        runTestHelper.catchParams();
        TestSuite testSuite = runTestHelper.getTestSuite();
        new TraceIntoFileBean().traceIntoFile(testSuite);
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

            ITreeElement<ITracerObject> tree = traceHelper.getTraceTree(tracer);
            return tree != null && hasDecisionTables(tree);
        } else {
            if (traceHelper.getTreeRoot() != null) {
                return hasDecisionTables(traceHelper.getTreeRoot());
            } else {
                return false;
            }
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

    private TreeNode buildTreeNode(TraceHelper traceHelper, ITreeElement<?> tree) {
        return new TraceTreeBuilder(tree, traceHelper).build();
    }

}
