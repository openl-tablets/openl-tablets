package org.openl.rules.repository.jcr;

import java.util.Date;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.openl.rules.repository.RProject;
import org.openl.rules.repository.RFolder;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.repository.exceptions.RDeleteException;
import org.openl.rules.repository.exceptions.RModifyException;

/**
 * Implementation for JCR Project.
 * 
 * @author Aleh Bykhavets
 *
 */
public class JcrProject extends JcrEntity implements RProject {
    //TODO: candidate to move into JcrNT
    private static final String NODE_FILES = "files";

    /** Project's root folder or project->files. */
    private JcrFolder rootFolder;
    
    private int vMajor;
    private int vMinor;
    private long vRevision;

    /**
     * Creates new project instance.
     * <p>
     * Note that OpenL project cannot be created inside other OpenL project.
     * I.e. nesting is not allowed for OpenL projects.
     *
     * @param parentNode parent node
     * @param nodeName name of node
     * @return newly created project
     * @throws RepositoryException if fails
     */
    protected static JcrProject createProject(Node parentNode, String nodeName) throws RepositoryException {
        Node n = NodeUtil.createNode(parentNode, nodeName, JcrNT.NT_PROJECT, true);

        // set the same
        n.setProperty(JcrNT.PROP_PRJ_NAME, nodeName);
        // TODO what should be in default description?
        n.setProperty(JcrNT.PROP_PRJ_DESCR, "created " + new Date() + " by UNKNOWN");
        
        n.setProperty(JcrNT.PROP_VERSION, 0);
        n.setProperty(JcrNT.PROP_REVISION, 0);

        Node files = n.addNode(NODE_FILES, JcrNT.NT_FILES);
        files.addMixin(JcrNT.MIX_VERSIONABLE);

        parentNode.save();
        n.checkin();
        n.save();

        return new JcrProject(n);
    }

    public JcrProject(Node node) throws RepositoryException {
        super(node);

        checkNodeType(JcrNT.NT_PROJECT);

        Node files = node.getNode(NODE_FILES);
        rootFolder = new JcrFolder(files);

        try {
            long l = node.getProperty(JcrNT.PROP_VERSION).getLong();
            int i = (int)l;
            vMajor = i >> 16;
            vMinor = i & (0xFFFF);
        } catch (RepositoryException e) {
            // TODO: add logging
            vMajor = 1;
            vMinor = 0;
        }

        try {
            vRevision = node.getProperty(JcrNT.PROP_REVISION).getLong();
        } catch (RepositoryException e) {
            // TODO: add logging
            vRevision = 0;
        }
    }

    /** {@inheritDoc} */
    public RFolder getRootFolder() {
        return rootFolder;
    }

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
    public void erase() throws RDeleteException {
        // ALL IS LOST
        // TODO: add logging here
        super.delete();
    }
    
    public void commit() throws RRepositoryException {
        try {
            Node n = node();
            NodeUtil.smartCheckout(n, true);
            vRevision++;
            long l = (vMajor << 16) | (vMinor & 0xFFFF);
            n.setProperty(JcrNT.PROP_VERSION, l);

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
        
        if (saveProps || n.isModified() || n.isNew()) {
            System.out.println("Saving... " + n.getPath());
            n.setProperty(JcrNT.PROP_REVISION, vRevision);
            n.save();
        }
        
        if (n.isNodeType(JcrNT.MIX_VERSIONABLE) && n.isCheckedOut()) {
            System.out.println("Checking in... " + n.getPath());
            n.checkin();
        }
    }
}
