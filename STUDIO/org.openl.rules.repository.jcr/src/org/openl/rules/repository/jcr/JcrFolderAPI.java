package org.openl.rules.repository.jcr;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.openl.rules.common.ArtefactPath;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectException;
import org.openl.rules.repository.api.FolderAPI;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation for JCR Folder.
 *
 * @author Aleh Bykhavets
 *
 */
public class JcrFolderAPI extends JcrEntityAPI implements FolderAPI {
    private final Logger log = LoggerFactory.getLogger(JcrFolderAPI.class);

    /**
     * Creates new folder.
     *
     * @param parent parent node (files or other folder)
     * @param nodeName name of new node
     * @return newly created folder
     * @throws RepositoryException if fails
     */
    protected static JcrFolderAPI createFolder(JcrFolderAPI parent,
            String nodeName,
            ArtefactPath path) throws RepositoryException {
        Node parentNode = parent.node();
        Node n = NodeUtil.createNode(parentNode, nodeName, JcrNT.NT_FOLDER, true);
        parentNode.save();
        n.save();

        return new JcrFolderAPI(n, path, false);
    }

    public JcrFolderAPI(Node node, ArtefactPath path, boolean oldVersion) throws RepositoryException {
        super(node, path, oldVersion);

        // NodeUtil.checkNodeType(node, JcrNT.NT_FOLDER);
    }

    public JcrFolderAPI(Node node, ArtefactPath path) throws RepositoryException {
        this(node, path, false);
    }

    @Override
    public JcrFileAPI addResource(String name, InputStream content) throws ProjectException {
        try {
            JcrFileAPI file = JcrFileAPI.createFile(this, name, getArtefactPath().withSegment(name));
            file.setContent(content);
            return file;
        } catch (RepositoryException e) {
            throw new ProjectException(String.format("Failed to Create File \"%s\". Reason: %s", name, e.getMessage()),
                e);
        }
    }

    @Override
    public JcrFolderAPI addFolder(String name) throws ProjectException {
        try {
            return JcrFolderAPI.createFolder(this, name, getArtefactPath().withSegment(name));
        } catch (RepositoryException e) {
            throw new ProjectException(
                String.format("Failed to Create Sub Folder \"%s\". Reason: %s", name, e.getMessage()),
                e);
        }
    }

    public List<JcrEntityAPI> getFiles() throws RRepositoryException {
        List<JcrEntityAPI> result = new LinkedList<>();
        listNodes(result, true);
        return result;
    }

    /** {@inheritDoc} */
    public List<JcrEntityAPI> getFolders() throws RRepositoryException {
        List<JcrEntityAPI> result = new LinkedList<>();
        listNodes(result, false);
        return result;
    }

    // ------ private methods ------

    /**
     * Lists nodes.
     *
     * @param list2add list to which nodes should be added
     * @param listFiles whether return only files or only folders
     * @throws RRepositoryException if failed
     */
    private void listNodes(List<JcrEntityAPI> list2add, boolean listFiles) throws RRepositoryException {
        try {
            // FIXME for old
            NodeIterator ni = node().getNodes();
            while (ni.hasNext()) {
                Node n = ni.nextNode();
                String name = n.getName();

                // TODO: use search? But looking through direct child nodes
                // seems faster
                boolean isFile;
                if (isOldVersion()) {
                    Node frozenNode = NodeUtil.normalizeOldNode(n, getVersion());
                    isFile = frozenNode.getProperty("jcr:frozenPrimaryType").getString().equals(JcrNT.NT_FILE);
                    n = frozenNode;
                } else {
                    isFile = n.isNodeType(JcrNT.NT_FILE);
                }
                if (!n.isNodeType(JcrNT.NT_LOCK)) {
                    if (isFile) {
                        if (listFiles) {
                            list2add.add(new JcrFileAPI(n, getArtefactPath().withSegment(name), isOldVersion()));
                        }
                    } else {
                        if (!listFiles) {
                            list2add.add(new JcrFolderAPI(n, getArtefactPath().withSegment(name), isOldVersion()));
                        }
                    }
                }
            }
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to list nodes.", e);
        }
    }

    @Override
    public JcrEntityAPI getArtefact(String name) throws ProjectException {
        try {
            Node n = node().getNode(name);
            boolean isFolder;
            if (isOldVersion()) {
                Node frozenNode = NodeUtil.normalizeOldNode(n, getVersion());
                isFolder = frozenNode.getProperty("jcr:frozenPrimaryType").getString().equals(JcrNT.NT_FOLDER);
                n = frozenNode;
            } else {
                isFolder = n.isNodeType(JcrNT.NT_FOLDER);
            }
            if (isFolder) {
                return new JcrFolderAPI(n, getArtefactPath().withSegment(name), isOldVersion());
            } else {
                return new JcrFileAPI(n, getArtefactPath().withSegment(name), isOldVersion());
            }
        } catch (RepositoryException e) {
            throw new ProjectException("Failed to list nodes.", e);
        }
    }

    @Override
    public boolean hasArtefact(String name) {
        try {
            return node().hasNode(name);
        } catch (RepositoryException e) {
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
            }
            return false;
        }
    }

    @Override
    public Collection<? extends JcrEntityAPI> getArtefacts() {
        List<JcrEntityAPI> artefacts = new ArrayList<>();
        try {
            artefacts.addAll(getFolders());
            artefacts.addAll(getFiles());
        } catch (RRepositoryException e) {
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
            }
        }
        return artefacts;
    }

    @Override
    public boolean isFolder() {
        return true;
    }

    @Override
    public JcrFolderAPI getVersion(CommonVersion version) throws RRepositoryException {
        try {
            Node frozenNode = NodeUtil.getNode4Version(node(), version);
            return new JcrFolderAPI(frozenNode, getArtefactPath(), true);
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to get version for node.", e);
        }
    }
}
