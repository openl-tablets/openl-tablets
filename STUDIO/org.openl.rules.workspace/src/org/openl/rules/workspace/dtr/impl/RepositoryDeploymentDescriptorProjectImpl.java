package org.openl.rules.workspace.dtr.impl;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

import org.openl.rules.repository.CommonUser;
import org.openl.rules.repository.CommonVersion;
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
import org.openl.util.Log;

public class RepositoryDeploymentDescriptorProjectImpl implements RepositoryDDProject {
    private RDeploymentDescriptorProject rulesDescrProject;

    private String name;
    private ArtefactPath path;
    
    private HashMap<String, ProjectDescriptor> descriptors;
    
    public RepositoryDeploymentDescriptorProjectImpl(RDeploymentDescriptorProject rulesDescrProject) {
        this.rulesDescrProject = rulesDescrProject;
    
        name = rulesDescrProject.getName();
        path = new ArtefactPathImpl(new String[]{name});
        
        descriptors = new HashMap<String, ProjectDescriptor>();
        
        for (RProjectDescriptor pd : rulesDescrProject.getProjectDescriptors()) {
            RepositoryProjectDescriptorImpl rpd = new RepositoryProjectDescriptorImpl(this, pd);
            descriptors.put(rpd.getProjectName(), rpd);
        }
    }
    
    public ProjectDescriptor addProjectDescriptor(String name, CommonVersion version) throws ProjectException {
        if (descriptors.get(name) != null) {
            throw new ProjectException("Project Descriptor {0} already exists", null, name);
        }

        RepositoryProjectDescriptorImpl pd = new RepositoryProjectDescriptorImpl(this, name, version);
        descriptors.put(name, pd);
        return pd;
    }

    public Collection<ProjectDescriptor> getProjectDescriptors() {
        return descriptors.values();
    }

    public void setProjectDescriptors(Collection<ProjectDescriptor> projectDescriptors) throws ProjectException {
        HashMap<String, ProjectDescriptor> newDescr = new HashMap<String, ProjectDescriptor>();
        
        for (ProjectDescriptor pd : projectDescriptors) {
            CommonVersion pv = pd.getProjectVersion();
            RepositoryProjectVersionImpl rpv = new RepositoryProjectVersionImpl(pv, null);
            RepositoryProjectDescriptorImpl rpd = new RepositoryProjectDescriptorImpl(this, pd.getProjectName(), rpv);
            
            newDescr.put(rpd.getProjectName(), rpd);
        }
        
        descriptors.clear();
        descriptors = newDescr;
    }

    public String getName() {
        return name;
    }
    
    protected void delete(String projectName) {
        descriptors.remove(projectName);
    }
    
    public ProjectVersion getVersion() {
        RVersion rv = rulesDescrProject.getActiveVersion();
        RepositoryVersionInfoImpl info = new RepositoryVersionInfoImpl(rv.getCreated(), rv.getCreatedBy().getUserName());
        RepositoryProjectVersionImpl version = new RepositoryProjectVersionImpl(rv, info);

        return version;
    }

    public ArtefactPath getArtefactPath() {
        return path;
    }

    public Collection<ProjectVersion> getVersions() {
        LinkedList<ProjectVersion> vers = new LinkedList<ProjectVersion>();
        
        try {
            for (RVersion rv : rulesDescrProject.getVersionHistory()) {
                RepositoryVersionInfoImpl rvii = new RepositoryVersionInfoImpl(rv.getCreated(), rv.getCreatedBy().getUserName());
                vers.add(new RepositoryProjectVersionImpl(rv, rvii));
            }
        } catch (RRepositoryException e) {
            Log.error("Failed to get version history", e);
        }
        return vers;
    }

    public void update(DeploymentDescriptorProject ddp) throws ProjectException {
        descriptors.clear();
        
        for (ProjectDescriptor pd : ddp.getProjectDescriptors()) {
            descriptors.put(pd.getProjectName(), pd);
        }

        Collection<RProjectDescriptor> projectDescriptors = new LinkedList<RProjectDescriptor>();
        for (ProjectDescriptor pd : descriptors.values()) {
            RPD2 rpd = new RPD2(pd);
            projectDescriptors.add(rpd);
        }

        try {
            rulesDescrProject.setProjectDescriptors(projectDescriptors);
        } catch (RRepositoryException e) {
            throw new ProjectException("Cannot update descriptors for {0}", e, name);
        }        
    }

    public void update(ProjectArtefact srcArtefact) throws ProjectException {
        if (srcArtefact instanceof DeploymentDescriptorProject) {
            DeploymentDescriptorProject ddp = (DeploymentDescriptorProject) srcArtefact;
            update(ddp);
        }
    }

