package org.openl.rules.webstudio.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.model.SelectItem;

//import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.ui.tests.results.RanTestsResults;
import org.openl.rules.ui.tree.richfaces.ProjectTreeBuilder;
//import org.openl.rules.ui.tree.richfaces.TreeStateManager;
import org.openl.rules.ui.tree.view.RulesTreeView;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.tree.ITreeElement;
import org.richfaces.model.SequenceRowKey;
import org.richfaces.model.TreeNode;
import org.richfaces.model.TreeNodeImpl;

/**
 * Request scope managed bean providing logic for tree page of OpenL Studio.
 */
@ManagedBean
@RequestScoped
public class TreeBean {

    //private TreeStateManager stateManager;

    public TreeBean() {
        //String nodeToOpen = FacesUtils.getRequestParameter("nodeToOpen");
        //stateManager = new TreeStateManager(nodeToOpen);
    }

    /*public TreeStateManager getStateManager() {
        return stateManager;
    }*/

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
        RanTestsResults allTestMethods = studio.getModel().getAllTestMethods();
        if (allTestMethods != null) {
            return allTestMethods.getTests().length > 0;
        }
        return false;
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

    public Collection<Object> getSelected() {
        Collection<Object> selected = new ArrayList<Object>();
        selected.add(new SequenceRowKey("0.1"));
        return selected;
    }

}
