package org.openl.rules.project.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.repository.CommonUser;
import org.openl.rules.repository.CommonVersion;
import org.openl.rules.repository.CommonVersionImpl;
import org.openl.rules.repository.RCommonProject;
import org.openl.rules.repository.RDependency;
import org.openl.rules.repository.RDeploymentDescriptorProject;
import org.openl.rules.repository.REntity;
import org.openl.rules.repository.RFile;
import org.openl.rules.repository.RFolder;
import org.openl.rules.repository.RProject;
import org.openl.rules.repository.RProjectDescriptor;
import org.openl.rules.repository.RProperty;
import org.openl.rules.repository.RPropertyType;
import org.openl.rules.repository.RVersion;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.ProjectDependency;
import org.openl.rules.workspace.abstracts.ProjectDescriptor;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectVersion;
import org.openl.rules.workspace.abstracts.impl.ProjectDependencyImpl;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.LockInfo;
import org.openl.rules.workspace.dtr.impl.LockInfoImpl;
import org.openl.rules.workspace.dtr.impl.RepositoryProjectDescriptorImpl;
import org.openl.rules.workspace.dtr.impl.RepositoryProjectVersionImpl;
import org.openl.rules.workspace.dtr.impl.RepositoryVersionInfoImpl;
import org.openl.rules.workspace.props.Property;
import org.openl.rules.workspace.props.PropertyException;
import org.openl.rules.workspace.props.ValueType;
import org.openl.rules.workspace.props.impl.PropertyImpl;

public class RepositoryAPI implements ProjectArtefactAPI {
    private static final Log log = LogFactory.getLog(RepositoryAPI.class);
    private REntity node;
    private ArtefactPath path;
    private DesignTimeRepository repository;

    public RepositoryAPI(REntity node, ArtefactPath path, DesignTimeRepository repository) {
        this.node = node;
        this.path = path;
        this.repository = repository;
    }

    public Date getEffectiveDate() {
        return node.getEffectiveDate();
    }

    public Date getExpirationDate() {
        return node.getExpirationDate();
    }

    public String getLineOfBusiness() {
        return node.getLineOfBusiness();
    }

    public Map<String, Object> getProps() {
        return node.getProps();
    }

    public void setEffectiveDate(Date date) throws ProjectException {
        try {
            node.setEffectiveDate(date);
        } catch (RRepositoryException e) {
            throw new ProjectException("Failed to set Effective Date", e);
        }
    }

    public void setExpirationDate(Date date) throws ProjectException {
        try {
            node.setExpirationDate(date);
        } catch (RRepositoryException e) {
            throw new ProjectException("Failed to set Expiration Date", e);
        }
    }

    public void setLineOfBusiness(String lineOfBusiness) throws ProjectException {
        try {
            node.setLineOfBusiness(lineOfBusiness);
        } catch (RRepositoryException e) {
            throw new ProjectException("Failed to set Lines Of Business", e);
        }
    }

    public void setProps(Map<String, Object> props) throws ProjectException {
        try {
            node.setProps(props);
        } catch (RRepositoryException e) {
            throw new ProjectException("Failed to set props", e);
        }
    }

    private RepositoryAPI wrapFile(RFile file) {
        ArtefactPath ap = getArtefactPath().withSegment(file.getName());

        return new RepositoryAPI(file, ap, repository);
    }

    private RepositoryAPI wrapFolder(RFolder folder) {
        ArtefactPath ap = getArtefactPath().withSegment(folder.getName());

        return new RepositoryAPI(folder, ap, repository);
    }

    private RFolder getFolder() throws ProjectException {
        if (node instanceof RFolder) {
            return (RFolder) node;
        }
        if (node instanceof RProject) {
            return ((RProject) node).getRootFolder();
        }
        throw new ProjectException("This operation is only allowed for folders or projects");
    }

    public RepositoryAPI addFolder(String name) throws ProjectException {
        RFolder rf;
        try {
            rf = getFolder().createFolder(name);
        } catch (RRepositoryException e) {
            throw new ProjectException("Failed to add folder.", e);
        }
        return wrapFolder(rf);
    }

    public void addProperty(Property property) throws PropertyException {
        try {
            node.addProperty(property.getName(), RPropertyType.valueOf(property.getType().name()), property.getValue());
        } catch (RRepositoryException e) {
            throw new PropertyException("Failed to set property.", e);
        }
    }

    public RepositoryAPI addResource(String name, ProjectArtefactAPI resource) throws ProjectException {
        return addResource(name, resource.getContent());
    }

