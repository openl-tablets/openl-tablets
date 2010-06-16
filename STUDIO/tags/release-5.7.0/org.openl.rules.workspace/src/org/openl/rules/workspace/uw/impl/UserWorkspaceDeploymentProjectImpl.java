package org.openl.rules.workspace.uw.impl;

import static org.openl.rules.security.Privileges.*;
import static org.openl.rules.security.SecurityUtils.check;
import static org.openl.rules.security.SecurityUtils.isGranted;

import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.openl.rules.repository.CommonVersion;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectDependency;
import org.openl.rules.workspace.abstracts.ProjectDescriptor;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectResource;
import org.openl.rules.workspace.abstracts.ProjectVersion;
import org.openl.rules.workspace.abstracts.impl.ArtefactPathImpl;
import org.openl.rules.workspace.dtr.LockInfo;
import org.openl.rules.workspace.dtr.RepositoryDDProject;
import org.openl.rules.workspace.props.Property;
import org.openl.rules.workspace.props.PropertyException;
import org.openl.rules.workspace.uw.UserWorkspaceDeploymentProject;
import org.openl.rules.workspace.uw.UserWorkspaceProjectArtefact;
import org.openl.rules.workspace.uw.UserWorkspaceProjectFolder;
import org.openl.rules.workspace.uw.UserWorkspaceProjectResource;

public class UserWorkspaceDeploymentProjectImpl implements UserWorkspaceDeploymentProject {
    private UserWorkspaceImpl userWorkspace;
    private RepositoryDDProject dtrDProject;
    /**
     * Currently active version. Can be {@link #dtrDProject} or old version
     * (read only).
     */
    private RepositoryDDProject activeProjectVersion;

    private String name;
    private ArtefactPath path;
    private HashMap<String, ProjectDescriptor> descriptors;

    protected UserWorkspaceDeploymentProjectImpl(UserWorkspaceImpl userWorkspace, RepositoryDDProject dtrDProject) {
        this.userWorkspace = userWorkspace;
        this.dtrDProject = dtrDProject;

        name = dtrDProject.getName();
        path = new ArtefactPathImpl(new String[] { name });

        activeProjectVersion = dtrDProject;

        descriptors = new HashMap<String, ProjectDescriptor>();
        refresh();
    }

    public UserWorkspaceProjectFolder addFolder(String name) throws ProjectException {
        notSupported();
        return null;
    }

    public ProjectDescriptor addProjectDescriptor(String name, CommonVersion version) throws ProjectException {
        UserWorkspaceProjectDescriptorImpl uwpd = new UserWorkspaceProjectDescriptorImpl(this, name, version);
        descriptors.put(name, uwpd);

        return uwpd;
    }

    public void addProperty(Property property) throws PropertyException {
        notSupportedProps();
    }

    public UserWorkspaceProjectResource addResource(String name, ProjectResource resource) throws ProjectException {
        notSupported();
        return null;
    }

    public void checkIn() throws ProjectException {
        // do not rise version
        checkIn(0, 0);
    }

    public void checkIn(int major, int minor) throws ProjectException {
        if (!isCheckedOut()) {
            throw new ProjectException("Project ''{0}'' must be checked-out before checking-in", null, getName());
        }

        if (major != 0 || minor != 0) {
            activeProjectVersion.riseVersion(major, minor);
        }

        activeProjectVersion.commit(this, userWorkspace.getUser());
        activeProjectVersion.unlock(userWorkspace.getUser());
        refresh();
    }

    public void checkOut() throws ProjectException {
        if (isCheckedOut()) {
            throw new ProjectException("Project ''{0}'' is already checked-out", null, getName());
        }

        if (isLocked()) {
            throw new ProjectException("Project ''{0}'' is locked by ''{1}'' since ''{2}''", null, getName(),
                    activeProjectVersion.getlLockInfo().getLockedBy().getUserName(), activeProjectVersion
                            .getlLockInfo().getLockedAt());
        }

        check(PRIVILEGE_EDIT_DEPLOYMENT);

        if (isOpened()) {
            close();
        }

        activeProjectVersion.lock(userWorkspace.getUser());
        refresh();
    }

