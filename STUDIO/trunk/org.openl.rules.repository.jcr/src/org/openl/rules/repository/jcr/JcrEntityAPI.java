package org.openl.rules.repository.jcr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.common.ArtefactPath;
import org.openl.rules.common.CommonUser;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.LockInfo;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.PropertyException;
import org.openl.rules.common.ValueType;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.common.impl.RepositoryProjectVersionImpl;
import org.openl.rules.common.impl.RepositoryVersionInfoImpl;
import org.openl.rules.repository.RLock;
import org.openl.rules.repository.RVersion;

import static org.openl.rules.repository.jcr.NodeUtil.isSame;

import org.openl.rules.repository.api.ArtefactAPI;
import org.openl.rules.repository.api.ArtefactProperties;
import org.openl.rules.repository.exceptions.RRepositoryException;

/**
 * Implementation of JCR Entity. It is linked with node in JCR implementation,
 * always.
 * 
 * @author Aleh Bykhavets
 * 
 */
public class JcrEntityAPI extends JcrCommonArtefact implements ArtefactAPI {
    private static Log LOG = LogFactory.getLog(JcrEntityAPI.class);
    private static final String[] ALLOWED_PROPS = {ArtefactProperties.PROP_EFFECTIVE_DATE, ArtefactProperties.PROP_EXPIRATION_DATE, ArtefactProperties.PROP_LINE_OF_BUSINESS};

    private Map<String, org.openl.rules.common.Property> properties;
    private Map<String, Object> props;

    private boolean oldVersion;

    private JcrLock lock;

    private CommonVersionImpl risedVersion;
    private JcrVersion version;
    private ArtefactPath path;

    // ------ protected methods ------

    public JcrEntityAPI(Node node, ArtefactPath path, boolean oldVersion) throws RepositoryException {
        super(node, path.segment(path.segmentCount() - 1));
        this.oldVersion = oldVersion;
        this.path = path;
        if (oldVersion) {
            lock = null;
        } else {
            lock = new JcrLock(node);
        }
        version = new JcrVersion(node);

        properties = new HashMap<String, org.openl.rules.common.Property>();
        initProperties();
        props = new HashMap<String, Object>();
        loadProps();
    }

    public boolean isOldVersion() {
        return oldVersion;
    }