    public RepositoryAPI addResource(String name, InputStream content) throws ProjectException {
        RFile rf;
        try {
            rf = getFolder().createFile(name);
        } catch (RRepositoryException e) {
            throw new ProjectException("Failed to add folder.", e);
        }
        RepositoryAPI res = new RepositoryAPI(rf, path.withSegment(name), repository);
        res.setContent(content);
        return res;
    }

    public void commit(CommonUser user, int major, int minor) throws ProjectException {
        try {
            ((RCommonProject) node).riseVersion(major, minor);
            ((RCommonProject) node).commit(user);
            ((RCommonProject) node).unlock(user);
        } catch (RRepositoryException e) {
            throw new ProjectException("Failed to commit project", e);
        }
    }

    public void close(CommonUser user) throws ProjectException {
        // TODO open last version?
        try {
            ((RCommonProject) node).unlock(user);
        } catch (RRepositoryException e) {
            throw new ProjectException("Failed to commit project", e);
        }
    }

    public void delete(CommonUser user) throws ProjectException {
        try {
            if(node instanceof RCommonProject){
                ((RCommonProject)node).erase(user);
            }else{
                node.delete();
            }
        } catch (RRepositoryException e) {
            throw new ProjectException("Failed to delete node.", e);
        }
    }

    public RepositoryAPI getArtefact(String name) throws ProjectException {
        RRepositoryException rre = null;
        RFolder rulesFolder = getFolder();

        try {
            for (RFolder f : rulesFolder.getFolders()) {
                if (name.equals(f.getName())) {
                    return wrapFolder(f);
                }
            }
            for (RFile f : rulesFolder.getFiles()) {
                if (name.equals(f.getName())) {
                    return wrapFile(f);
                }
            }
        } catch (RRepositoryException e) {
            rre = e;
        }

        throw new ProjectException("Cannot find project artefact ''{0}''!", rre, name);
    }

    public ArtefactPath getArtefactPath() {
        return path;
    }

    public Collection<RepositoryAPI> getArtefacts() {
        List<RepositoryAPI> result = new LinkedList<RepositoryAPI>();
        RFolder rulesFolder;
        try {
            rulesFolder = getFolder();
        } catch (ProjectException e1) {
            return result;
        }

        try {
            for (RFolder rf : rulesFolder.getFolders()) {
                result.add(wrapFolder(rf));
            }
            for (RFile rf : rulesFolder.getFiles()) {
                result.add(wrapFile(rf));
            }
        } catch (RRepositoryException e) {
            log.error("Cannot get artefacts!", e);
        }

        return result;
    }

    public InputStream getContent() throws ProjectException {
        try {
            return ((RFile) node).getContent();
        } catch (RRepositoryException e) {
            throw new ProjectException("Failed to get content.", e);
        }
    }

    public Collection<ProjectDependency> getDependencies() {
        LinkedList<ProjectDependency> result = new LinkedList<ProjectDependency>();
        if (node instanceof RProject) {
            RProject rulesProject = (RProject) node;
            try {
                for (RDependency rDep : rulesProject.getDependencies()) {
                    String projectName = rDep.getProjectName();

                    ProjectVersion lowVer = new RepositoryProjectVersionImpl(rDep.getLowerLimit(), null);

                    ProjectVersion upVer = null;
                    CommonVersion dependencyUpperLimit = rDep.getUpperLimit();
                    if (dependencyUpperLimit != null) {
                        upVer = new RepositoryProjectVersionImpl(dependencyUpperLimit, null);
                    }

                    ProjectDependency pd = new ProjectDependencyImpl(projectName, lowVer, upVer);

                    result.add(pd);
                }
            } catch (RRepositoryException e) {
                log.error("Cannot get dependencies!", e);
            }
        }

        return result;
    }

    public LockInfo getLockInfo() {
        try {
            RCommonProject project = (RCommonProject) node;
            return new LockInfoImpl(project.getLock());
        } catch (RRepositoryException e) {
            log.error("getLockInfo", e);
            return LockInfoImpl.NO_LOCK;
        }
    }

    public void lock(CommonUser user) throws ProjectException {
        try {
            ((RCommonProject) node).lock(user);
        } catch (RRepositoryException e) {
            throw new ProjectException("Failed to lock node.", e);
        }
    }

    public void unlock(CommonUser user) throws ProjectException {
        try {
            ((RCommonProject) node).unlock(user);
        } catch (RRepositoryException e) {
            throw new ProjectException("Failed to unlock node.", e);
        }
    }

