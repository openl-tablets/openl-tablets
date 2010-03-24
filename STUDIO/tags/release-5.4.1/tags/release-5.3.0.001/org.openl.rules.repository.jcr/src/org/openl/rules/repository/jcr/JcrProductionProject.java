package org.openl.rules.repository.jcr;

import org.openl.rules.repository.RProject;
import org.openl.rules.repository.RFolder;
import org.openl.rules.repository.RDependency;
import org.openl.rules.repository.CommonVersion;
import org.openl.rules.repository.CommonUser;
import org.openl.rules.repository.RLock;
import org.openl.rules.repository.exceptions.RRepositoryException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.Collection;

public class JcrProductionProject extends JcrProductionEntity implements RProject {
    public static final String NODE_FILES = "files";
    private RFolder rootFolder;

    /**
     * Creates new project instance.
     *
     * @param parentNode parent node
     * @param nodeName name of node
     * @return newly created project
     * @throws RepositoryException if fails
     */
    protected static JcrProductionProject createProject(Node parentNode, String nodeName) throws RepositoryException {
        Node n = NodeUtil.createNode(parentNode, nodeName, JcrNT.NT_PROD_PROJECT, false);

        parentNode.save();
        n.save();

        Node nodeFiles = NodeUtil.createNode(n, NODE_FILES, JcrNT.NT_PROD_FILES, false);
        n.save();
        nodeFiles.save();

        return new JcrProductionProject(n);
    }

    public JcrProductionProject(Node node) throws RepositoryException {
        super(node);

        Node files = node.getNode(NODE_FILES);

        rootFolder = new JcrProductionFolder(files);
    }

    /**
     * Commits changes in background versioned storage.
     *
     * @throws org.openl.rules.repository.exceptions.RRepositoryException if
     *             failed
     */
    public void commit(CommonUser user) throws RRepositoryException {
        throw new UnsupportedOperationException();
    }

    /**
     * Marks the project for deletion. Project is too important to be deleted so
     * easily.
     *
     * @throws RRepositoryException if failed
     */
    public void delete(CommonUser user) throws RRepositoryException {
        throw new UnsupportedOperationException();
    }

    /**
     * Erases the project from the repository completely. Before erasing the
     * project must be marked for deletion. I.e. {@link #delete()} should be
     * invoked. Otherwise this method will throw exception.
     *
     * @throws RRepositoryException if failed
     */
    public void erase(CommonUser user) throws RRepositoryException {
        throw new UnsupportedOperationException();
    }

    public Collection<RDependency> getDependencies() throws RRepositoryException {
        throw new UnsupportedOperationException();
    }

    public RLock getLock() throws RRepositoryException {
        throw new UnsupportedOperationException();
    }

    public RProject getProjectVersion(CommonVersion version) throws RRepositoryException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns root folder of the project.
     *
     * @return root folder
     */
    public RFolder getRootFolder() {
        return rootFolder;
    }

    public boolean isLocked() throws RRepositoryException {
        return false;
    }

    /**
     * Returns whether the project is marked for deletion. If a project is
     * marked for deletion, it should not be used.
     *
     * @return <code>true</code> if project is marked for deletion;
     *         <code>false</code> otherwise
     */
    public boolean isMarked4Deletion() throws RRepositoryException {
        return false;
    }

    public void lock(CommonUser user) throws RRepositoryException {
        throw new UnsupportedOperationException();
    }

    public void riseVersion(int major, int minor) throws RRepositoryException {
        throw new UnsupportedOperationException();
    }

    public void setDependencies(Collection<? extends RDependency> dependencies) throws RRepositoryException {
        throw new UnsupportedOperationException();
    }

    /**
     * Unmarks the project from deletion.
     *
     * @throws RRepositoryException if failed
     */
    public void undelete(CommonUser user) throws RRepositoryException {
        throw new UnsupportedOperationException();
    }

    public void unlock(CommonUser user) throws RRepositoryException {
        throw new UnsupportedOperationException();
    }
}
