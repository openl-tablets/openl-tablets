package org.openl.rules.ui.repository.dependency;

import org.openl.rules.ui.repository.RepositoryTreeController;
import org.openl.rules.ui.repository.RepositoryTreeState;
import org.openl.rules.ui.repository.UiConst;
import org.openl.rules.ui.repository.tree.AbstractTreeNode;
import org.openl.rules.ui.repository.tree.TreeProject;
import org.openl.rules.webstudio.util.FacesUtils;
import org.openl.rules.workspace.abstracts.ProjectException;

import javax.faces.context.FacesContext;
import javax.faces.application.FacesMessage;

public class DependencyBean {
    private String projectName;
    private String lowerVersion;
    private String upperVersion;

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getLowerVersion() {
        return lowerVersion;
    }

    public void setLowerVersion(String lowerVersion) {
        this.lowerVersion = lowerVersion;
    }

    public String getUpperVersion() {
        return upperVersion;
    }

    public void setUpperVersion(String upperVersion) {
        this.upperVersion = upperVersion;
    }

    public String getVersionString() {
        StringBuilder sb = new StringBuilder(lowerVersion).append(" - ");
        if (upperVersion != null) {
            sb.append(upperVersion);
        } else {
            sb.append("...");
        }
        return sb.toString();
    }

    public String delete() {
        RepositoryTreeState treeState = (RepositoryTreeState) FacesUtils.getFacesVariable("#{repositoryTreeState}");
        if (treeState != null) {
            AbstractTreeNode selectedNode = treeState.getSelectedNode();
            if (selectedNode instanceof TreeProject) {
                TreeProject project = (TreeProject) selectedNode;
                try {
                    project.removeDependency(projectName);
                } catch (ProjectException e) {
                    FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(e.getMessage()));
                    return UiConst.OUTCOME_FAILURE;
                }
            }
        }

        return null;
    }
}
