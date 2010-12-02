package org.openl.rules.repository.jcr;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.repository.CommonUser;
import org.openl.rules.repository.CommonVersionImpl;
import org.openl.rules.repository.RCommonProject;
import org.openl.rules.repository.RLock;
import org.openl.rules.repository.exceptions.RRepositoryException;

public class JcrCommonProject extends JcrEntity implements RCommonProject {
    private static Log log = LogFactory.getLog(JcrCommonProject.class);

    private JcrVersion version;
    private JcrLock lock;

    // when null -- don't rise version on commit
    private CommonVersionImpl risedVersion;

    protected JcrCommonProject(Node node) throws RepositoryException {
        super(node);

        version = new JcrVersion(node);
        try {
            lock = new JcrLock(node);
        } catch (RepositoryException e) {
            // for old deployments projects
            lock = null;
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
            n.setProperty(JcrNT.PROP_MODIFIED_BY, user.getUserName());
            n.save();
            log.info("Checking in... " + n.getPath());
            n.checkin();
        } else if (mustBeSaved) {
            log.info("Saving... " + n.getPath());
            n.save();
        }
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

    public void delete(CommonUser user) throws RRepositoryException {
        if (isMarked4Deletion()) {
            throw new RRepositoryException("Project ''{0}'' is already marked for deletion!", null, getName());
        }

        try {
            Node n = node();

            n.checkout();
            n.setProperty(JcrNT.PROP_PRJ_MARKED_4_DELETION, true);
            commit(user);
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to Mark project ''{0}'' for Deletion!", e, getName());
        }
    }

    public void erase(CommonUser user) throws RRepositoryException {
        try {
            Node parent = node().getParent();
            // ALL IS LOST
            // TODO: add logging here
            log.info("Erasing project '" + getName() + "' on behalf of " + user.getUserName());

            super.delete();
            commitParent(parent);
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to delete project ''{0}''!", e, getName());
        }
    }

    // --- protected

    public RLock getLock() throws RRepositoryException {
        return lock;
    }

    public boolean isLocked() throws RRepositoryException {
        return lock.isLocked();
    }

    public boolean isMarked4Deletion() throws RRepositoryException {
        try {
            boolean isMarked;

            Node n = node();
            // even if property itself is 'false' it still means that project is
            // 'marked'
            isMarked = n.hasProperty(JcrNT.PROP_PRJ_MARKED_4_DELETION);

            return isMarked;
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to Check Marked4Deletion!", e);
        }
    }

    public void lock(CommonUser user) throws RRepositoryException {
        lock.lock(user);
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

    public void undelete(CommonUser user) throws RRepositoryException {
        if (!isMarked4Deletion()) {
            throw new RRepositoryException("Project ''{0}'' is not marked for deletion!", null, getName());
        }

        try {
            Node n = node();

            n.checkout();
            n.setProperty(JcrNT.PROP_PRJ_MARKED_4_DELETION, (Value) null, PropertyType.BOOLEAN);
            commit(user);
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to Unmark project ''{0}'' from Deletion!", e, getName());
        }
    }

    public void unlock(CommonUser user) throws RRepositoryException {
        lock.unlock(user);
    }
}
