package org.openl.rules.repository.jcr;

import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.openl.rules.repository.RFile;
import org.openl.rules.repository.RFolder;
import org.openl.rules.repository.exceptions.RRepositoryException;

/**
 * Implementation for JCR Folder.
 *
 * @author Aleh Bykhavets
 *
 */
public class JcrFolder extends JcrEntity implements RFolder {

    /**
     * Creates new folder.
     *
     * @param parentNode parent node (files or other folder)
     * @param nodeName name of new node
     * @return newly created folder
     * @throws RepositoryException if fails
     */
    protected static JcrFolder createFolder(Node parentNode, String nodeName) throws RepositoryException {
        Node n = NodeUtil.createNode(parentNode, nodeName, JcrNT.NT_FOLDER, true);
        parentNode.save();
        n.save();

        return new JcrFolder(n);
    }

    public JcrFolder(Node node) throws RepositoryException {
        super(node);

        NodeUtil.checkNodeType(node, JcrNT.NT_FOLDER);
    }

    /** {@inheritDoc} */
    public RFile createFile(String name) throws RRepositoryException {
        try {
            return JcrFile.createFile(node(), name);
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to Create File.", e);
        }
    }

    /** {@inheritDoc} */
    public RFolder createFolder(String name) throws RRepositoryException {
        try {
            return JcrFolder.createFolder(node(), name);
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to Create Sub Folder.", e);
        }
    }

    /** {@inheritDoc} */
    public List<RFile> getFiles() throws RRepositoryException {
        List<RFile> result = new LinkedList<RFile>();
        listNodes(result, true);
        return result;
    }

    /** {@inheritDoc} */
    public List<RFolder> getFolders() throws RRepositoryException {
        List<RFolder> result = new LinkedList<RFolder>();
        listNodes(result, false);
        return result;
    }

    // ------ private methods ------

    /**
     * Lists nodes.
     *
     * @param list2add list to which nodes should be added
     * @param isFiles whether return only files or only folders
     * @throws RRepositoryException if failed
     */
    private void listNodes(List list2add, boolean isFiles) throws RRepositoryException {
        try {
            NodeIterator ni = node().getNodes();
            while (ni.hasNext()) {
                Node n = ni.nextNode();

                // TODO: use search? But looking through direct child nodes
                // seems faster
                if (n.isNodeType(JcrNT.NT_FOLDER)) {
                    if (!isFiles) {
                        list2add.add(new JcrFolder(n));
                    }
                } else {
                    if (isFiles) {
                        list2add.add(new JcrFile(n));
                    }
                }
            }
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to list nodes.", e);
        }
    }
}
