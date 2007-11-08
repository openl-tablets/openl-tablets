package org.openl.rules.repository.jcr;

import java.util.Date;

import javax.jcr.Node;
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
        // TODO what should be in status?
        n.setProperty(JcrNT.PROP_PRJ_STATUS, "DRAFT");

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
}
