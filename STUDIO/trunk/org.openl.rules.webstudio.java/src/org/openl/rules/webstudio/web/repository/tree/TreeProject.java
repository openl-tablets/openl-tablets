package org.openl.rules.webstudio.web.repository.tree;

import org.openl.rules.common.LockInfo;
import org.openl.rules.common.ProjectDependency;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.VersionInfo;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.webstudio.web.repository.DependencyBean;
import org.openl.rules.webstudio.web.repository.UiConst;
import org.openl.util.filter.IFilter;

import java.util.Date;
import java.util.List;
import java.util.Collection;
import java.util.ArrayList;

/**
 * Represents OpenL project in a tree.
 *
 * @author Aleh Bykhavets
 *
 */
public class TreeProject extends TreeFolder {

    private static final long serialVersionUID = -326805891782640894L;

    private List<DependencyBean> dependencies;

    protected static String generateComments(UserWorkspaceProject userProject) {
        if (userProject.isLocalOnly()) {
            return "Local";
        }

        if (userProject.isDeleted()) {
            return "Deleted";
        }

        if (userProject.isCheckedOut()) {
            // "Checked-out"
            return null;
        }

        if (userProject.isOpened()) {
            ProjectVersion activeVersion = userProject.getVersion();

            if (activeVersion != null && userProject.isOpenedOtherVersion()) {
                return "Version " + activeVersion.getVersionName();
            } else {
                return null;
            }
        }

        if (userProject.isLocked()) {
            LockInfo lock = userProject.getLockInfo();

            if (lock != null) {
                if (userProject.isLockedByMe()) {
                    return "Locked by you. Please close this project.";
                } else {
                    return "Locked by " + lock.getLockedBy().getUserName();
                }
            }
        }

        return null;
    }

    // ------ UI methods ------

    protected static String generateStatus(UserWorkspaceProject userProject) {
        if (userProject.isLocalOnly()) {
            return "Local";
        }

        if (userProject.isDeleted()) {
            return "Deleted";
        }

        if (userProject.isCheckedOut()) {
            return "Checked-out";
        }
        
        StringBuilder status = new StringBuilder();

        if (userProject.isOpened()) {
            if (userProject.isOpenedOtherVersion()) {
                status.append("Opened Old Version");
            } else {
                status.append("Opened");
            }
        }else{
            status.append("Closed");
        }
        
        if(userProject.isLocked()){
            status.append(" - Locked");
        }

        return status.toString();
    }

    public TreeProject(String id, String name, IFilter<AProjectArtefact> filter) {
        super(id, name, filter);
    }

    public synchronized boolean addDependency(ProjectDependency dep) throws ProjectException {
        Collection<ProjectDependency> dependencies = getProject().getDependencies();
        if (dependencies.contains(dep)) {
            return false;
        }

        List<ProjectDependency> newDeps = new ArrayList<ProjectDependency>(dependencies);
        newDeps.add(dep);
        ((AProject) getData()).setDependencies(newDeps);
        this.dependencies = null;
        return true;
    }

    public String getComments() {
        return generateComments(getProject());
    }

    public Date getCreatedAt() {
        ProjectVersion projectVersion = getProject().getVersion();
        if (projectVersion == null) {
            return null;
        }

        VersionInfo vi = projectVersion.getVersionInfo();
        return (vi != null) ? vi.getCreatedAt() : null;
    }

    public String getCreatedBy() {
        ProjectVersion projectVersion = (getProject()).getVersion();
        if (projectVersion == null) {
            return null;
        }

        VersionInfo vi = projectVersion.getVersionInfo();
        return (vi != null) ? vi.getCreatedBy() : null;
    }

    @Override
    public synchronized List<DependencyBean> getDependencies() {
        if (dependencies == null) {
            Collection<ProjectDependency> deps = getProject().getDependencies();
            dependencies = new ArrayList<DependencyBean>(deps.size());
            for (ProjectDependency pd : deps) {
                DependencyBean depBean = new DependencyBean();
                depBean.setProjectName(pd.getProjectName());
                depBean.setLowerVersion(pd.getLowerLimit().getVersionName());
                if (pd.hasUpperLimit()) {
                    depBean.setUpperVersion(pd.getUpperLimit().getVersionName());
                }
                dependencies.add(depBean);
            }
        }

        return dependencies;
    }

    @Override
    public String getIcon() {
        RulesProject project = getProject();

        if (project.isLocalOnly()) {
            return UiConst.ICON_PROJECT_LOCAL;
        }

        if (project.isDeleted()) {
            return UiConst.ICON_PROJECT_DELETED;
        }

        if (project.isCheckedOut()) {
            return UiConst.ICON_PROJECT_CHECKED_OUT;
        }

        boolean isLocked = project.isLocked();
        if (project.isOpened()) {
            if (isLocked) {
                return UiConst.ICON_PROJECT_OPENED_LOCKED;
            } else {
                return UiConst.ICON_PROJECT_OPENED;
            }
        } else {
            if (isLocked) {
                return UiConst.ICON_PROJECT_CLOSED_LOCKED;
            } else {
                return UiConst.ICON_PROJECT_CLOSED;
            }
        }
    }

    private RulesProject getProject() {
        return (RulesProject) getData();
    }

    public String getStatus() {
        return generateStatus(getProject());
    }

    @Override
    public String getType() {
        return UiConst.TYPE_PROJECT;
    }

    public String getVersion() {
        ProjectVersion projectVersion = (getProject()).getVersion();
        if (projectVersion == null) {
            return "unversioned";
        }
        return projectVersion.getVersionName();
    }

    public synchronized void removeDependency(String dependency) throws ProjectException {
        List<ProjectDependency> dependencies = getProject().getDependencies();
        List<ProjectDependency> newDeps = new ArrayList<ProjectDependency>();
        boolean changed = false;
        for (ProjectDependency d : dependencies) {
            if (d.getProjectName().equals(dependency)) {
                changed = true;
            } else {
                newDeps.add(d);
            }
        }
        if (changed) {
            this.dependencies = null;
            getProject().setDependencies(newDeps);
        }
    }
    
    @Override
    public void refresh() {
        super.refresh();
        dependencies = null;
    }
}
