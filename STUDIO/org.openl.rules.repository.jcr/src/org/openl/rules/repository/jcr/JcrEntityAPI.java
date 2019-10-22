package org.openl.rules.repository.jcr;

import java.util.*;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.openl.rules.common.*;
import org.openl.rules.common.impl.RepositoryProjectVersionImpl;
import org.openl.rules.common.impl.RepositoryVersionInfoImpl;
import org.openl.rules.repository.RLock;
import org.openl.rules.repository.RVersion;
import org.openl.rules.repository.api.ArtefactAPI;
import org.openl.rules.repository.api.ArtefactProperties;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of JCR Entity. It is linked with node in JCR implementation, always.
 *
 * @author Aleh Bykhavets
 */
public class JcrEntityAPI extends JcrCommonArtefact implements ArtefactAPI {
    private final Logger log = LoggerFactory.getLogger(JcrEntityAPI.class);
    /*
     * private static final String[] ALLOWED_PROPS = {ArtefactProperties.PROP_EFFECTIVE_DATE,
     * ArtefactProperties.PROP_EXPIRATION_DATE, ArtefactProperties.PROP_LINE_OF_BUSINESS,
     * ArtefactProperties.VERSION_COMMENT};
     */
    private Map<String, org.openl.rules.common.Property> properties;
    private Map<String, Object> props;

    private JcrVersion version;
    private ArtefactPath path;

    // ------ protected methods ------
    public JcrEntityAPI(Node node, ArtefactPath path, boolean oldVersion) throws RepositoryException {
        super(node, path.segment(path.segmentCount() - 1), oldVersion);
        this.path = path;
        version = new JcrVersion(node);

        properties = new HashMap<>();
        initProperties();
        props = new HashMap<>();
        loadProps();
    }

    @Override
    public void addProperty(String name, ValueType type, Object value) throws PropertyException {
        if (hasProperty(name)) {
            removeProperty(name);
        }

        if (value != null) {
            try {
                NodeUtil.smartCheckout(node(), false);
                JcrProperty jp;
                try {
                    jp = new JcrProperty(node(), name, type, value);
                } catch (RRepositoryException e) {
                    throw new PropertyException("Internal error.", e);
                }
                properties.put(name, jp);
                node().save();
            } catch (RepositoryException e) {
                throw new PropertyException("Internal error.", e);
            }
        }
    }

    private void buildRelPath(StringBuilder sb, Node n) throws RepositoryException {
        if (!n.isNodeType(JcrNT.NT_PROJECT)) {
            buildRelPath(sb, n.getParent());
        }

        if (!n.isNodeType(JcrNT.NT_FILES)) {
            sb.append('/');
            sb.append(n.getName());
        }
    }

    public String getPath() throws RRepositoryException {
        StringBuilder sb = new StringBuilder(128);
        try {
            buildRelPath(sb, node());
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to get relative path.", e);
        }

        return sb.toString();
    }

    @Override
    public Collection<org.openl.rules.common.Property> getProperties() {
        return properties.values();
    }

    @Override
    public org.openl.rules.common.Property getProperty(String name) throws PropertyException {
        org.openl.rules.common.Property rp = properties.get(name);

        try {
            if (node().hasProperty(name)) {
                rp = new JcrProperty(node(), node().getProperty(name));
                properties.put(name, rp);
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
            }
        }

        if (rp == null) {
            throw new PropertyException("No such property ''{0}''.", null, name);
        }

        return rp;
    }

    @Override
    public Map<String, Object> getProps() {
        return props;
    }

    @Override
    public boolean hasProperty(String name) {
        if (properties.containsKey(name)) {
            return true;
        }
        try {
            return node().hasProperty(name);
        } catch (RepositoryException e) {
            return false;
        }
    }

    private void initProperties() {
        properties.clear();
    }

    private void loadProps() throws RepositoryException {
        Node n = node();

        /* Set attrs */
        for (int i = 1; i <= ArtefactProperties.PROPS_COUNT; i++) {
            String propName = ArtefactProperties.PROP_ATTRIBUTE + i;
            if (n.hasProperty(propName)) {
                props.put(propName, getPropValueByType(propName));
            }
        }
    }

