package org.openl.rules.repository.jcr;

import java.util.Collection;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.openl.rules.repository.CommonUser;
import org.openl.rules.repository.CommonVersion;
import org.openl.rules.repository.RDependency;
import org.openl.rules.repository.RFolder;
import org.openl.rules.repository.RLock;
import org.openl.rules.repository.RProject;
import org.openl.rules.repository.exceptions.RRepositoryException;

/**
 * Implementation for JCR Project.
 *
 * @author Aleh Bykhavets
 *
 */
public class JcrProject extends JcrEntity implements RProject {
    // TODO: candidate to move into JcrNT
    public static final String NODE_FILES = "files";
    public static final String NODE_DEPENDENCIES = "dependencies";

    /** Project's root folder or project->files. */
    private JcrFolder rootFolder;
    private JcrDependencies dependencies;

    private JcrCommonProject project;

    private JcrLock lock;

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

        NodeUtil.createNode(n, NODE_FILES, JcrNT.NT_FILES, true);
        NodeUtil.createNode(n, NODE_DEPENDENCIES, JcrNT.NT_DEPENDENCIES, false);

        parentNode.save();
        n.checkin();
        n.save();

        return new JcrProject(n);
    }

    public JcrProject(Node node) throws RepositoryException {
        super(node);

        NodeUtil.checkNodeType(node, JcrNT.NT_PROJECT);

        Node files = node.getNode(NODE_FILES);
        rootFolder = new JcrFolder(files);
        Node deps = node.getNode(NODE_DEPENDENCIES);
        dependencies = new JcrDependencies(deps);

        project = new JcrCommonProject(node);
        lock = new JcrLock(node);
    }

    public void commit(CommonUser user) throws RRepositoryException {
        project.commit(user);
    }

    @Override
    public void delete() throws RRepositoryException {
        throw new RRepositoryException("Use delete(CommonUser) instead", null);
    }

    public void delete(CommonUser user) throws RRepositoryException {
        project.delete(user);
    }

    public void erase(CommonUser user) throws RRepositoryException {
        project.erase(user);
    }

    public Collection<RDependency> getDependencies() throws RRepositoryException {
        return dependencies.getDependencies();
    }

    public RLock getLock() throws RRepositoryException {
        return lock;
    }

    public RProject getProjectVersion(CommonVersion version) throws RRepositoryException {
        try {
            Node frozenNode = NodeUtil.getNode4Version(node(), version);
            return new JcrOldProject(getName(), frozenNode, version);
        } catch (RepositoryException e) {
            throw new RRepositoryException("Cannot get project version.", e);
        }
    }

    /** {@inheritDoc} */
    public RFolder getRootFolder() {
        return rootFolder;
    }

    public boolean isLocked() throws RRepositoryException {
        return lock.isLocked();
    }

    public boolean isMarked4Deletion() throws RRepositoryException {
        return project.isMarked4Deletion();
    }

    public void lock(CommonUser user) throws RRepositoryException {
        lock.lock(user);
    }

    public void riseVersion(int major, int minor) throws RRepositoryException {
        project.riseVersion(major, minor);
    }

    public void setDependencies(Collection<? extends RDependency> dependencies) throws RRepositoryException {
        this.dependencies.updateDependencies(dependencies);
    }

    public void undelete(CommonUser user) throws RRepositoryException {
        project.undelete(user);
    }

    public void unlock(CommonUser user) throws RRepositoryException {
        lock.unlock(user);
    }
}
