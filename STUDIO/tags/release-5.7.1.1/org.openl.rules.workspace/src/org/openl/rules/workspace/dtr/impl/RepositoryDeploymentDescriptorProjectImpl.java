package org.openl.rules.workspace.dtr.impl;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.repository.CommonUser;
import org.openl.rules.repository.CommonVersion;
import org.openl.rules.repository.CommonVersionImpl;
import org.openl.rules.repository.RDeploymentDescriptorProject;
import org.openl.rules.repository.RProjectDescriptor;
import org.openl.rules.repository.RVersion;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.DeploymentDescriptorProject;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectDependency;
import org.openl.rules.workspace.abstracts.ProjectDescriptor;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectVersion;
import org.openl.rules.workspace.abstracts.impl.ArtefactPathImpl;
import org.openl.rules.workspace.dtr.LockInfo;
import org.openl.rules.workspace.dtr.RepositoryDDProject;
import org.openl.rules.workspace.dtr.RepositoryProjectArtefact;
import org.openl.rules.workspace.props.Property;
import org.openl.rules.workspace.props.PropertyException;

/**
 *
 * @author Aleh Bykhavets
 *
 */
public class RepositoryDeploymentDescriptorProjectImpl implements RepositoryDDProject {
    private class RPD2 implements RProjectDescriptor {
        private ProjectDescriptor pd;

        private RPD2(ProjectDescriptor pd) {
            this.pd = pd;
        }

        public String getProjectName() {
            return pd.getProjectName();
        }

        public RVersion getProjectVersion() {
            return new RV2(pd.getProjectVersion());
        }

        public void setProjectVersion(RVersion version) throws RRepositoryException {
            // do nothing
        }
    }

    private class RV2 implements RVersion {
        private CommonVersion version;

        private RV2(CommonVersion version) {
            this.version = version;
        }

        public int compareTo(CommonVersion o) {
            return new CommonVersionImpl(this).compareTo(o);
        }

        public Date getCreated() {
            return null;
        }

        public CommonUser getCreatedBy() {
            return null;
        }

        public int getMajor() {
            return version.getMajor();
        }

        public int getMinor() {
            return version.getMinor();
        }

        public int getRevision() {
            return version.getRevision();
        }

        public String getVersionName() {
            return version.getVersionName();
        }
    }

    private static final Log log = LogFactory.getLog(RepositoryDeploymentDescriptorProjectImpl.class);
    private RDeploymentDescriptorProject rulesDescrProject;

    private String name;

    private ArtefactPath path;

    private HashMap<String, ProjectDescriptor> descriptors;

    public RepositoryDeploymentDescriptorProjectImpl(RDeploymentDescriptorProject rulesDescrProject) {
        this.rulesDescrProject = rulesDescrProject;

        name = rulesDescrProject.getName();
        path = new ArtefactPathImpl(new String[] { name });

        descriptors = new HashMap<String, ProjectDescriptor>();

        for (RProjectDescriptor ralProjectDescriptor : rulesDescrProject.getProjectDescriptors()) {
            RepositoryProjectDescriptorImpl dtrProjectDescriptor = new RepositoryProjectDescriptorImpl(this,
                    ralProjectDescriptor);
            descriptors.put(dtrProjectDescriptor.getProjectName(), dtrProjectDescriptor);
        }
    }

    public ProjectDescriptor addProjectDescriptor(String projectName, CommonVersion version) throws ProjectException {
        if (descriptors.get(projectName) != null) {
            throw new ProjectException("Project Descriptor ''{0}'' already exists!", null, projectName);
        }

        RepositoryProjectDescriptorImpl dtrProjectDescriptor = new RepositoryProjectDescriptorImpl(this, projectName,
                version);
        descriptors.put(projectName, dtrProjectDescriptor);
        return dtrProjectDescriptor;
    }

    public void addProperty(Property property) throws PropertyException {
        notSupportedProps();
    }

    public void commit(Project source, CommonUser user) throws ProjectException {
        update(source);

        try {
            rulesDescrProject.commit(user);
        } catch (RRepositoryException e) {
            throw new ProjectException("Failed to commit changes!", e);
        }
    }

    public void delete() throws ProjectException {
        throw new ProjectException("Use delete(CommonUser) instead!");
    }

    public void delete(CommonUser user) throws ProjectException {
        if (isMarkedForDeletion()) {
            throw new ProjectException("Deployment project ''{0}'' is already marked for deletion!", null, getName());
        }

        try {
            rulesDescrProject.delete(user);
        } catch (RRepositoryException e) {
            throw new ProjectException("Cannot delete deployment project ''{0}''!", e, getName());
        }
    }

    protected void delete(String projectName) {
        descriptors.remove(projectName);
    }

    public void erase(CommonUser user) throws ProjectException {
        try {
            rulesDescrProject.erase(user);
        } catch (RRepositoryException e) {
            throw new ProjectException("Cannot erase deployment project ''{0}''!", e, getName());
        }
    }