    private Object getPropValueByType(String propName) throws RepositoryException {
        Node n = node();

        return getPropValueByType(propName, n);
    }

    private Object getPropValueByType(String propName, Node n) throws RepositoryException {

        Value value = n.getProperty(propName).getValue();
        Object propValue;
        int valueType = value.getType();
        switch (valueType) {
            case PropertyType.DATE:
                propValue = value.getDate().getTime();
                break;
            case PropertyType.LONG:
                propValue = new Date(value.getLong());
                break;
            case PropertyType.DOUBLE:
                propValue = value.getDouble();
                break;
            default:
                propValue = value.getString();
                break;
        }

        return propValue;
    }

    @Override
    public org.openl.rules.common.Property removeProperty(String name) throws PropertyException {
        org.openl.rules.common.Property rp = getProperty(name);

        if (rp == null) {
            throw new PropertyException("No such property ''{0}''.", null, name);
        }

        Node n = node();
        try {
            NodeUtil.smartCheckout(n, false);
            n.getProperty(name).remove();
            n.save();
        } catch (RepositoryException e) {
            throw new PropertyException("Cannot remove property ''{0}''.", e, name);
        }
        return properties.remove(name);
    }

    private void setProperty(String propName, Object propValue) throws RRepositoryException {
        Node n = node();
        try {
            NodeUtil.smartCheckout(n, false);
            if (propValue instanceof Date) {
                n.setProperty(propName, ((Date) propValue).getTime());
            } else if (propValue instanceof Double) {
                n.setProperty(propName, (Double) propValue);
            } else {
                n.setProperty(propName, propValue.toString());
            }
            n.save();
        } catch (RepositoryException e) {
            throw new RRepositoryException("Cannot set property " + propName + ".", e);
        }
    }