    public void addProperty(String name, ValueType type, Object value) throws PropertyException {
        //FIXME
        try {
            NodeUtil.smartCheckout(node(), false);
        } catch (RepositoryException e) {
            throw new PropertyException("Internal error.", e);
        }

        if (hasProperty(name)) {
            removeProperty(name);
        }

        if (value != null) {
            JcrProperty jp;
            try {
                jp = new JcrProperty(node(), name, type, value);
            } catch (RRepositoryException e) {
                throw new PropertyException("Internal error.", e);
            }
            properties.put(name, jp);
            try {
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

    public Collection<org.openl.rules.common.Property> getProperties() {
        return properties.values();
    }

    public org.openl.rules.common.Property getProperty(String name) throws PropertyException {
        org.openl.rules.common.Property rp = properties.get(name);

        try {
            if (node().hasProperty(name)) {
                rp = new JcrProperty(node(), node().getProperty(name));
                properties.put(name, rp);
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (rp == null) {
            throw new PropertyException("No such property ''{0}''.", null, name);
        }

        return rp;
    }

    public Map<String, Object> getProps() {
        return props;
    }

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

    private void initProperties() throws RepositoryException {
        properties.clear();

        Node n = node();
        for (String s : ALLOWED_PROPS) {
            if (n.hasProperty(s)) {
                Property p = n.getProperty(s);

                properties.put(s, new JcrProperty(node(), p));
            }
        }
    }

    private void loadProps() throws RepositoryException {
        Node n = node();
        for (int i = 1; i <= ArtefactProperties.PROPS_COUNT; i++) {
            String propName = ArtefactProperties.PROP_ATTRIBUTE + i;
            if (n.hasProperty(propName)) {
                Value value = n.getProperty(propName).getValue();
                Object propValue = null;
                int valueType = value.getType();
                switch (valueType) {
                    case PropertyType.DATE:
                        propValue = value.getDate().getTime();
                        break;
                    case PropertyType.DOUBLE:
                        propValue = value.getDouble();
                        break;
                    default:
                        propValue = value.getString();
                        break;
                }
                props.put(propName, propValue);
            }
        }
    }

    public org.openl.rules.common.Property removeProperty(String name) throws PropertyException {
        org.openl.rules.common.Property rp = getProperty(name);

        if (rp == null) {
            throw new PropertyException("No such property ''{0}''.", null, name);
        }

        Node n = node();
        try {
            NodeUtil.smartCheckout(n, false);
        } catch (RepositoryException e) {
            throw new PropertyException("Internal error.", e);
        }

        try {
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
                n.setProperty(propName, (String) propValue);
            }
            n.save();
        } catch (RepositoryException e) {
            throw new RRepositoryException("Cannot set property " + propName + ".", e);
        }
    }

    public void setProps(Map<String, Object> props) throws PropertyException {
        if (props == null) {
            return;
        }
        // do not update JCR if property wasn't changed
        if (isSame(this.props, props)) {
            return;
        }
        Set<String> propNames = props.keySet();
        for (String propName : propNames) {
            Object propValue = props.get(propName);
            try {
                setProperty(propName, propValue);
            } catch (RRepositoryException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        this.props = props;
    }

    public RLock getLock() throws RRepositoryException {
        // FIXME
        if (lock == null) {
            return RLock.NO_LOCK;
        } else {
            return lock;
        }
    }

    public boolean isLocked() throws RRepositoryException {
        return getLock().isLocked();
    }

    public void lock(CommonUser user) throws ProjectException {
        try {
            lock.lock(user);
        } catch (RRepositoryException e) {
            throw new ProjectException("", e);
        }
    }

    public void unlock(CommonUser user) throws ProjectException {
        try {
            lock.unlock(user);
        } catch (RRepositoryException e) {
            throw new ProjectException("", e);
        }
    }

    public void riseVersion(int major, int minor) throws RRepositoryException {
        int ma = version.getMajor();
        int mi = version.getMinor();

        // clear in case of invalid input
        risedVersion = null;

        if (major < ma) {
            throw new RRepositoryException("New major version is less than current!", null);
        } else if (major == ma) {
            if (minor < mi) {
                throw new RRepositoryException(
                        "New minor version cannot be less than current, when major version remains unchanged!", null);
            }
        }

        risedVersion = new CommonVersionImpl(major, minor, version.getRevision());
    }

    public void commit(CommonUser user) throws RRepositoryException {
        if (risedVersion != null) {
            version.set(risedVersion.getMajor(), risedVersion.getMinor());
            risedVersion = null;
        }

        try {
            Node n = node();
            NodeUtil.smartCheckout(n, true);
            version.nextRevision();
            version.updateVersion(n);

            commitParent(n.getParent());
            checkInAll(n, user);
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to checkin project ''{0}''!", e, getName());
        }
    }

    protected void commitParent(Node parent) throws RepositoryException {
        if (parent.isModified()) {
            parent.save();
        }
        if (parent.isCheckedOut()) {
            if (parent.isNodeType(JcrNT.MIX_VERSIONABLE)) {
                parent.save();
                parent.checkin();
            }
        }
    }

    protected void checkInAll(Node n, CommonUser user) throws RepositoryException {
        NodeIterator ni = n.getNodes();

        while (ni.hasNext()) {
            Node child = ni.nextNode();
            checkInAll(child, user);
        }

        boolean saveProps = false;
        PropertyIterator pi = n.getProperties();
        while (pi.hasNext()) {
            Property p = pi.nextProperty();
            if (p.isModified() || p.isNew()) {
                saveProps = true;
                break;
            }
        }

        boolean mustBeSaved = (saveProps || n.isModified() || n.isNew());
        boolean mustBeCheckedIn = (n.isNodeType(JcrNT.MIX_VERSIONABLE) && n.isCheckedOut());

        if (mustBeCheckedIn) {
            version.updateRevision(n);
            n.setProperty(ArtefactProperties.PROP_MODIFIED_BY, user.getUserName());
            n.save();
            LOG.info("Checking in... " + n.getPath());
            n.checkin();
        } else if (mustBeSaved) {
            LOG.info("Saving... " + n.getPath());
            n.save();
        }
    }

    public void delete(CommonUser user) throws ProjectException {
        try {
            Node parent = node().getParent();
            delete();
            commitParent(parent);
        } catch (Exception e) {
            throw new ProjectException("Failed to delete node.", e);
        }
    }

    public ArtefactPath getArtefactPath() {
        return path;
    }

    public boolean isFolder() {
        return false;
    }

    public ProjectVersion getVersion() {
        // FIXME
        RVersion rv = getActiveVersion();
        RepositoryVersionInfoImpl rvii = new RepositoryVersionInfoImpl(rv.getCreated(), rv.getCreatedBy().getUserName());

        return new RepositoryProjectVersionImpl(rv, rvii);
    }

    public List<ProjectVersion> getVersions() {
        // FIXME
        LinkedList<ProjectVersion> vers = new LinkedList<ProjectVersion>();

        try {
            for (RVersion rv : getVersionHistory()) {
                RepositoryVersionInfoImpl rvii = new RepositoryVersionInfoImpl(rv.getCreated(), rv.getCreatedBy()
                        .getUserName());
                vers.add(new RepositoryProjectVersionImpl(rv, rvii));
            }

        } catch (RRepositoryException e) {
            LOG.error("Failed to get version history!", e);
        }
        return vers;
    }

    public LockInfo getLockInfo() {
        // FIXME
        try {
            return getLock();
        } catch (RRepositoryException e) {
            LOG.error("getLockInfo", e);
            return RLock.NO_LOCK;
        }
    }

    public void commit(CommonUser user, int major, int minor) throws ProjectException {
        try {
            riseVersion(major, minor);
            commit(user);
        } catch (RRepositoryException e) {
            throw new ProjectException("", e);
        }
    }

    public JcrEntityAPI getVersion(CommonVersion version) throws RRepositoryException{
        try {
            Node frozenNode = NodeUtil.getNode4Version(node(), version);
            return new JcrEntityAPI(frozenNode, getArtefactPath(), true);
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to get version for node.", e);
        }
    }

    public void removeAllProperties() throws PropertyException {
        List<String> propertyNames = new ArrayList<String>(properties.keySet());
        for(String propertyName : propertyNames){
            removeProperty(propertyName);
        }
        properties.clear();
    }
}
