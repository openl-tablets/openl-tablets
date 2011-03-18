package org.openl.rules.webstudio.web;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

import org.openl.base.INamedThing;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.ui.tests.results.RanTestsResults;
import org.openl.rules.ui.tree.richfaces.ProjectTreeBuilder;
import org.openl.rules.ui.tree.richfaces.TreeStateManager;
import org.openl.rules.ui.view.WebStudioViewMode;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.tree.ITreeElement;
import org.richfaces.model.TreeNode;

/**
 * Request scope managed bean providing logic for tree page of OpenL Studio.
 */
public class TreeBean {

    private TreeStateManager stateManager;

    public TreeBean() {
        String nodeToOpen = FacesUtils.getRequestParameter("nodeToOpen");
        stateManager = new TreeStateManager(nodeToOpen);
    }

    public TreeStateManager getStateManager() {
        return stateManager;
    }

    public String getCurrentSubMode() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        String mode = studio.getMode().getName();
        return mode;
    }

    public void setCurrentSubMode(String currentSubMode) throws Exception {
        WebStudio studio = WebStudioUtils.getWebStudio();
        studio.setMode(currentSubMode);
    }

    public List<SelectItem> getSubModes() {
        List<SelectItem> subModes = new ArrayList<SelectItem>();
        WebStudio studio = WebStudioUtils.getWebStudio();
        WebStudioViewMode mode = studio.getMode();
        String modeType = (String) mode.getType();
        WebStudioViewMode[] modes = studio.getViewSubModes(modeType);
        if (modes != null) {
            for (WebStudioViewMode viewMode : modes) {
                subModes.add(new SelectItem(viewMode.getName(), viewMode.getDisplayName(INamedThing.REGULAR)));
            }
        }
        return subModes;
    }

    public boolean isProjectExists() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        AProject currentProject = studio.getCurrentProject();
        return currentProject != null;
    }

    public boolean isProjectCheckedOut() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        RulesProject currentProject = studio.getCurrentProject();
        return currentProject.isCheckedOut();
    }

    public boolean isProjectLocalOnly() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        RulesProject currentProject = studio.getCurrentProject();
        return currentProject.isLocalOnly();
    }

    public boolean isProjectLocked() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        AProject currentProject = studio.getCurrentProject();
        return currentProject.isLocked();
    }

    public boolean isProjectHasTests() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        RanTestsResults allTestMethods = studio.getModel().getAllTestMethods();
        if (allTestMethods != null) {
            return allTestMethods.getTests().length > 0;
        }
        return false;
    }

    public TreeNode<?> getTree() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        ITreeElement<?> tree = studio.getModel().getProjectTree();
        if (tree != null) {
            TreeNode<?> rfTree = new ProjectTreeBuilder(tree, studio.getModel()).build();
            return rfTree;
        }
        return null;
    }

}
