package org.openl.rules.repository.jcr;

import java.util.Calendar;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.repository.CommonUser;
import org.openl.rules.repository.CommonUserImpl;
import org.openl.rules.repository.RLock;
import org.openl.rules.repository.exceptions.RRepositoryException;

public class JcrLock implements RLock {
    private static final Log log = LogFactory.getLog(JcrLock.class);

    private Node node;

    protected JcrLock(Node projectNode) throws RepositoryException {
        String projectName = projectNode.getName();
        Node parent = projectNode.getParent();

        String lockNode = "lock~" + projectName;

        if (parent.hasNode(lockNode)) {
            node = parent.getNode(lockNode);
        } else {
            node = NodeUtil.createNode(parent, lockNode, JcrNT.NT_LOCK, false);
            parent.save();
        }
    }

    protected Calendar currTime() {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        return c;
    }

    public Date getLockedAt() {
        try {
            if (node.hasProperty(JcrNT.PROP_LOCKED_AT)) {
                return node.getProperty(JcrNT.PROP_LOCKED_AT).getDate().getTime();
            }
        } catch (RepositoryException e) {
            log.info("getLockedAt", e);
        }

        return null;
    }

    public CommonUser getLockedBy() {
        try {
            if (node.hasProperty(JcrNT.PROP_LOCKED_BY)) {
                String whoLocked = node.getProperty(JcrNT.PROP_LOCKED_BY).getString();
                return new CommonUserImpl(whoLocked);
            }
        } catch (RepositoryException e) {
            log.info("getLockedBy", e);
        }

        return null;
    }

    public boolean isLocked() {
        try {
            return (node.hasProperty(JcrNT.PROP_LOCKED_BY));
        } catch (RepositoryException e) {
            log.info("isLocked", e);
            return false;
        }
    }

    protected void lock(CommonUser user) throws RRepositoryException {
        try {
            if (node.hasProperty(JcrNT.PROP_LOCKED_BY)) {
                // already locked
                String whoLocked = node.getProperty(JcrNT.PROP_LOCKED_BY).getString();
                throw new RRepositoryException("Already locked by ''{0}''.", null, whoLocked);
            }
        } catch (RepositoryException e) {
            throw new RRepositoryException("Cannot check lock.", e);
        }

        try {
            node.setProperty(JcrNT.PROP_LOCKED_BY, user.getUserName());
            node.setProperty(JcrNT.PROP_LOCKED_AT, currTime());
            node.save();
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to set lock.", e);
        }
    }

    protected void unlock(CommonUser user) throws RRepositoryException {
        try {
            if (!node.hasProperty(JcrNT.PROP_LOCKED_BY)) {
                // no locks
                return;
            }

            String whoLocked = node.getProperty(JcrNT.PROP_LOCKED_BY).getString();
            String whoUnlocks = user.getUserName();

            if (!whoLocked.equals(whoUnlocks)) {
                throw new RRepositoryException("Lock that was set by ''{0}'' cannot be removed by ''{1}''.", null,
                        whoLocked, whoUnlocks);
            }
        } catch (RepositoryException e) {
            throw new RRepositoryException("Cannot check lock.", e);
        }

        try {
            node.setProperty(JcrNT.PROP_LOCKED_BY, (String) null);
            node.setProperty(JcrNT.PROP_LOCKED_AT, (Calendar) null);
            node.save();
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to remove lock.", e);
        }
    }
}
