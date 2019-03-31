package org.openl.rules.project.impl.local;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.rules.common.ArtefactPath;
import org.openl.rules.common.ArtefactType;
import org.openl.rules.common.CommonUser;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.InheritedProperty;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalArtefactAPI implements ArtefactAPI {
    private final Logger log = LoggerFactory.getLogger(LocalArtefactAPI.class);

    protected File source;
    private PropertiesContainerImpl properties;
    private Map<String, Object> props;
    protected ArtefactPath path;
    private ProjectVersion currentVersion;
    private List<ProjectVersion> versions;
    private boolean modified;
    private long creationDate;
    protected LocalWorkspace workspace;

    public LocalArtefactAPI(File source, ArtefactPath path, LocalWorkspace workspace) {
        this.source = source;
        this.modified = false;
        this.creationDate = source.lastModified();
        this.path = path;
        this.workspace = workspace;
        currentVersion = new RepositoryProjectVersionImpl();
        properties = new PropertiesContainerImpl();
        load();
    }

    public StateHolder getStateHolder() {
        ArtefactStateHolder state = new ArtefactStateHolder();

        state.version = getVersion();
        state.props = getProps();
        state.modified = this.modified;
        state.creationDate = this.creationDate;

        state.properties = new ArrayList<>();
        state.properties.addAll(getProperties());
        return state;
    }

    public void applyStateHolder(StateHolder stateHolder) {
        if (stateHolder instanceof ArtefactStateHolder) {
            ArtefactStateHolder state = (ArtefactStateHolder) stateHolder;
            this.currentVersion = state.version;
            this.props = state.props;
            this.modified = state.modified;
            this.creationDate = state.creationDate;
            
            for (Property property : state.properties) {
                try {
                    this.properties.addProperty(property);
                } catch (PropertyException e) {
                    if (log.isErrorEnabled()) {
                        log.error(e.getMessage(), e);
                    }
                }
            }
        } else {
            // TODO consider exception throwing
            log.error("Incorrect type of stateHolder. Was: '{}', but should be 'ArtefactStateHolder", stateHolder.getClass().getName());
        }
    }

    public File getSource() {
        return source;
    }

    @Override
    public Map<String, Object> getProps() {
        return props;
    }

    @Override
    public void setProps(Map<String, Object> props) throws PropertyException {
        this.props = props;
        notifyModified();
    }

    @Override
    public void addProperty(String name, ValueType type, Object value) throws PropertyException {
        properties.addProperty(new PropertyImpl(name, type, value));
        notifyModified();
    }

    @Override
    public Collection<Property> getProperties() {
        return properties.getProperties();
    }

    @Override
    public Property getProperty(String name) throws PropertyException {
        return properties.getProperty(name);
    }

    @Override
    public boolean hasProperty(String name) {
        return properties.hasProperty(name);
    }

    @Override
    public Property removeProperty(String name) throws PropertyException {
        return properties.removeProperty(name);
    }

    @Override
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

    @Override
    public ArtefactPath getArtefactPath() {
        return path;
    }

    @Override
    public String getName() {
        return source.getName();
    }

    @Override
    public boolean isFolder() {
        return source.isDirectory();
    }

    @Override
    public ProjectVersion getVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(ProjectVersion currentVersion) {
        this.currentVersion = currentVersion;
        notifyModified();
    }

    @Override
    public List<ProjectVersion> getVersions() {
        if (versions == null) {
            versions = new ArrayList<>();
        }
        return versions;
    }

    @Override
    public int getVersionsCount() {
        return versions == null ? 0 : versions.size();
    }

    @Override
    public ProjectVersion getVersion(int index) {
        return versions.get(index);
    }

    public void setVersions(List<ProjectVersion> versions) {
        this.versions = versions;
        notifyModified();
    }

    private static class ArtefactStateHolder implements StateHolder {
        private static final long serialVersionUID = -3771476721294909451L;

        private ProjectVersion version;
        private Map<String, Object> props;
        private Map<String, InheritedProperty> inheritedProps;
        private Collection<Property> properties;
        private boolean modified;
        private long creationDate;
    }

    @Override
    public LockInfo getLockInfo() {
        return LockInfoImpl.NO_LOCK;
    }

    @Override
    public void lock(CommonUser user) throws ProjectException {
        // TODO Auto-generated method stub
    }

    @Override
    public void unlock(CommonUser user) throws ProjectException {
        // TODO Auto-generated method stub
    }

    @Override
    public ArtefactAPI getVersion(CommonVersion version) {
        // TODO Auto-generated method stub
        return this;
    }

    @Override
    public void removeAllProperties() throws PropertyException {
        properties.removeAll();
    }
    
    private File getProjectLocation(){
        String projectName = getArtefactPath().segment(0);
        return new File(workspace.getLocation(), projectName);
    }
    
    @Override
    public void commit(CommonUser user, int revision) throws ProjectException {
        modified = false;
        creationDate = source.lastModified();
        save();
    }

    protected void save() {
        try {
            new StatePersistance(this, getProjectLocation()).save();
        } catch (ProjectException e) {
            // TODO: log
        }
    }

    private void load(){
        new StatePersistance(this, getProjectLocation()).load();
    }

    protected void notifyModified() {
        modified = true;
        save();
    }

    public boolean isModified() {
        if(!new StatePersistance(this, getProjectLocation()).isStateSaved()){
            return true;
        }
        return modified || creationDate != source.lastModified();
    }

    @Override
    public Map<String, InheritedProperty> getInheritedProps() {
        int segmentId = path.segmentCount();

        if (segmentId > 1) {
            LocalArtefactAPI parentArtefactAPI = new LocalArtefactAPI(source.getParentFile(), path.withoutSegment(segmentId - 1), workspace);
            Map<String, InheritedProperty> inheritedProps = new HashMap<>();
            
            inheritedProps.putAll(parentArtefactAPI.getInheritedProps());
            
            if (parentArtefactAPI.getProps() != null) {
                Map<String, Object> parentProp = parentArtefactAPI.getProps();
                
                for (Map.Entry<String, Object> entry: parentProp.entrySet()) {
                    InheritedProperty inhProp = new InheritedProperty(entry.getValue(),
                            (parentArtefactAPI.source.isDirectory() ? ArtefactType.FOLDER : ArtefactType.PROJECT ),
                            parentArtefactAPI.getName() );
                    inheritedProps.put(entry.getKey(), inhProp);
                }
            }

            return inheritedProps;
        }

        return new HashMap<>();
    }
    
    public void clearModifyStatus() { 
        modified = false;
        creationDate = source.lastModified();
        save();
    }

    public long getCreationDate() {
        return creationDate;
    }
}
