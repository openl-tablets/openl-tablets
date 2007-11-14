package org.openl.rules.ui.repository.dependency;

import org.openl.rules.ui.repository.RepositoryTreeController;
import org.openl.rules.ui.repository.UiConst;
import org.openl.rules.ui.repository.tree.AbstractTreeNode;
import org.openl.rules.ui.repository.tree.TreeProject;
import org.openl.rules.webstudio.util.FacesUtils;

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
        RepositoryTreeController tree = (RepositoryTreeController) FacesUtils.getFacesVariable("#{repositoryTree}");
        if (tree != null && tree.getSelected() != null) {
            AbstractTreeNode selected = tree.getSelected();
            if (selected instanceof TreeProject) {
                TreeProject project = (TreeProject) selected;
                project.removeDependency(projectName);
            }
        }

        return UiConst.OUTCOME_SUCCESS;
    }
}
