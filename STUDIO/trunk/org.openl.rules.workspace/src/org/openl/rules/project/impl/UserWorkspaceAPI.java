package org.openl.rules.project.impl;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.repository.CommonUser;
import org.openl.rules.repository.CommonVersion;
import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.ProjectDependency;
import org.openl.rules.workspace.abstracts.ProjectDescriptor;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectVersion;
import org.openl.rules.workspace.dtr.LockInfo;
import org.openl.rules.workspace.dtr.impl.RepositoryProjectVersionImpl;
import org.openl.rules.workspace.props.Property;
import org.openl.rules.workspace.props.PropertyException;
import org.openl.rules.workspace.uw.UserWorkspace;

public class UserWorkspaceAPI implements ProjectArtefactAPI {
    private LocalAPI local;
    private RepositoryAPI repository;
    private ProjectArtefactAPI current;
    private UserWorkspace workspace;

    public UserWorkspaceAPI(LocalAPI local, RepositoryAPI repository, UserWorkspace workspace) {
        this.local = local;
        this.repository = repository;
        if (local != null) {
            current = local;
        } else {
            current = repository;
        }
        this.workspace = workspace;
    }
    
    public UserWorkspace getUserWorkspace(){
        return workspace;
    }
    
    public RepositoryAPI getRepositoryAPI(){
        return repository;
    }
    
    public LocalAPI getLocalAPI(){
        return local;
    }

    public Date getEffectiveDate() {
        return current.getEffectiveDate();
    }

    public Date getExpirationDate() {
        return current.getExpirationDate();
    }

    public String getLineOfBusiness() {
        return current.getLineOfBusiness();
    }

    public Map<String, Object> getProps() {
        return current.getProps();
    }

    public void setEffectiveDate(Date date) throws ProjectException {
        current.setEffectiveDate(date);
    }

    public void setExpirationDate(Date date) throws ProjectException {
        current.setExpirationDate(date);
    }

    public void setLineOfBusiness(String lineOfBusiness) throws ProjectException {
        current.setLineOfBusiness(lineOfBusiness);
    }

    public void setProps(Map<String, Object> props) throws ProjectException {
        current.setProps(props);
    }

    public ProjectArtefactAPI addFolder(String name) throws ProjectException {
        return current.addFolder(name);
    }

    public void addProperty(Property property) throws PropertyException {
        current.addProperty(property);
    }

    public ProjectArtefactAPI addResource(String name, ProjectArtefactAPI resource) throws ProjectException {
        return current.addResource(name, resource);
    }

    public ProjectArtefactAPI addResource(String name, InputStream content) throws ProjectException {
        return current.addResource(name, content);
    }

    public void commit(CommonUser user, int major, int minor) throws ProjectException {
        update(local, repository, user);
        repository.commit(user, major, minor);
        local.close(user);
        if (!isLocalOnly()) {
            current = repository;
        }
    }

    public void close(CommonUser user) throws ProjectException {
        if (local != null) {
            local.close(user);
        }
        if (!isLocalOnly()) {
            current = repository;
        }
    }

    public void delete(CommonUser user) throws ProjectException {
        current.delete(user);
    }

    public ProjectArtefactAPI getArtefact(String name) throws ProjectException {
        return current.getArtefact(name);
    }

    public ArtefactPath getArtefactPath() {
        return current.getArtefactPath();
    }

    public Collection<? extends ProjectArtefactAPI> getArtefacts() {
        return current.getArtefacts();
    }

    public InputStream getContent() throws ProjectException {
        return current.getContent();
    }

    public Collection<ProjectDependency> getDependencies() {
        return current.getDependencies();
    }

    public LockInfo getLockInfo() {
        if (repository != null) {
            return repository.getLockInfo();
        } else {
            return local.getLockInfo();
        }
    }

    public void lock(CommonUser user) throws ProjectException {
        repository.lock(user);
    }

    public void unlock(CommonUser user) throws ProjectException {
        repository.unlock(user);
    }

    public String getName() {
        return current.getName();
    }

    public Collection<Property> getProperties() {
        return current.getProperties();
    }

    public Property getProperty(String name) throws PropertyException {
        return current.getProperty(name);
    }

    public String getResourceType() {
        return current.getResourceType();
    }

    public void setResourceType(String type) {
        // TODO Auto-generated method stub
    }

    public ProjectVersion getVersion() {
        // TODO ???
        if (isOpened()) {
            return local.getVersion();
        } else {
            return repository.getVersion();
        }
    }

    public List<ProjectVersion> getVersions() {
        if (repository != null) {
            return repository.getVersions();
        } else {
            return local.getVersions();
        }
    }

    public boolean hasArtefact(String name) {
        return current.hasArtefact(name);
    }

    public boolean hasProperty(String name) {
        return current.hasProperty(name);
    }

    public boolean isLocalOnly() {
        return repository == null;
    }

    public boolean isOpened() {
        return current == local;
    }

    public boolean isFolder() {
        return current.isFolder();
    }

    public void openVersion(CommonVersion version) throws ProjectException {
        AProject openedProject  = workspace.getDesignTimeRepository().getProject(repository.getName(), version);
        File source;
        if (local == null) {
            ArtefactPath path = repository.getArtefactPath();
             source = new File(workspace.getLocalWorkspace().getLocation(), path.segment(path.segmentCount() - 1));
            local = new LocalAPI(source, path, workspace.getLocalWorkspace());
        }else{
            source = local.getSource();
        }
        source.mkdir();
        local.setCurrentVersion(new RepositoryProjectVersionImpl(version, null));
        update(openedProject.getAPI(), local, null);
        local.commit(null, 0, 0);//save persistence
        current = local;
    }

    // FIXME
    private void update(ProjectArtefactAPI from, ProjectArtefactAPI to, CommonUser user) throws ProjectException {
        new AProject(to, user).update(new AProject(from, user));
    }

    public Property removeProperty(String name) throws PropertyException {
        return current.removeProperty(name);
    }

    public void setContent(InputStream inputStream) throws ProjectException {
        current.setContent(inputStream);
    }

    public void setDependencies(Collection<ProjectDependency> dependencies) throws ProjectException {
        current.setDependencies(dependencies);
    }

    public Collection<ProjectDescriptor> getProjectDescriptors() {
        return repository.getProjectDescriptors();
    }

    public void setProjectDescriptors(Collection<ProjectDescriptor> projectDescriptors) throws ProjectException {
        repository.setProjectDescriptors(projectDescriptors);
    }
}
