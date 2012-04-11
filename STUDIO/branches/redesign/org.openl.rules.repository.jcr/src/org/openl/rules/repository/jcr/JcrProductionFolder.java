package org.openl.rules.repository.jcr;

import org.openl.rules.repository.RFile;
import org.openl.rules.repository.RFolder;
import org.openl.rules.repository.exceptions.RRepositoryException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import java.util.LinkedList;
import java.util.List;

public class JcrProductionFolder extends JcrProductionEntity implements RFolder {
    /**
     * Creates new folder.
     *
     * @param parentNode parent node (files or other folder)
     * @param nodeName name of new node
     * @return newly created folder
     * @throws RepositoryException if fails
     */
    protected static JcrProductionFolder createFolder(Node parentNode, String nodeName) throws RepositoryException {
        Node n = NodeUtil.createNode(parentNode, nodeName, JcrNT.NT_PROD_FOLDER, false);
        parentNode.save();
        n.save();

        return new JcrProductionFolder(n);
    }

    public JcrProductionFolder(Node node) throws RepositoryException {
        super(node);
    }

    /**
     * Creates file to the folder.
     *
     * @param name name of new file
     * @return newly created file
     * @throws RRepositoryException if failed
     */
    public RFile createFile(String name) throws RRepositoryException {
        try {
            return JcrProductionFile.createFile(node(), name);
        } catch (RepositoryException e) {
            throw new RRepositoryException("failed to create file", e);
        }
    }

    /**
     * Creates sub folder to the folder.
     *
     * @param name name of new folder
     * @return newly created folder
     * @throws RRepositoryException if failed
     */
    public RFolder createFolder(String name) throws RRepositoryException {
        try {
            return createFolder(node(), name);
        } catch (RepositoryException e) {
            throw new RRepositoryException("failed to create subfolder", e);
        }
    }

    /**
     * Gets list of files from the folder.
     *
     * @return list of files
     */
    public List<RFile> getFiles() throws RRepositoryException {
        List<RFile> result = new LinkedList<RFile>();
        listNodes(result, true);
        return result;
    }

    /**
     * Gets list of sub folders. It returns direct descendants only.
     *
     * @return list of sub folders.
     */
    public List<RFolder> getFolders() throws RRepositoryException {
        List<RFolder> result = new LinkedList<RFolder>();
        listNodes(result, false);
        return result;

    }

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
                if (n.isNodeType(JcrNT.NT_PROD_FOLDER)) {
                    if (!isFiles) {
                        list2add.add(new JcrProductionFolder(n));
                    }
                } else {
                    if (isFiles) {
                        list2add.add(new JcrProductionFile(n));
                    }
                }
            }
        } catch (RepositoryException e) {
            throw new RRepositoryException("failed to list nodes", e);
        }
    }

}