    public void close() throws ProjectException {
        if (isLockedByMe()) {
            activeProjectVersion.unlock(userWorkspace.getUser());
        }

        activeProjectVersion = dtrDProject;
        refresh();
    }

    public void delete() throws ProjectException {
        if (isLocked() && !isLockedByMe()) {
            throw new ProjectException("Cannot delete project ''{0}'' while it is locked by other user", null,
                    getName());
        }

        check(PRIVILEGE_DELETE_DEPLOYMENT);

        if (isOpened()) {
            close();
        }

        activeProjectVersion.delete(userWorkspace.getUser());
    }

    public void erase() throws ProjectException {
        check(PRIVILEGE_ERASE_DEPLOYMENT);

        activeProjectVersion.erase(userWorkspace.getUser());
    }

    public File exportVersion(CommonVersion version) throws ProjectException {
        throw new ProjectException("Export isn't supported for deployment project!");
    }

    public UserWorkspaceProjectArtefact getArtefact(String name) throws ProjectException {
        notSupported();
        return null;
    }

    public ProjectArtefact getArtefactByPath(ArtefactPath artefactPath) throws ProjectException {
        notSupported();
        return null;
    }

    public ArtefactPath getArtefactPath() {
        return path;
    }

    public Collection<? extends UserWorkspaceProjectArtefact> getArtefacts() {
        // not supported
        return new LinkedList<UserWorkspaceProjectArtefact>();
    }

    public boolean getCanCheckOut() {
        if (isCheckedOut() || isLocked()) {
            return false;
        }

        return isGranted(PRIVILEGE_EDIT_DEPLOYMENT);
    }

    public boolean getCanDelete() {
        return (!isLocked() || isLockedByMe()) && isGranted(PRIVILEGE_DELETE_DEPLOYMENT);
    }

    public boolean getCanDeploy() {
        return (!isCheckedOut() && isGranted(PRIVILEGE_DEPLOY));
    }

    public boolean getCanErase() {
        return (isDeleted() && isGranted(PRIVILEGE_ERASE_DEPLOYMENT));
    }

    public boolean getCanOpen() {
        return (!isCheckedOut() && isGranted(PRIVILEGE_READ));
    }

    public boolean getCanUndelete() {
        return (isDeleted() && isGranted(PRIVILEGE_EDIT_DEPLOYMENT));
    }

    public Collection<ProjectDependency> getDependencies() {
        // not supported
        return new LinkedList<ProjectDependency>();
    }

    public Date getEffectiveDate() {
        // not supported
        return null;
    }

    public Date getExpirationDate() {
        // not supported
        return null;
    }

    public String getLineOfBusiness() {
        // not supported
        return null;
    }

    public LockInfo getLockInfo() {
        if (dtrDProject == null) {
            return null;
        }

        return dtrDProject.getlLockInfo();
    }

    public String getName() {
        return name;
    }

    public UserWorkspaceDeploymentProjectImpl getProject() {
        return this;
    }

    public ProjectDescriptor getProjectDescriptor(String name) throws ProjectException {
        ProjectDescriptor pd = descriptors.get(name);

        if (pd == null) {
            throw new ProjectException("Cannot find descriptor for project {0} in {1}", null, name, getName());
        }

        return pd;
    }

    public Collection<ProjectDescriptor> getProjectDescriptors() {
        return descriptors.values();
    }

    public Collection<Property> getProperties() {
        // not supported
        return null;
    }

    public Property getProperty(String name) throws PropertyException {
        notSupportedProps();
        return null;
    }

    public Map<String, Object> getProps() {
        // not supported
        return null;
    }

    public ProjectVersion getVersion() {
        return activeProjectVersion.getVersion();
    }

    public Collection<ProjectVersion> getVersions() {
        return dtrDProject.getVersions();
    }

    public boolean hasArtefact(String name) {
        return false;
    }

    public boolean hasProperty(String name) {
        return false;
    }

    public boolean isCheckedOut() {
        if (activeProjectVersion.isLocked()) {
            WorkspaceUser lockedBy = activeProjectVersion.getlLockInfo().getLockedBy();

            if (lockedBy.equals(userWorkspace.getUser())) {
                return true;
            }
        }

        return false;
    }

    public boolean isDeleted() {
        return activeProjectVersion.isMarkedForDeletion();
    }

