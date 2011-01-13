package org.openl.rules.webstudio.web.repository;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.common.ProjectException;
import org.openl.rules.webstudio.web.repository.tree.AbstractTreeNode;
import org.openl.rules.webstudio.web.repository.tree.TreeProject;

public class DependencyBean {
    private String projectName;
    private String lowerVersion;
    private String upperVersion;

    public String delete() {
        RepositoryTreeState treeState = (RepositoryTreeState) FacesUtils.getValueExpressionValue("#{repositoryTreeState}");
        if (treeState != null) {
            AbstractTreeNode selectedNode = treeState.getSelectedNode();
            if (selectedNode instanceof TreeProject) {
                TreeProject project = (TreeProject) selectedNode;
                try {
                    project.removeDependency(projectName);
                } catch (ProjectException e) {
                    FacesUtils.addErrorMessage(e.getMessage());
                    return UiConst.OUTCOME_FAILURE;
                }
            }
        }

        return null;
    }

    public String getLowerVersion() {
        return lowerVersion;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getUpperVersion() {
        return upperVersion;
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

    public void setLowerVersion(String lowerVersion) {
        this.lowerVersion = lowerVersion;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setUpperVersion(String upperVersion) {
        this.upperVersion = upperVersion;
    }
}
