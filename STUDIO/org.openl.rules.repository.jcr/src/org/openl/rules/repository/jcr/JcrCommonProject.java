package org.openl.rules.repository.jcr;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.openl.rules.repository.CommonUser;
import org.openl.rules.repository.RCommonProject;
import org.openl.rules.repository.exceptions.RDeleteException;
import org.openl.rules.repository.exceptions.RModifyException;
import org.openl.rules.repository.exceptions.RRepositoryException;

public class JcrCommonProject extends JcrCommonArtefact implements RCommonProject {

    private JcrVersion version;

    protected JcrCommonProject(Node node) throws RepositoryException {
        super(node);

        version = new JcrVersion(node);
    }

    public boolean isMarked4Deletion() throws RRepositoryException {
        try {
            boolean isMarked;
            
            Node n = node();
            // even if property itself is 'false' it still means that project is 'marked'
            isMarked = n.hasProperty(JcrNT.PROP_PRJ_MARKED_4_DELETION);
            
            return isMarked;
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to Check Marked4Deletion", e);
        }
    }

    public void delete() throws RDeleteException {
        try {
            Node n = node();

            n.checkout();
            n.setProperty(JcrNT.PROP_PRJ_MARKED_4_DELETION, true);
            n.save();
            n.checkin();
        } catch (RepositoryException e) {
            throw new RDeleteException("Failed to Mark project for Deletion", e);
        }
    }

    public void undelete() throws RModifyException {
        try {
            Node n = node();

            n.checkout();
            n.setProperty(JcrNT.PROP_PRJ_MARKED_4_DELETION, (Value)null, PropertyType.BOOLEAN);
            n.save();
            n.checkin();
        } catch (RepositoryException e) {
            throw new RModifyException("Failed to Unmark project from Deletion", e);
        }
    }

    public void erase() throws RDeleteException {
        try {
            Node parent = node().getParent();
            // ALL IS LOST
            // TODO: add logging here
            super.delete();
            commitParent(parent);
        } catch (RepositoryException e) {
            throw new RDeleteException("Failed to delete project {0}", e, getName());
        }        
    }
    
    public void commit(CommonUser user) throws RRepositoryException {
        try {
            Node n = node();
            NodeUtil.smartCheckout(n, true);
            version.nextRevision();
            version.updateVersion(n);

            checkInAll(n, user);
            commitParent(n.getParent());
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to checkin project {0}", e, getName());
        }        
    }
    
    // --- protected
    
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
            n.setProperty(JcrNT.PROP_MODIFIED_BY, user.getUserName());
            n.save();
            System.out.println("Checking in... " + n.getPath());
            n.checkin();
        } else if (mustBeSaved){
            System.out.println("Saving... " + n.getPath());
            n.save();
        }
    }
}
