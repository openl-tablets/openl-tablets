package org.openl.rules.project.impl.local;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.openl.rules.common.ArtefactPath;
import org.openl.rules.common.CommonUser;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.LockInfo;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.Property;
import org.openl.rules.common.PropertyException;
import org.openl.rules.common.ValueType;
import org.openl.rules.common.impl.PropertyImpl;
import org.openl.rules.common.impl.RepositoryProjectVersionImpl;
import org.openl.rules.repository.api.ArtefactAPI;
import org.openl.rules.workspace.dtr.impl.LockInfoImpl;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.openl.rules.workspace.lw.impl.StateHolder;
import org.openl.rules.workspace.props.impl.PropertiesContainerImpl;

public class LocalArtefactAPI implements ArtefactAPI {
    protected File source;
    private PropertiesContainerImpl properties;
    private Map<String, Object> props;
    protected ArtefactPath path;
    private ProjectVersion currentVersion;
    private List<ProjectVersion> versions;
    protected LocalWorkspace workspace;

    public LocalArtefactAPI(File source, ArtefactPath path, LocalWorkspace workspace) {
        this.source = source;
        this.path = path;
        this.workspace = workspace;
        currentVersion = new RepositoryProjectVersionImpl(0, 0, 0, null);
        properties = new PropertiesContainerImpl();
    }

    public StateHolder getStateHolder() {
        ArtefactStateHolder state = new ArtefactStateHolder();

        state.version = getVersion();
        state.props = getProps();

        state.properties = new ArrayList<Property>();
        state.properties.addAll(getProperties());
        return state;
    }

    public void applyStateHolder(StateHolder stateHolder) {
        if (stateHolder instanceof ArtefactStateHolder) {
            ArtefactStateHolder state = (ArtefactStateHolder) stateHolder;
            this.currentVersion = state.version;
            this.props = state.props;
            for (Property property : state.properties) {
                try {
                    this.properties.addProperty(property);
                } catch (PropertyException e) {
                    // TODO: log
                }
            }
        } else {
            // TODO: log fail
        }
    }

    public File getSource() {
        return source;
    }

    public Map<String, Object> getProps() {
        return props;
    }

    public void setProps(Map<String, Object> props) throws PropertyException {
        this.props = props;
    }

    public void addProperty(String name, ValueType type, Object value) throws PropertyException {
        properties.addProperty(new PropertyImpl(name, type, value));
    }

    public Collection<Property> getProperties() {
        return properties.getProperties();
    }

    public Property getProperty(String name) throws PropertyException {
        return properties.getProperty(name);
    }

    public boolean hasProperty(String name) {
        return properties.hasProperty(name);
    }

    public Property removeProperty(String name) throws PropertyException {
        return properties.removeProperty(name);
    }

    public void delete(CommonUser user) throws ProjectException {
        boolean success;
        try {
            if (source.isFile()) {
                success = source.delete();
            } else {
                success = FolderHelper.deleteFolder(source);
            }
        } catch (Exception e) {
            throw new ProjectException("Failed to delete local resource", e);
        }
        if (!success) {
            throw new ProjectException("Failed to delete local resource");
        }
    }

    public ArtefactPath getArtefactPath() {
        return path;
    }

    public String getName() {
        return source.getName();
    }

    public boolean isFolder() {
        return source.isDirectory();
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

    private static class ArtefactStateHolder implements StateHolder {
        private static final long serialVersionUID = -3771476721294909451L;

        private ProjectVersion version;
        private Map<String, Object> props;
        private Collection<Property> properties;
    }

    public LockInfo getLockInfo() {
        return LockInfoImpl.NO_LOCK;
    }

    public void lock(CommonUser user) throws ProjectException {
        // TODO Auto-generated method stub
    }

    public void unlock(CommonUser user) throws ProjectException {
        // TODO Auto-generated method stub
    }

    public void commit(CommonUser user, int major, int minor) throws ProjectException {
        // TODO Auto-generated method stub
    }

    public ArtefactAPI getVersion(CommonVersion version) {
        // TODO Auto-generated method stub
        return this;
    }

    public void removeAllProperties() throws PropertyException {
        properties.removeAll();
    }
}