    @Override
    public void setProps(Map<String, Object> props) throws PropertyException {
        if (props == null) {
            return;
        }

        for (Map.Entry<String, Object> entry : props.entrySet()) {
            try {
                setProperty(entry.getKey(), entry.getValue());
            } catch (RRepositoryException e) {
                if (log.isErrorEnabled()) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        this.props = props;
    }

    public RLock getLock() throws RRepositoryException {
        if (isOldVersion()) {
            return RLock.NO_LOCK;
        } else {
            try {
                return new JcrLock(node());
            } catch (RepositoryException e) {
                throw new RRepositoryException("Failed to get lock.", e);
            }
        }
    }

    public boolean isLocked() throws RRepositoryException {
        return getLock().isLocked();
    }

    @Override
    public void lock(CommonUser user) throws ProjectException {
        try {
            getLock().lock(user);
        } catch (RRepositoryException e) {
            throw new ProjectException("", e);
        }
    }

    @Override
    public void unlock(CommonUser user) throws ProjectException {
        try {
            getLock().unlock(user);
        } catch (RRepositoryException e) {
            throw new ProjectException("", e);
        }
    }

    private void saveParent(Node node) throws RepositoryException {
        Node parent = node.getParent();
        if (parent.isNew()) {
            saveParent(parent);
        } else if (parent.isModified()) {
            parent.save();
        }
    }

    @Override
    public void delete(CommonUser user) throws ProjectException {
        try {
            if (isLocked()) {
                unlock(user);
            }
            delete();
        } catch (Exception e) {
            throw new ProjectException("Failed to delete node.", e);
        }
    }

    @Override
    public ArtefactPath getArtefactPath() {
        return path;
    }

    @Override
    public boolean isFolder() {
        return false;
    }

    @Override
    public ProjectVersion getVersion() {
        // FIXME
        RVersion rv = getActiveVersion();
        RepositoryVersionInfoImpl rvii = new RepositoryVersionInfoImpl(rv.getCreated(),
            rv.getCreatedBy().getUserName());

        return new RepositoryProjectVersionImpl(rv, rvii);
    }

    @Override
    public List<ProjectVersion> getVersions() {
        // FIXME
        LinkedList<ProjectVersion> vers = new LinkedList<>();
        try {
            List<RVersion> verHist = getVersionHistory();

            RVersion lastVersion = verHist.get(verHist.size() - 1);
            Date modifiedAt = lastVersion.getCreated();
            String modifiedBy = lastVersion.getCreatedBy().getUserName();

            for (RVersion rv : verHist) {
                vers.add(createRepositoryProjectVersion(rv, modifiedAt, modifiedBy));
            }

        } catch (RRepositoryException e) {
            log.error("Failed to get version history.", e);
            // TODO exception should be rethrown
        }
        return vers;
    }

    private RepositoryProjectVersionImpl createRepositoryProjectVersion(RVersion rv,
            Date modifiedAt,
            String modifiedBy) {
        RepositoryVersionInfoImpl rvii = new RepositoryVersionInfoImpl(rv.getCreated(),
            rv.getCreatedBy().getUserName(),
            modifiedAt,
            modifiedBy);
        String versionComment = "";
        Map<String, Object> versionProperties = new HashMap<>();

        try {
            JcrEntityAPI entity = getVersion(rv);

            if (entity.hasProperty(ArtefactProperties.VERSION_COMMENT)) {
                versionComment = entity.getProperty(ArtefactProperties.VERSION_COMMENT).getString();
            }
        } catch (Exception e) {
            log.error("Failed to get version properties.", e);
            // TODO exception should be rethrown
        }

        return new RepositoryProjectVersionImpl(rv, rvii, versionComment, versionProperties);
    }

    @Override
    public int getVersionsCount() {
        try {
            return getVersionHistory().size();
        } catch (RRepositoryException e) {
            log.error("Failed to get version history.", e);
            // TODO exception should be rethrown
            return 0;
        }
    }

    @Override
    public ProjectVersion getVersion(int index) throws RRepositoryException {
        List<RVersion> verHist = getVersionHistory();

        RVersion lastVersion = verHist.get(verHist.size() - 1);
        Date modifiedAt = lastVersion.getCreated();
        String modifiedBy = lastVersion.getCreatedBy().getUserName();

        return createRepositoryProjectVersion(verHist.get(index), modifiedAt, modifiedBy);
    }

    @Override
    public LockInfo getLockInfo() {
        // FIXME
        try {
            return getLock();
        } catch (RRepositoryException e) {
            log.error("getLockInfo", e);
            return RLock.NO_LOCK;
        }
    }

    @Override
    public void commit(CommonUser user, int revision) throws ProjectException {
        try {
            Node n = node();
            saveParent(n);

            NodeUtil.smartCheckout(n, false);
            version.set(revision);
            version.updateVersion(n);
            n.setProperty(ArtefactProperties.PROP_MODIFIED_BY, user.getUserName());

            if (NodeUtil.isVersionable(n)) {
                log.info("Checking in... {}", n.getPath());
                n.save();
                n.checkin();
            } else {
                n.save();
                log.info("Saving... {}", n.getPath());
            }
        } catch (RepositoryException e) {
            throw new ProjectException("Failed to check in artefact ''{0}''.", e, getPath());
        }
    }

    @Override
    public JcrEntityAPI getVersion(CommonVersion version) throws RRepositoryException {
        try {
            Node frozenNode = NodeUtil.getNode4Version(node(), version);
            return new JcrEntityAPI(frozenNode, getArtefactPath(), true);
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to get version for node.", e);
        }
    }

    @Override
    public void removeAllProperties() throws PropertyException {
        List<String> propertyNames = new ArrayList<>(properties.keySet());
        for (String propertyName : propertyNames) {
            removeProperty(propertyName);
        }

        try {
            for (int i = 1; i <= ArtefactProperties.PROPS_COUNT; i++) {
                String propName = ArtefactProperties.PROP_ATTRIBUTE + i;

                if (node().hasProperty(propName)) {
                    removeProperty(propName);
                }
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
            }
        }

        properties.clear();
    }

    @Override
    public Map<String, InheritedProperty> getInheritedProps() {
        Map<String, InheritedProperty> inhProps = new HashMap<>();

        try {
            if (node().getDepth() > 3 && node().getParent() != null) {
                inhProps = getParentProps(node().getParent());
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
            }
        }

        return inhProps;
    }

    private Map<String, InheritedProperty> getParentProps(Node n) {
        Map<String, InheritedProperty> inhProps = new HashMap<>();

        try {
            if (n.getDepth() > 3 && n.getParent() != null) {
                inhProps = getParentProps(n.getParent());
            }

        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
            }
        }

        return inhProps;
    }

}
