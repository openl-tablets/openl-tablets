package org.openl.rules.workspace.mock;

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
import org.openl.rules.repository.RTransactionManager;
import org.openl.rules.repository.api.ArtefactAPI;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.workspace.dtr.impl.LockInfoImpl;
import org.openl.rules.workspace.props.impl.PropertiesContainerImpl;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.transaction.UserTransaction;

public class MockArtefact implements ArtefactAPI {

    private final static ProjectVersion INITIAL_VERSION = new RepositoryProjectVersionImpl(0, 0, 0, null);
    private String name;
    private PropertiesContainerImpl properties;
    private Map<String, Object> props;

    public MockArtefact(String name) {
        this.name = name;
        props = new HashMap<String, Object>();
        properties = new PropertiesContainerImpl();
    }

    public boolean isModified() {
        return false;
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
        return false;
    }

    public Property removeProperty(String name) throws PropertyException {
        return properties.removeProperty(name);
    }

    public void removeAllProperties() throws PropertyException {
        properties.removeAll();
    }

    public void delete(CommonUser user) throws ProjectException {
    }

    public ArtefactPath getArtefactPath() {
        return null;
    }

    public String getName() {
        return name;
    }

    public boolean isFolder() {
        return false;
    }

    public ProjectVersion getVersion() {
        return INITIAL_VERSION;
    }

    public List<ProjectVersion> getVersions() {
        return null;
    }

    public LockInfo getLockInfo() {
        return LockInfoImpl.NO_LOCK;
    }

    public void commit(CommonUser user, int major, int minor, int revision) throws ProjectException {
    }

    public void lock(CommonUser user) throws ProjectException {
    }

    public void unlock(CommonUser user) throws ProjectException {
    }

    public ArtefactAPI getVersion(CommonVersion version) throws ProjectException {
        return null;
    }

    public Map<String, Object> getProps() {
        return props;
    }

    public void setProps(Map<String, Object> props) throws PropertyException {
        this.props = props;
    }

    public UserTransaction createTransaction() throws RRepositoryException {
        return RTransactionManager.NO_TRANSACTION;
    }
}
