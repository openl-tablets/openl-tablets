package org.openl.rules.repository.jcr;

import java.util.Calendar;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.openl.rules.common.CommonUser;
import org.openl.rules.common.impl.CommonUserImpl;
import org.openl.rules.repository.RLock;
import org.openl.rules.repository.api.ArtefactProperties;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrLock implements RLock {
    private static final String LOCK_NODE_NAME_PREFIX = "lock~";

    private final Logger log = LoggerFactory.getLogger(JcrLock.class);

    private Node lockNode;
    private Node forNode;

    protected JcrLock(Node forNode) throws RepositoryException {
        this.forNode = forNode;
        String projectName = forNode.getName();
        Node parent = forNode.getParent();

        String lockNodeName = LOCK_NODE_NAME_PREFIX + projectName;

        if (parent.hasNode(lockNodeName)) {
            lockNode = parent.getNode(lockNodeName);
        }
    }

    private void createLockNode() throws RepositoryException {
        String projectName = forNode.getName();
        String lockNodeName = LOCK_NODE_NAME_PREFIX + projectName;
        Node parent = forNode.getParent();
        lockNode = NodeUtil.createNode(parent, lockNodeName, JcrNT.NT_LOCK, false);
        parent.save();
    }

    protected Calendar currTime() {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        return c;
    }

    @Override
    public Date getLockedAt() {
        try {
            if (lockNode.hasProperty(ArtefactProperties.PROP_LOCKED_AT)) {
                return lockNode.getProperty(ArtefactProperties.PROP_LOCKED_AT).getDate().getTime();
            }
        } catch (RepositoryException e) {
            log.info("getLockedAt", e);
        }

        return null;
    }

    @Override
    public CommonUser getLockedBy() {
        try {
            if (lockNode.hasProperty(ArtefactProperties.PROP_LOCKED_BY)) {
                String whoLocked = lockNode.getProperty(ArtefactProperties.PROP_LOCKED_BY).getString();
                return new CommonUserImpl(whoLocked);
            }
        } catch (RepositoryException e) {
            log.info("getLockedBy", e);
        }

        return null;
    }

    @Override
    public boolean isLocked() {
        try {
            return lockNode != null && lockNode.hasProperty(ArtefactProperties.PROP_LOCKED_BY);
        } catch (RepositoryException e) {
            log.info("isLocked", e);
            return false;
        }
    }

    @Override
    public void lock(CommonUser user) throws RRepositoryException {
        try {
            if (lockNode == null) {
                createLockNode();
            }

            if (lockNode.hasProperty(ArtefactProperties.PROP_LOCKED_BY)) {
                // already locked
                String whoLocked = lockNode.getProperty(ArtefactProperties.PROP_LOCKED_BY).getString();
                throw new RRepositoryException("Already locked by ''{0}''.", null, whoLocked);
            }
        } catch (RepositoryException e) {
            throw new RRepositoryException("Cannot check lock.", e);
        }

        try {
            lockNode.setProperty(ArtefactProperties.PROP_LOCKED_BY, user.getUserName());
            lockNode.setProperty(ArtefactProperties.PROP_LOCKED_AT, currTime());
            lockNode.save();
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to set lock.", e);
        }
    }

    @Override
    public void unlock(CommonUser user) throws RRepositoryException {
        try {
            if (lockNode == null) {
                // no locks
                return;
            }

            if (lockNode.hasProperty(ArtefactProperties.PROP_LOCKED_BY)) {
                lockNode.remove();
                forNode.getParent().save();
            }
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to remove lock.", e);
        }
    }
}
