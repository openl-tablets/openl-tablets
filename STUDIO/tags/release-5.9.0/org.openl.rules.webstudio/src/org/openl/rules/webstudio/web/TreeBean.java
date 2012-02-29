package org.openl.rules.webstudio.web;

import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.model.SelectItem;

import org.apache.commons.lang.ArrayUtils;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.ui.tree.richfaces.ProjectTreeBuilder;
import org.openl.rules.ui.tree.view.RulesTreeView;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.tree.ITreeElement;
import org.richfaces.model.TreeNode;
import org.richfaces.model.TreeNodeImpl;

/**
 * Request scope managed bean providing logic for tree page of OpenL Studio.
 */
@ManagedBean
@RequestScoped
public class TreeBean {

    public String getCurrentView() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        return studio.getTreeView().getName();
    }

    public void setCurrentView(String currentView) throws Exception {
        WebStudio studio = WebStudioUtils.getWebStudio();
        studio.setTreeView(currentView);
    }

    public List<SelectItem> getViews() {
        List<SelectItem> views = new ArrayList<SelectItem>();
        WebStudio studio = WebStudioUtils.getWebStudio();
        RulesTreeView[] treeViews = studio.getTreeViews();
        if (treeViews != null) {
            for (RulesTreeView viewMode : treeViews) {
                views.add(new SelectItem(viewMode.getName(), "By " + viewMode.getDisplayName()));
            }
        }
        return views;
    }

    public boolean isProjectHasTests() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        TestSuiteMethod[] allTestMethods = studio.getModel().getAllTestMethods();
        return ArrayUtils.isNotEmpty(allTestMethods);
    }

    public TreeNode getTree() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        ITreeElement<?> tree = studio.getModel().getProjectTree();
        if (tree != null) {
            TreeNode rfTree = new ProjectTreeBuilder(tree, studio.getModel()).build();
            return rfTree;
        }
        // Empty tree
        return new TreeNodeImpl();
    }

}
