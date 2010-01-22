package org.openl.rules.webstudio.web;

import org.openl.rules.ui.AllTestsRunResult;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.ui.tree.ProjectRichFacesTreeBuilder;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.uw.UserWorkspaceProject;
import org.openl.util.tree.ITreeElement;
import org.richfaces.model.TreeNode;

/**
 * Request scope managed bean providing logic for tree page of OpenL Studio.
 */
public class TreeBean {

    public TreeBean() {
    }

    public boolean isProjectExists() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        UserWorkspaceProject currentProject = studio.getCurrentProject();
        return currentProject != null;
    }

    public boolean isProjectCheckedOut() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        UserWorkspaceProject currentProject = studio.getCurrentProject();
        return currentProject.isCheckedOut();
    }

    public boolean isProjectLocalOnly() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        UserWorkspaceProject currentProject = studio.getCurrentProject();
        return currentProject.isLocalOnly();
    }

    public boolean isProjectLocked() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        UserWorkspaceProject currentProject = studio.getCurrentProject();
        return currentProject.isLocked();
    }

    public boolean isProjectCanBeValidated() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        return studio.getModel().getAllValidatedNodes().size() > 0;
    }

    public boolean isProjectHasTests() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        AllTestsRunResult allTestMethods = studio.getModel().getAllTestMethods();
        if (allTestMethods != null) {
            return allTestMethods.getTests().length > 0;
        }
        return false;
    }

    public TreeNode<?> getTree() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        ITreeElement<?> tree = studio.getModel().getProjectTree();
        if (tree != null) {
            TreeNode<?> rfTree = new ProjectRichFacesTreeBuilder(tree, studio.getModel()).build();
            return rfTree;
        }
        return null;
    }

}