    public boolean isDeploymentProject() {
        return true;
    }

    public boolean isFolder() {
        return false;
    }

    public boolean isLocalOnly() {
        return false;
    }

    public boolean isLocked() {
        return dtrDProject.isLocked();
    }

    public boolean isLockedByMe() {
        if (!isLocked()) {
            return false;
        }

        WorkspaceUser lockedBy = dtrDProject.getlLockInfo().getLockedBy();
        return lockedBy.equals(userWorkspace.getUser());
    }

    public boolean isOpened() {
        return (activeProjectVersion != dtrDProject) || isCheckedOut();
    }

    public boolean isOpenedOtherVersion() {
        if (isCheckedOut()) {
            return false;
        }

        if (activeProjectVersion == dtrDProject) {
            return false;
        }

        ProjectVersion activeVersion = activeProjectVersion.getVersion();
        ProjectVersion max = dtrDProject.getVersion();

        return (!activeVersion.equals(max));
    }

    public boolean isReadOnly() {
        return !isCheckedOut();
    }

    public boolean isRulesProject() {
        return false;
    }

    protected void notSupported() throws ProjectException {
        throw new ProjectException("Not supported for deployment project");
    }

    protected void notSupportedProps() throws PropertyException {
        throw new PropertyException("Not supported for deployment project", null);
    }

    public void open() throws ProjectException {
        if (isCheckedOut()) {
            throw new ProjectException("Project ''{0}'' is checked-out!", null, getName());
        }

        check(PRIVILEGE_READ);

        if (isOpened()) {
            close();
        }

        activeProjectVersion = dtrDProject;
        refresh();
    }

    public void openVersion(CommonVersion version) throws ProjectException {
        if (isCheckedOut()) {
            throw new ProjectException("Deployment Project ''{0}'' is checked-out", null, getName());
        }

        if (isOpened()) {
            close();
        }

        // open specified version
        activeProjectVersion = userWorkspace.getDDProjectFor(dtrDProject, version);
        refresh();
    }

    protected void refresh() {
        updateDescriptors(activeProjectVersion.getProjectDescriptors());
    }

    protected void removeProjectDescriptor(UserWorkspaceProjectDescriptorImpl pd) {
        descriptors.remove(pd.getProjectName());
    }

    public Property removeProperty(String name) throws PropertyException {
        notSupportedProps();
        return null;
    }

    public void setDependencies(Collection<ProjectDependency> dependencies) {
        // not supported
    }

    public void setEffectiveDate(Date date) throws ProjectException {
        notSupported();
    }

    public void setExpirationDate(Date date) throws ProjectException {
        notSupported();
    }

    public void setLineOfBusiness(String lineOfBusiness) throws ProjectException {
        notSupported();
    }

    public void setProjectDescriptors(Collection<ProjectDescriptor> projectDescriptors) throws ProjectException {
        if (isReadOnly()) {
            throw new ProjectException("Cannot change deployment descriptor in read only mode");
        }
        updateDescriptors(projectDescriptors);
    }

    public void setProps(Map<String, Object> props) throws ProjectException {
        notSupported();
    }

    public void undelete() throws ProjectException {
        check(PRIVILEGE_EDIT_DEPLOYMENT);

        activeProjectVersion.undelete(userWorkspace.getUser());
    }

    protected void updateArtefact(RepositoryDDProject repositoryDDProject) {
        if (isCheckedOut()) {
            return;
        }

        if (isOpened()) {
            dtrDProject = repositoryDDProject;
        } else {
            dtrDProject = repositoryDDProject;
            activeProjectVersion = repositoryDDProject;
            refresh();
        }
    }

    protected void updateDescriptors(Collection<ProjectDescriptor> projectDescriptors) {
        HashMap<String, ProjectDescriptor> descrs = new HashMap<String, ProjectDescriptor>();

        for (ProjectDescriptor pd : projectDescriptors) {
            String name = pd.getProjectName();
            UserWorkspaceProjectDescriptorImpl uwpd = new UserWorkspaceProjectDescriptorImpl(this, name, pd
                    .getProjectVersion());
            descrs.put(name, uwpd);
        }

        descriptors.clear();
        descriptors = descrs;
    }
}
