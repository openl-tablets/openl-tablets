package org.openl.rules.project.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.repository.CommonUser;
import org.openl.rules.repository.CommonVersion;
import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.ProjectDependency;
import org.openl.rules.workspace.abstracts.ProjectDescriptor;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectVersion;
import org.openl.rules.workspace.dtr.LockInfo;
import org.openl.rules.workspace.dtr.SimpleLockInfo;
import org.openl.rules.workspace.dtr.impl.RepositoryProjectVersionImpl;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.openl.rules.workspace.lw.impl.LocalWorkspaceImpl;
import org.openl.rules.workspace.props.Property;
import org.openl.rules.workspace.props.PropertyException;
import org.openl.rules.workspace.props.impl.PropertiesContainerImpl;

public class LocalAPI implements ProjectArtefactAPI {
    private File source;
    private PropertiesContainerImpl properties;
    private Date effectiveDate;
    private Date expirationDate;
    private String LOB;
    private Map<String, Object> props;
    private ArtefactPath path;
    private ProjectVersion currentVersion;
    private List<ProjectVersion> versions;
    private Collection<ProjectDependency> dependencies;
    private LocalWorkspace workspace;

    public LocalAPI(File source, ArtefactPath path, LocalWorkspace workspace) {
        this.source = source;
        this.path = path;
        this.workspace = workspace;
        currentVersion = new RepositoryProjectVersionImpl(0, 0, 0, null);
        properties = new PropertiesContainerImpl();
    }

    public File getSource() {
        return source;
    }

    public Date getEffectiveDate() {
        return effectiveDate;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public String getLineOfBusiness() {
        return LOB;
    }

    public Map<String, Object> getProps() {
        return props;
    }

    public void setEffectiveDate(Date date) throws ProjectException {
        this.effectiveDate = date;
    }

    public void setExpirationDate(Date date) throws ProjectException {
        this.expirationDate = date;
    }

    public void setLineOfBusiness(String lineOfBusiness) throws ProjectException {
        this.LOB = lineOfBusiness;
    }

    public void setProps(Map<String, Object> props) throws ProjectException {
        this.props = props;
    }

    public LocalAPI addFolder(String name) throws ProjectException {
        File newFolder = new File(source, name);
        newFolder.mkdir();
        LocalAPI aProjectFolder = new LocalAPI(newFolder, path.withSegment(name), workspace);
        return aProjectFolder;
    }

    public void addProperty(Property property) throws PropertyException {
        properties.addProperty(property);
    }

    public LocalAPI addResource(String name, ProjectArtefactAPI resource) throws ProjectException {
        return addResource(name, resource.getContent());
    }

    public LocalAPI addResource(String name, InputStream content) throws ProjectException {
        File newFile = new File(source, name);
        try {
            newFile.createNewFile();
            LocalAPI newResource = new LocalAPI(newFile, path.withSegment(name), workspace);
            newResource.setContent(content);
            return newResource;
        } catch (IOException e) {
            throw new ProjectException("Failed to create resource", e);
        }
    }

    public void commit(CommonUser user, int major, int minor) throws ProjectException {
        // FIXME
        new StatePersistance(new AProject(this, null), source).save();
    }

    public void close(CommonUser user) throws ProjectException {
        delete(user);
    }

    public void delete(CommonUser user) throws ProjectException {
        try {
            if (source.isFile()) {
                source.delete();
            } else {
                FolderHelper.deleteFolder(source);
            }
        } catch (Exception e) {
            throw new ProjectException("Failed to delete local resourse", e);
        }
    }

    public LocalAPI getArtefact(String name) throws ProjectException {
        File[] files = source.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().equals(name)) {
                    return new LocalAPI(file, path.withSegment(name), workspace);
                }
            }
        }
        throw new ProjectException(String.format("Artefact with name \"%s\" is" + " not found", name));
    }

    public ArtefactPath getArtefactPath() {
        return path;
    }

    public Collection<LocalAPI> getArtefacts() {
        List<LocalAPI> artefacts = new ArrayList<LocalAPI>();
        File[] files = source.listFiles(((LocalWorkspaceImpl)workspace).getLocalWorkspaceFileFilter());
        if (files != null) {
            for (File file : files) {
                artefacts.add(new LocalAPI(file, path.withSegment(file.getName()), workspace));
            }
        }
        return artefacts;
    }

    public InputStream getContent() throws ProjectException {
        try {
            return new FileInputStream(source);
        } catch (FileNotFoundException e) {
            throw new ProjectException("Failed to get content.", e);
        }
    }

    public Collection<ProjectDependency> getDependencies() {
        if (dependencies == null) {
            dependencies = new ArrayList<ProjectDependency>();
        }
        return dependencies;
    }

    public LockInfo getLockInfo() {
        return new SimpleLockInfo(null, null);
    }

    public void lock(CommonUser user) throws ProjectException {
        // TODO Auto-generated method stub
    }

    public void unlock(CommonUser user) throws ProjectException {
        // TODO Auto-generated method stub
    }

    public String getName() {
        return source.getName();
    }

    public Collection<Property> getProperties() {
        return properties.getProperties();
    }

    public Property getProperty(String name) throws PropertyException {
        return properties.getProperty(name);
    }

    public String getResourceType() {
        // TODO
        return "unknown";
    }

    public ProjectVersion getVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(ProjectVersion currentVersion) {
        this.currentVersion = currentVersion;
    }

    public List<ProjectVersion> getVersions() {
        if (versions == null) {
            versions = new ArrayList<ProjectVersion>();
        }
        return versions;
    }

    public void setVersions(List<ProjectVersion> versions) {
        this.versions = versions;
    }

    public boolean hasArtefact(String name) {
        File[] files = source.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasProperty(String name) {
        return properties.hasProperty(name);
    }

    public boolean isLocalOnly() {
        return true;
    }

    public boolean isOpened() {
        return true;
    }

    public boolean isFolder() {
        return source.isDirectory();
    }

    public void openVersion(CommonVersion version) throws ProjectException {
        // FIXME
        new StatePersistance(new AProject(this, null), source).load();
    }

    public Property removeProperty(String name) throws PropertyException {
        return properties.removeProperty(name);
    }

    public void setContent(InputStream inputStream) throws ProjectException {
        try {
            IOUtils.copy(inputStream, new FileOutputStream(source));
        } catch (IOException e) {
            throw new ProjectException("Failed to set content.", e);
        }
    }

    public void setDependencies(Collection<ProjectDependency> dependencies) throws ProjectException {
        this.dependencies = dependencies;
    }

    public Collection<ProjectDescriptor> getProjectDescriptors() {
        //TODO
        return null;
    }

    public void setProjectDescriptors(Collection<ProjectDescriptor> projectDescriptors) throws ProjectException {
        //TODO
    }
}
