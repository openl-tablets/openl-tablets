package org.openl.rules.repository.jcr;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

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
        // ALL IS LOST
        // TODO: add logging here
        super.delete();
    }
    
    public void commit() throws RRepositoryException {
        try {
            Node n = node();
            NodeUtil.smartCheckout(n, true);
            version.nextRevision();
            version.updateVersion(n);

            checkInAll(n);
            Node parent = n.getParent();
            if (parent.isModified()) {
                parent.save();
            }
            if (parent.isCheckedOut()) {
                if (parent.isNodeType(JcrNT.MIX_VERSIONABLE)) {
                    parent.checkin();
                }
            }
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to checkin project {0}", e, getName());
        }        
    }
    
    // --- protected
    
    protected void checkInAll(Node n) throws RepositoryException {
        NodeIterator ni = n.getNodes();
        
        while (ni.hasNext()) {
            Node child = ni.nextNode();
            checkInAll(child);
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
            n.save();
            System.out.println("Checking in... " + n.getPath());
            n.checkin();
        } else if (mustBeSaved){
            System.out.println("Saving... " + n.getPath());
            n.save();
        }
    }
}
