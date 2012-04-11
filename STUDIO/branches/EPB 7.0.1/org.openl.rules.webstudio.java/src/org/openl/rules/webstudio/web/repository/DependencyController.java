package org.openl.rules.webstudio.web.repository;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.webstudio.web.repository.tree.AbstractTreeNode;
import org.openl.rules.webstudio.web.repository.tree.TreeProject;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectVersion;
import org.openl.rules.workspace.abstracts.impl.ProjectDependencyImpl;
import org.openl.rules.workspace.dtr.impl.RepositoryProjectVersionImpl;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.UserWorkspaceProject;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class DependencyController {
    /** A controller which contains pre-built UI object tree. */
    private RepositoryTreeState repositoryTreeState;
    private String projectName;
    private String lowerVersion;
    private String upperVersion;

    public SelectItem[] getAvailableProjects() {
        AbstractTreeNode selected = repositoryTreeState.getSelectedNode();
        Set<String> existing = new HashSet<String>();

        if (selected instanceof TreeProject) {
            for (DependencyBean dep : selected.getDependencies()) {
                existing.add(dep.getProjectName());
            }
            existing.add(selected.getName());
        }

        List<String> matching = new ArrayList<String>();
        for (AbstractTreeNode node : repositoryTreeState.getRulesRepository()
                .getChildNodes()) {
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
        AbstractTreeNode selected = repositoryTreeState.getSelectedNode();
        if (!(selected instanceof TreeProject)) {
            return null;
        }
        TreeProject project = (TreeProject) selected;

        ProjectDependencyImpl dependency = buildDependencyObject();
        if (dependency == null) {
            return null;
        }

        try {
            if (!project.addDependency(dependency)) {
                FacesContext.getCurrentInstance()
                    .addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "duplicate dependency", null));
                return null;
            }
        } catch (ProjectException e) {
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
            return null;
        }

        return null;
    }

    public SelectItem[] getProjectVersions() {
        UserWorkspace workspace = RepositoryUtils.getWorkspace();
        if (workspace == null || projectName == null) {
            return new SelectItem[0];
        }

        try {
            UserWorkspaceProject project = workspace.getProject(projectName);
            List<SelectItem> selectItems = new ArrayList<SelectItem>();
            for (ProjectVersion version : project.getVersions()) {
                selectItems.add(new SelectItem(version.getVersionName()));
            }
            return selectItems.toArray(new SelectItem[selectItems.size()]);
        } catch (ProjectException e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(e.getMessage()));
        }

        return new SelectItem[0];
    }

    private ProjectDependencyImpl buildDependencyObject() {
        ProjectVersion projectVersion1 = versionFromString(lowerVersion);
        ProjectVersion projectVersion2 = null;
        if (projectVersion1 == null) {
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "lower version format error", "expected format - X[.Y[.Z]]"));
            return null;
        }
        if (!StringUtils.isEmpty(upperVersion)) {
            projectVersion2 = versionFromString(upperVersion);

            if (projectVersion2 == null) {
                FacesContext.getCurrentInstance()
                    .addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "upper version format error", "expected format - X[.Y[.Z]]"));
                return null;
            }
        }

        if ((projectVersion2 != null) && (projectVersion1.compareTo(projectVersion2) > 0)) {
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "lower version is greater than upper one", null));
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
        return null;
    }

    public void setUpperVersion(String upperVersion) {
        this.upperVersion = upperVersion;
    }

    public void setRepositoryTreeState(RepositoryTreeState repositoryTreeState) {
        this.repositoryTreeState = repositoryTreeState;
    }
}
