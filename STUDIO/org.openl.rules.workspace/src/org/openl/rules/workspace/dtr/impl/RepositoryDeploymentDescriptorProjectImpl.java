package org.openl.rules.workspace.dtr.impl;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

import org.openl.rules.repository.RDeploymentDescriptorProject;
import org.openl.rules.repository.RProjectDescriptor;
import org.openl.rules.repository.RUser;
import org.openl.rules.repository.RVersion;
import org.openl.rules.repository.exceptions.RModifyException;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.workspace.abstracts.DeploymentDescriptorProject;
import org.openl.rules.workspace.abstracts.ProjectDescriptor;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectVersion;

public class RepositoryDeploymentDescriptorProjectImpl implements DeploymentDescriptorProject {
    private RDeploymentDescriptorProject rulesDescrProject;

    private String name;
    private HashMap<String, ProjectDescriptor> descriptors;
    
    public RepositoryDeploymentDescriptorProjectImpl(RDeploymentDescriptorProject rulesDescrProject) {
        this.rulesDescrProject = rulesDescrProject;
    
        name = rulesDescrProject.getName();
        
        descriptors = new HashMap<String, ProjectDescriptor>();
        
        for (RProjectDescriptor pd : rulesDescrProject.getProjectDescriptors()) {
            RepositoryProjectDescriptorImpl rpd = new RepositoryProjectDescriptorImpl(this, pd);
            descriptors.put(rpd.getProjectName(), rpd);
        }
    }
    
    public ProjectDescriptor createProjectDescriptor(String name) throws ProjectException {
        if (descriptors.get(name) != null) {
            throw new ProjectException("Project Descriptor {0} already exists", null, name);
        }

        RepositoryProjectVersionImpl rpv = new RepositoryProjectVersionImpl(0, 0, 0, null);
        RepositoryProjectDescriptorImpl pd = new RepositoryProjectDescriptorImpl(this, name, rpv);
        descriptors.put(name, pd);
        return pd;
    }

    public Collection<ProjectDescriptor> getProjectDescriptors() {
        return descriptors.values();
    }

    public void setProjectDescriptors(Collection<ProjectDescriptor> projectDescriptors) throws ProjectException {
        HashMap<String, ProjectDescriptor> newDescr = new HashMap<String, ProjectDescriptor>();
        
        for (ProjectDescriptor pd : projectDescriptors) {
            ProjectVersion pv = pd.getProjectVersion();
            RepositoryProjectVersionImpl rpv = new RepositoryProjectVersionImpl(pv.getMajor(), pv.getMinor(), pv.getRevision(), null);
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
    
    public void update() throws ProjectException {
        Collection<RProjectDescriptor> projectDescriptors = new LinkedList<RProjectDescriptor>();
        for (ProjectDescriptor pd : descriptors.values()) {
            RPD2 rpd = new RPD2(pd);
            projectDescriptors.add(rpd);
        }
        
        try {
            rulesDescrProject.setProjectDescriptors(projectDescriptors);
            rulesDescrProject.commit();
        } catch (RRepositoryException e) {
            throw new ProjectException("Cannot update deployment project {0}", e, name);
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

        public void setProjectVersion(RVersion version) throws RModifyException {
            // do nothing
        }
    }
    
    private class RV2 implements RVersion {
        private ProjectVersion pv;
        
        private RV2 (ProjectVersion pv) {
            this.pv = pv;
        }

        public Date getCreated() {
            return null;
        }

        public RUser getCreatedBy() {
            return null;
        }

        public int getMajor() {
            return pv.getMajor();
        }

        public int getMinor() {
            return pv.getMinor();
        }

        public String getName() {
            return pv.getVersionName();
        }

        public int getRevision() {
            return pv.getRevision();
        }
    }
}