    public String getName() {
        return node.getName();
    }

    public Collection<Property> getProperties() {
        //FIXME not supported
        List<Property> properties = new ArrayList<Property>();
        return properties;
    }

    public Property getProperty(String name) throws PropertyException {
        try {
            RProperty property = node.getProperty(name);
            return new PropertyImpl(name, ValueType.valueOf(property.getType().name()), property.getValue());
        } catch (RRepositoryException e) {
            throw new PropertyException("Failed to get property.", e);
        }
    }

    public String getResourceType() {
        return ((RFile) node).getMimeType();
    }

    public ProjectVersion getVersion() {
        RVersion rv = node.getActiveVersion();
        RepositoryVersionInfoImpl rvii = new RepositoryVersionInfoImpl(rv.getCreated(), rv.getCreatedBy().getUserName());

        return new RepositoryProjectVersionImpl(rv, rvii);
    }

    public List<ProjectVersion> getVersions() {
        LinkedList<ProjectVersion> vers = new LinkedList<ProjectVersion>();

        try {
            for (RVersion rv : node.getVersionHistory()) {
                RepositoryVersionInfoImpl rvii = new RepositoryVersionInfoImpl(rv.getCreated(), rv.getCreatedBy()
                        .getUserName());
                vers.add(new RepositoryProjectVersionImpl(rv, rvii));
            }

        } catch (RRepositoryException e) {
            log.error("Failed to get version history!", e);
        }
        return vers;
    }

    public boolean hasArtefact(String name) {
        try {
            getArtefact(name);
            return true;
        } catch (ProjectException e) {
            if (log.isTraceEnabled()) {
                // if there is no artefact with such name it will catch
                // exception
                log.trace("hasArtefact", e);
            }
            return false;
        }
    }

    public boolean hasProperty(String name) {
        return node.hasProperty(name);
    }

    public boolean isLocalOnly() {
        return false;
    }

    public boolean isOpened() {
        // TODO is it true?
        return false;
    }

    public boolean isFolder() {
        return !(node instanceof RFile);
    }

    public void openVersion(CommonVersion version) throws ProjectException {
        // FIXME
        node = ((RepositoryAPI) repository.getProject(getName(), version).getAPI()).node;
    }

    public Property removeProperty(String name) throws PropertyException {
        try {
            RProperty property = node.getProperty(name);
            node.removeProperty(name);
            return new PropertyImpl(name, ValueType.valueOf(property.getType().name()), property.getValue());
        } catch (RRepositoryException e) {
            throw new PropertyException("Failed to remove property.", e);
        }
    }

    public void setContent(InputStream inputStream) throws ProjectException {
        try {
            ((RFile) node).setContent(inputStream);
        } catch (RRepositoryException e) {
            throw new ProjectException("Failed to set content.", e);
        }
    }

    public void setDependencies(Collection<ProjectDependency> dependencies) throws ProjectException {
        if (node instanceof RProject) {
            try {
                LinkedList<RDependency> newDeps = new LinkedList<RDependency>();
                for (ProjectDependency pd : dependencies) {
                    newDeps.add(pd);
                }

                ((RProject) node).setDependencies(newDeps);
            } catch (RRepositoryException e) {
                throw new ProjectException("Cannot update dependencies!", e);
            }
        }
    }

    public Collection<ProjectDescriptor> getProjectDescriptors() {
        List<ProjectDescriptor> descriptors = new ArrayList<ProjectDescriptor>();

        for (RProjectDescriptor ralProjectDescriptor : ((RDeploymentDescriptorProject) node).getProjectDescriptors()) {
            RepositoryProjectDescriptorImpl dtrProjectDescriptor = new RepositoryProjectDescriptorImpl(
                    ralProjectDescriptor);
            descriptors.add(dtrProjectDescriptor);
        }
        return descriptors;
    }

    public void setProjectDescriptors(Collection<ProjectDescriptor> projectDescriptors) throws ProjectException {
        List<RProjectDescriptor> descriptors = new ArrayList<RProjectDescriptor>();
        for (ProjectDescriptor descriptor : projectDescriptors) {
            descriptors.add(new RPD2(descriptor));
        }
        try {
            ((RDeploymentDescriptorProject) node).setProjectDescriptors(descriptors);
        } catch (RRepositoryException e) {
            throw new ProjectException("Failed to set project descriptors", e);
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
}