    public void commit(Project source, CommonUser user) throws ProjectException {
        update(source);

        try {
            rulesDescrProject.commit(user);
        } catch (RRepositoryException e) {
            throw new ProjectException("Failed to commit changes", e);
        }        
    }

    public void delete(CommonUser user) throws ProjectException {
        if (isMarkedForDeletion()) {
            throw new ProjectException("Project ''{0}'' is already marked for deletion", null, getName());
        }

        try {
            rulesDescrProject.delete(user);
        } catch (RRepositoryException e) {
            throw new ProjectException("Cannot delete project {0}", e, name);
        }        
    }

    public void erase(CommonUser user) throws ProjectException {
        try {
            rulesDescrProject.erase(user);
        } catch (RRepositoryException e) {
            throw new ProjectException("Cannot erase project {0}", e, name);
        }        
    }

    public boolean isLocked() {
        try {
            return rulesDescrProject.isLocked();
        } catch (RRepositoryException e) {
            Log.error(e);
            return false;
        }        
    }

    public LockInfo getlLockInfo() {
        try {
            return new LockInfoImpl(rulesDescrProject.getLock());
        } catch (RRepositoryException e) {
            Log.error(e);
            return LockInfoImpl.NO_LOCK;
        }        
    }

    public boolean isMarkedForDeletion() {
        try {
            return rulesDescrProject.isMarked4Deletion();
        } catch (RRepositoryException e) {
            Log.error("isMarkedForDeletion", e);
            return false;
        }
    }

    public void lock(WorkspaceUser user) throws ProjectException {
        if (isLocked()) {
            throw new ProjectException("Project ''{0}'' is already locked", null, getName());
        }

        try {
            rulesDescrProject.lock(user);
        } catch (RRepositoryException e) {
            throw new ProjectException("Cannot lock project: " + e.getMessage(), e);
        }        
    }

    public void undelete(CommonUser user) throws ProjectException {
        if (!isMarkedForDeletion()) {
            throw new ProjectException("Cannot undelete non-marked project ''{0}''", null, getName());
        }

        try {
            rulesDescrProject.undelete(user);
        } catch (RRepositoryException e) {
            throw new ProjectException("Cannot undelete project {0}", e, name);
        }        
    }

    public void unlock(WorkspaceUser user) throws ProjectException {
        if (!isLocked()) {
            throw new ProjectException("Cannot unlock non-locked project ''{0}''", null, getName());
        }

        try {
            rulesDescrProject.unlock(user);
        } catch (RRepositoryException e) {
            throw new ProjectException("Cannot unlock project: " + e.getMessage(), e);
        }        
    }
    
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
        
        private RV2 (CommonVersion version) {
            this.version = version;
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

        public String getVersionName() {
            return version.getVersionName();
        }

        public int getRevision() {
            return version.getRevision();
        }
    }


    
    public ProjectArtefact getArtefactByPath(ArtefactPath artefactPath) throws ProjectException {
        notSupported();
        return null;
    }

    public Collection<ProjectDependency> getDependencies() {
        // not supported
        return null;
    }


    public void setDependencies(Collection<ProjectDependency> dependencies) {
        // not supported
    }

    public Collection<RepositoryProjectArtefact> getArtefacts() {
        // not supported
        return null;
    }

    public RepositoryProjectArtefact getArtefact(String name) throws ProjectException {
        notSupported();
        return null;
    }


    public boolean hasArtefact(String name) {
        return false;
    }

    public boolean isFolder() {
        return false;
    }

    public void addProperty(Property property) throws PropertyException {
        notSupportedProps();
    }

    public Collection<Property> getProperties() {
        // not supported
        return null;
    }

    public Property getProperty(String name) throws PropertyException {
        notSupportedProps();
        return null;
    }

    public boolean hasProperty(String name) {
        return false;
    }

    public Property removeProperty(String name) throws PropertyException {
        notSupportedProps();
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

    public void setEffectiveDate(Date date) throws ProjectException {
        notSupported();
    }

    public void setExpirationDate(Date date) throws ProjectException {
        notSupported();
    }

    public void setLineOfBusiness(String lineOfBusiness) throws ProjectException {
        notSupported();
    }

    protected void notSupported() throws ProjectException {
        throw new ProjectException("Not supported for deployment project");
    }

    protected void notSupportedProps() throws PropertyException {
        throw new PropertyException("Not supported for deployment project", null);
    }

    public void delete() throws ProjectException {
        throw new ProjectException("Use delete(CommonUser) instead");
    }
}
