package org.openl.rules.webstudio.web.repository;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.impl.ProjectDependencyImpl;
import org.openl.rules.common.impl.RepositoryProjectVersionImpl;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.webstudio.web.repository.tree.AbstractTreeNode;
import org.openl.rules.webstudio.web.repository.tree.TreeProject;
import org.openl.rules.webstudio.web.servlet.RulesUserSession;
import org.openl.rules.workspace.uw.UserWorkspace;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DependencyController {
    private static final Log LOG = LogFactory.getLog(DependencyController.class);

    /** A controller which contains pre-built UI object tree. */
    private RepositoryTreeState repositoryTreeState;
    private String projectName;
    private String lowerVersion;
    private String upperVersion;

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
            // ignore exception
            return null;
        }
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
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "duplicate dependency", null));
                return null;
            }
        } catch (ProjectException e) {
            LOG.error("Failed to add dependency!", e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
            return null;
        }

        return null;
    }

    private ProjectDependencyImpl buildDependencyObject() {
        ProjectVersion projectVersion1 = versionFromString(lowerVersion);
        ProjectVersion projectVersion2 = null;
        if (projectVersion1 == null) {
            FacesContext.getCurrentInstance().addMessage(
                    null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "lower version format error",
                            "expected format - X[.Y[.Z]]"));
            return null;
        }
        if (!StringUtils.isEmpty(upperVersion)) {
            projectVersion2 = versionFromString(upperVersion);

            if (projectVersion2 == null) {
                FacesContext.getCurrentInstance().addMessage(
                        null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "upper version format error",
                                "expected format - X[.Y[.Z]]"));
                return null;
            }
        }

        if ((projectVersion2 != null) && (projectVersion1.compareTo(projectVersion2) > 0)) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "lower version is greater than upper one", null));
            return null;
        }

        return new ProjectDependencyImpl(projectName, projectVersion1, projectVersion2);
    }

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
        for (AbstractTreeNode node : repositoryTreeState.getRulesRepository().getChildNodes()) {
            if (!existing.contains(node.getName()) && !((AProject) node.getDataBean()).isLocalOnly()) {
                matching.add(node.getName());
            }
        }

        SelectItem[] result = new SelectItem[matching.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = new SelectItem(matching.get(i));
        }

        return result;
    }

    public String getLowerVersion() {
        return lowerVersion;
    }

    public String getProjectName() {
        return projectName;
    }

    public SelectItem[] getProjectVersions() {
        UserWorkspace workspace;
        try {
            workspace = getRulesUserSession().getUserWorkspace();
        } catch (Exception e) {
            LOG.error("Failed to get user workspace!", e);
            return new SelectItem[0];
        }
        if (projectName == null) {
            return new SelectItem[0];
        }

        try {
            AProject project = workspace.getProject(projectName);
            List<SelectItem> selectItems = new ArrayList<SelectItem>();
            for (ProjectVersion version : project.getVersions()) {
                selectItems.add(new SelectItem(version.getVersionName()));
            }
            return selectItems.toArray(new SelectItem[selectItems.size()]);
        } catch (ProjectException e) {
            LOG.error("Cannot get project versions", e);
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(e.getMessage()));
        }

        return new SelectItem[0];
    }

    private RulesUserSession getRulesUserSession() {
        return (RulesUserSession) FacesUtils.getSessionParam("rulesUserSession");
    }

    public String getUpperVersion() {
        return null;
    }

    public void setLowerVersion(String lowerVersion) {
        this.lowerVersion = lowerVersion;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setRepositoryTreeState(RepositoryTreeState repositoryTreeState) {
        this.repositoryTreeState = repositoryTreeState;
    }

    public void setUpperVersion(String upperVersion) {
        this.upperVersion = upperVersion;
    }
}