    public RepositoryProjectArtefact getArtefact(String name) throws ProjectException {
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

    public Collection<RepositoryProjectArtefact> getArtefacts() {
        // not supported
        return null;
    }

    public Collection<ProjectDependency> getDependencies() {
        // not supported
        return null;
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

    public LockInfo getlLockInfo() {
        try {
            return new LockInfoImpl(rulesDescrProject.getLock());
        } catch (RRepositoryException e) {
            log.error("getLockInfo", e);
            return LockInfoImpl.NO_LOCK;
        }
    }

    public String getName() {
        return name;
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
        RVersion ralVersion = rulesDescrProject.getActiveVersion();
        RepositoryVersionInfoImpl info = new RepositoryVersionInfoImpl(ralVersion.getCreated(), ralVersion
                .getCreatedBy().getUserName());
        RepositoryProjectVersionImpl version = new RepositoryProjectVersionImpl(ralVersion, info);

        return version;
    }

    public Collection<ProjectVersion> getVersions() {
        LinkedList<ProjectVersion> vers = new LinkedList<ProjectVersion>();

        try {
            for (RVersion ralVersion : rulesDescrProject.getVersionHistory()) {
                RepositoryVersionInfoImpl dtrVersion = new RepositoryVersionInfoImpl(ralVersion.getCreated(),
                        ralVersion.getCreatedBy().getUserName());
                vers.add(new RepositoryProjectVersionImpl(ralVersion, dtrVersion));
            }
        } catch (RRepositoryException e) {
            log.error("Failed to get version history!", e);
            // empty or partial list will be returned
        }
        return vers;
    }

    public boolean hasArtefact(String name) {
        return false;
    }

    public boolean hasProperty(String name) {
        return false;
    }

    public boolean isFolder() {
        return false;
    }

    public boolean isLocked() {
        try {
            return rulesDescrProject.isLocked();
        } catch (RRepositoryException e) {
            log.error("isLocked", e);
            return false;
        }
    }

    public boolean isMarkedForDeletion() {
        try {
            return rulesDescrProject.isMarked4Deletion();
        } catch (RRepositoryException e) {
            log.error("isMarkedForDeletion", e);
            return false;
        }
    }

    public void lock(WorkspaceUser user) throws ProjectException {
        if (isLocked()) {
            throw new ProjectException("Deployment project ''{0}'' is already locked!", null, getName());
        }

        try {
            rulesDescrProject.lock(user);
        } catch (RRepositoryException e) {
            throw new ProjectException("Cannot lock deployment project: " + e.getMessage(), e);
        }
    }

    protected void notSupported() throws ProjectException {
        throw new ProjectException("Not supported for deployment project!");
    }

    protected void notSupportedProps() throws PropertyException {
        throw new PropertyException("Not supported for deployment project!", null);
    }

    public Property removeProperty(String name) throws PropertyException {
        notSupportedProps();
        return null;
    }

    public void riseVersion(int major, int minor) throws ProjectException {
        try {
            rulesDescrProject.riseVersion(major, minor);
        } catch (RRepositoryException e) {
            throw new ProjectException(e.getMessage(), e);
        }
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
        HashMap<String, ProjectDescriptor> newDescriptors = new HashMap<String, ProjectDescriptor>();

        for (ProjectDescriptor projectDescriptor : projectDescriptors) {
            CommonVersion projectVersion = projectDescriptor.getProjectVersion();
            RepositoryProjectVersionImpl dtrProjectVersion = new RepositoryProjectVersionImpl(projectVersion, null);
            RepositoryProjectDescriptorImpl dtrProjectDescriptor = new RepositoryProjectDescriptorImpl(this,
                    projectDescriptor.getProjectName(), dtrProjectVersion);

            newDescriptors.put(dtrProjectDescriptor.getProjectName(), dtrProjectDescriptor);
        }

        descriptors.clear();
        descriptors = newDescriptors;
    }

    public void setProps(Map<String, Object> props) throws ProjectException {
        notSupported();
    }

    public void undelete(CommonUser user) throws ProjectException {
        if (!isMarkedForDeletion()) {
            throw new ProjectException("Cannot undelete non-marked deployment project ''{0}''!", null, getName());
        }

        try {
            rulesDescrProject.undelete(user);
        } catch (RRepositoryException e) {
            throw new ProjectException("Cannot undelete deployment project ''{0}''!", e, name);
        }
    }

    public void unlock(WorkspaceUser user) throws ProjectException {
        if (!isLocked()) {
            throw new ProjectException("Cannot unlock non-locked deployment project ''{0}''!", null, getName());
        }

        try {
            rulesDescrProject.unlock(user);
        } catch (RRepositoryException e) {
            throw new ProjectException("Cannot unlock pdeployment roject: " + e.getMessage(), e);
        }
    }

    public void update(DeploymentDescriptorProject deploymentProject) throws ProjectException {
        descriptors.clear();

        for (ProjectDescriptor projectDescriptor : deploymentProject.getProjectDescriptors()) {
            descriptors.put(projectDescriptor.getProjectName(), projectDescriptor);
        }

        Collection<RProjectDescriptor> projectDescriptors = new LinkedList<RProjectDescriptor>();
        for (ProjectDescriptor projectDescriptor : descriptors.values()) {
            RPD2 substituteProjectDescriptor = new RPD2(projectDescriptor);
            projectDescriptors.add(substituteProjectDescriptor);
        }

        try {
            rulesDescrProject.setProjectDescriptors(projectDescriptors);
        } catch (RRepositoryException e) {
            throw new ProjectException("Cannot update descriptors for ''{0}''!", e, getName());
        }
    }

    public void update(ProjectArtefact srcArtefact) throws ProjectException {
        if (srcArtefact instanceof DeploymentDescriptorProject) {
            DeploymentDescriptorProject deploymentProject = (DeploymentDescriptorProject) srcArtefact;
            update(deploymentProject);
        }
    }
}
