package org.openl.rules.ui.repository.dependency;

import org.apache.commons.lang.StringUtils;

import org.openl.rules.ui.repository.RepositoryTreeState;
import org.openl.rules.ui.repository.UiConst;
import org.openl.rules.ui.repository.tree.AbstractTreeNode;
import org.openl.rules.ui.repository.tree.TreeProject;
import org.openl.rules.workspace.abstracts.ProjectVersion;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.impl.ProjectDependencyImpl;
import org.openl.rules.workspace.dtr.impl.RepositoryProjectVersionImpl;
import org.openl.rules.workspace.uw.UserWorkspaceProject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;


public class AddDependencyController {
    /** A controller which contains pre-built UI object tree. */
    private RepositoryTreeState repositoryTreeState;
    private String projectName;
    private String lowerVersion;
    private String upperVersion;

    public SelectItem[] getAvailableProjects() {
        AbstractTreeNode selected = repositoryTreeState.getCurrentNode();
        if (!(selected instanceof TreeProject)) {
            return new SelectItem[0];
        }
        Set<String> existing = new HashSet<String>();
        for (DependencyBean dep : selected.getDependencies()) {
            existing.add(dep.getProjectName());
        }
        existing.add(selected.getName());

        List<String> matching = new ArrayList<String>();
        for (AbstractTreeNode node : repositoryTreeState.getRulesRepository().getChildNodes()) {
            if (!existing.contains(node.getName())
                    && !((UserWorkspaceProject) node.getDataBean()).isLocalOnly()) {
                matching.add(node.getName());
            }
        }

        SelectItem[] result = new SelectItem[matching.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = new SelectItem(matching.get(i));
        }

        return result;
    }

    public String add() {
        AbstractTreeNode selected = repositoryTreeState.getCurrentNode();
        if (!(selected instanceof TreeProject)) {
            return UiConst.OUTCOME_FAILURE;
        }
        TreeProject project = (TreeProject) selected;

        ProjectDependencyImpl dependency = buildDependencyObject();
        if (dependency == null) {
            return UiConst.OUTCOME_FAILURE;
        }

        try {
            if (!project.addDependency(dependency)) {
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("duplicate dependency"));
                return UiConst.OUTCOME_FAILURE;
            }
        } catch (ProjectException e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(e.getMessage()));
            return UiConst.OUTCOME_FAILURE;
        }

        return null;
    }

    private ProjectDependencyImpl buildDependencyObject() {
        ProjectVersion projectVersion1 = versionFromString(lowerVersion);
        ProjectVersion projectVersion2 = null;
        if (projectVersion1 == null) {
            FacesContext.getCurrentInstance()
                .addMessage(null, new FacesMessage("lower version format error", "expected format - X[.Y[.Z]]"));
            return null;
        }
        if (!StringUtils.isEmpty(upperVersion)) {
            projectVersion2 = versionFromString(upperVersion);

            if (projectVersion2 == null) {
                FacesContext.getCurrentInstance()
                    .addMessage(null, new FacesMessage("upper version format error", "expected format - X[.Y[.Z]]"));
                return null;
            }
        }

        if ((projectVersion2 != null) && (projectVersion1.compareTo(projectVersion2) > 0)) {
            FacesContext.getCurrentInstance()
                .addMessage(null, new FacesMessage("lower version is greater than upper one"));
            return null;
        }

        return new ProjectDependencyImpl(projectName, projectVersion1, projectVersion2);
    }

    public static ProjectVersion versionFromString(String s) {
        if (StringUtils.isEmpty(s) || s.startsWith(".") || s.endsWith("..")) {
            return null;
        }
        String[] parts = s.split("\\.");
        if ((parts.length == 0) || (parts.length > 3)) {
            return null;
        }

        try {
            int major = Integer.parseInt(parts[0]);
            int minor = (parts.length < 2) ? 0 : Integer.parseInt(parts[1]);
            int rev = (parts.length < 3) ? 0 : Integer.parseInt(parts[2]);
            if ((major < 0) || (minor < 0) || (rev < 0)) {
                return null;
            }

            return new RepositoryProjectVersionImpl(major, minor, rev, null);
        } catch (Exception e) {
            return null;
        }
    }

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

    public void setRepositoryTreeState(RepositoryTreeState repositoryTreeState) {
        this.repositoryTreeState = repositoryTreeState;
    }
}
