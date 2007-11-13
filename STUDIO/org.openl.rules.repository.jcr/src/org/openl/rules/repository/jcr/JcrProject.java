package org.openl.rules.repository.jcr;

import java.util.Date;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

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
    
    private JcrCommonProject project;
    
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

        // TODO what should be in default description?
        n.setProperty(JcrNT.PROP_PRJ_DESCR, "created " + new Date() + " by UNKNOWN");

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

        project = new JcrCommonProject(node);
    }

    /** {@inheritDoc} */
    public RFolder getRootFolder() {
        return rootFolder;
    }

    public boolean isMarked4Deletion() throws RRepositoryException {
        return project.isMarked4Deletion();
    }

    public void delete() throws RDeleteException {
        project.delete();
    }

    public void undelete() throws RModifyException {
        project.undelete();
    }

    public void erase() throws RDeleteException {
        project.erase();
    }
    
    public void commit() throws RRepositoryException {
        project.commit();  
    }
}
