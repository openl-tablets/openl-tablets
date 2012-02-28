package org.openl.rules.repository.jcr;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
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
import org.openl.rules.common.CommonUser;
import org.openl.rules.common.ValueType;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.repository.REntity;
import org.openl.rules.repository.RLock;
import static org.openl.rules.repository.jcr.NodeUtil.isSame;
import static org.openl.rules.repository.jcr.NodeUtil.convertDate2Calendar;

import org.openl.rules.repository.api.ArtefactProperties;
import org.openl.rules.repository.exceptions.RRepositoryException;

/**
 * Implementation of JCR Entity. It is linked with node in JCR implementation,
 * always.
 *
 * @author Aleh Bykhavets
 *
 */
public class JcrEntity extends JcrCommonArtefact implements REntity {
    private static Log LOG = LogFactory.getLog(JcrEntity.class);
    private static final String[] ALLOWED_PROPS = {};

    private Map<String, org.openl.rules.common.Property> properties;
    private Date effectiveDate;
    private Date expirationDate;

    private String lineOfBusiness;

    private Map<String, Object> props;

    private JcrLock lock;

    private CommonVersionImpl risedVersion;
    private JcrVersion version;


    // ------ protected methods ------

    public JcrEntity(Node node) throws RepositoryException {
        super(node);
        lock = new JcrLock(node);
        version = new JcrVersion(node);

        if (node.hasProperty(ArtefactProperties.PROP_EFFECTIVE_DATE)) {
            effectiveDate = node.getProperty(ArtefactProperties.PROP_EFFECTIVE_DATE).getDate().getTime();
        }
        if (node.hasProperty(ArtefactProperties.PROP_EXPIRATION_DATE)) {
            expirationDate = node.getProperty(ArtefactProperties.PROP_EXPIRATION_DATE).getDate().getTime();
        }
        if (node.hasProperty(ArtefactProperties.PROP_LINE_OF_BUSINESS)) {
            lineOfBusiness = node.getProperty(ArtefactProperties.PROP_LINE_OF_BUSINESS).getString();
        }

        properties = new HashMap<String, org.openl.rules.common.Property>();
        initProperties();
        props = new HashMap<String, Object>();
        loadProps();
    }

    public void addProperty(String name, ValueType type, Object value) throws RRepositoryException {
        try {
            NodeUtil.smartCheckout(node(), false);
        } catch (RepositoryException e) {
            throw new RRepositoryException("Internal error.", e);
        }

        if (hasProperty(name)) {
            removeProperty(name);
        }

        JcrProperty jp = new JcrProperty(node(), name, type, value);
        properties.put(name, jp);
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

    public Date getEffectiveDate() {
        return effectiveDate;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public String getLineOfBusiness() {
        return lineOfBusiness;
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

    public org.openl.rules.common.Property getProperty(String name) throws RRepositoryException {
        org.openl.rules.common.Property rp = properties.get(name);
        try {
            if (node().hasProperty(name)) {
                Property p = node().getProperty(name);

                rp = new JcrProperty(node(), p);
                properties.put(name, rp);
            }
        } catch (RepositoryException e) {
            throw new RRepositoryException(String.format("Failed to get property \"%s\"", name), e);
        }

        if (rp == null) {
            throw new RRepositoryException("No such property ''{0}''.", null, name);
        }

        return rp;
    }

    public Map<String, Object> getProps() {
        return props;
    }

    public boolean hasProperty(String name) {
        return (properties.get(name) != null);
    }

    private void initProperties() throws RepositoryException {
        properties.clear();

        Node n = node();
        for (String s : ALLOWED_PROPS) {
            if (n.hasProperty(s)) {
                Property p = n.getProperty(s);

                JcrProperty prop = new JcrProperty(node(), p);
                properties.put(prop.getName(), prop);
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

    public void removeProperty(String name) throws RRepositoryException {
        org.openl.rules.common.Property rp = properties.get(name);

        if (rp == null) {
            throw new RRepositoryException("No such property ''{0}''.", null, name);
        }

        Node n = node();
        try {
            NodeUtil.smartCheckout(n, false);
        } catch (RepositoryException e) {
            throw new RRepositoryException("Internal error.", e);
        }

        try {
            n.getProperty(name).remove();
        } catch (RepositoryException e) {
            throw new RRepositoryException("Cannot remove property ''{0}''.", e, name);
        }
        properties.remove(name);
    }

    public void setEffectiveDate(Date date) throws RRepositoryException {
        // do not update JCR if property wasn't changed
        if (isSame(effectiveDate, date)) {
            return;
        }

        Node n = node();
        Calendar c = convertDate2Calendar(date);

        try {
            NodeUtil.smartCheckout(n, false);
            n.setProperty(ArtefactProperties.PROP_EFFECTIVE_DATE, c);
            effectiveDate = date;
        } catch (RepositoryException e) {
            throw new RRepositoryException("Cannot set effectiveDate.", e);
        }
    }

    // ------ private ------

    public void setExpirationDate(Date date) throws RRepositoryException {
        // do not update JCR if property wasn't changed
        if (isSame(expirationDate, date)) {
            return;
        }

        Node n = node();
        Calendar c = convertDate2Calendar(date);

        try {
            NodeUtil.smartCheckout(n, false);
            n.setProperty(ArtefactProperties.PROP_EXPIRATION_DATE, c);
            expirationDate = date;
        } catch (RepositoryException e) {
            throw new RRepositoryException("Cannot set expirationDate.", e);
        }
    }

    public void setLineOfBusiness(String lineOfBusiness) throws RRepositoryException {
        // do not update JCR if property wasn't changed
        if (isSame(this.lineOfBusiness, lineOfBusiness)) {
            return;
        }

        Node n = node();

        try {
            NodeUtil.smartCheckout(n, false);
            n.setProperty(ArtefactProperties.PROP_LINE_OF_BUSINESS, lineOfBusiness);
            this.lineOfBusiness = lineOfBusiness;
        } catch (RepositoryException e) {
            throw new RRepositoryException("Cannot set LOB.", e);
        }
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
        } catch (RepositoryException e) {
            throw new RRepositoryException("Cannot set property " + propName + ".", e);
        }
    }

    public void setProps(Map<String, Object> props) throws RRepositoryException {
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
            setProperty(propName, propValue);
        }
        this.props = props;
    }

    public RLock getLock() throws RRepositoryException {
        return lock;
    }

    public boolean isLocked() throws RRepositoryException {
        return lock.isLocked();
    }

    public void lock(CommonUser user) throws RRepositoryException {
        lock.lock(user);
    }

    public void unlock(CommonUser user) throws RRepositoryException {
        lock.unlock(user);
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

            checkInAll(n, user);
            commitParent(n.getParent());
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
}
