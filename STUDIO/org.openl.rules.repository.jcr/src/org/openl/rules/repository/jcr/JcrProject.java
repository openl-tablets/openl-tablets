package org.openl.rules.repository.jcr;

import java.util.Date;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.openl.rules.common.CommonUser;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.repository.RFile;
import org.openl.rules.repository.RFolder;
import org.openl.rules.repository.RProject;
import org.openl.rules.repository.api.ArtefactProperties;
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

        NodeUtil.createNode(n, NODE_FILES, JcrNT.NT_FILES, true);

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

        project = new JcrCommonProject(node);
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

    public boolean isMarked4Deletion() throws RRepositoryException {
        return project.isMarked4Deletion();
    }

    public void undelete(CommonUser user) throws RRepositoryException {
        project.undelete(user);
    }

    public RFile createFile(String name) throws RRepositoryException {
        return rootFolder.createFile(name);
    }

    public RFolder createFolder(String name) throws RRepositoryException {
        return rootFolder.createFolder(name);
    }

    public List<RFile> getFiles() throws RRepositoryException {
        return rootFolder.getFiles();
    }

    public List<RFolder> getFolders() throws RRepositoryException {
        return rootFolder.getFolders();
    }
}
