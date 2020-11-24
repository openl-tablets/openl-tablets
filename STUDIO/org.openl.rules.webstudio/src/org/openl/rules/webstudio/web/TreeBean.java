package org.openl.rules.webstudio.web;

import static org.openl.rules.security.AccessManager.isGranted;
import static org.openl.rules.security.Privileges.RUN;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.ui.tree.ProjectTreeNode;
import org.openl.rules.ui.tree.richfaces.ProjectTreeBuilder;
import org.openl.rules.validation.properties.dimentional.DispatcherTablesBuilder;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.syntax.ISyntaxNode;
import org.openl.util.CollectionUtils;
import org.richfaces.model.TreeNode;
import org.richfaces.model.TreeNodeImpl;

import java.util.function.Predicate;

/**
 * Request scope managed bean providing logic for tree page of OpenL Studio.
 */
@ManagedBean
@SessionScoped
public class TreeBean {

    private boolean hideUtilityTables = true;

    public void setHideUtilityTables(boolean hideUtilityTables) {
        this.hideUtilityTables = hideUtilityTables;
        WebStudioUtils.getWebStudio().getModel().resetProjectTree();
    }

    public boolean isHideUtilityTables() {
        return hideUtilityTables;
    }

    public void setCurrentView(String currentView) throws Exception {
        WebStudio studio = WebStudioUtils.getWebStudio();
        studio.setTreeView(currentView);
    }

    public boolean getCanRun() {
        return isGranted(RUN);
    }

    public int getProjectTestsCount() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        TestSuiteMethod[] allTestMethods = studio.getModel().getAllTestMethods();
        return CollectionUtils.isNotEmpty(allTestMethods) ? allTestMethods.length : 0;
    }

    public TreeNode getTree() {
        WebStudio studio = WebStudioUtils.getWebStudio();

        ProjectTreeNode tree = studio.getModel().getProjectTree();
        if (tree != null) {
            Predicate<ProjectTreeNode> utilityTablePredicate = new UtilityTablePredicate(hideUtilityTables);

            return new ProjectTreeBuilder(utilityTablePredicate).build(tree);
        }
        // Empty tree
        return new TreeNodeImpl();
    }

    private static class UtilityTablePredicate implements Predicate<ProjectTreeNode> {
        private boolean hideUtilityTables;

        public UtilityTablePredicate(boolean hideUtilityTables) {
            this.hideUtilityTables = hideUtilityTables;
        }

        @Override
        public boolean test(ProjectTreeNode tableNode) {
            if (tableNode.isLeaf() && tableNode.getObject() instanceof ISyntaxNode) {
                String tableType = ((ISyntaxNode) tableNode.getObject()).getType();
                if (hideUtilityTables) {
                    if (XlsNodeTypes.XLS_OTHER.toString().equals(tableType)) {
                        return true;
                    }
                }

                // Always hide dispatcher tables
                if (XlsNodeTypes.XLS_DT.toString().equals(tableType)) {
                    if (DispatcherTablesBuilder.isDispatcherTable((TableSyntaxNode) tableNode.getObject())) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
