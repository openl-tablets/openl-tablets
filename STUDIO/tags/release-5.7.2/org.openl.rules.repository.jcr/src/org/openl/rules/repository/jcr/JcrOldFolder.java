package org.openl.rules.repository.jcr;

import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.openl.rules.repository.CommonVersion;
import org.openl.rules.repository.RFile;
import org.openl.rules.repository.RFolder;
import org.openl.rules.repository.exceptions.RRepositoryException;

public class JcrOldFolder extends JcrOldEntity implements RFolder {

    private CommonVersion projectVersion;

    public JcrOldFolder(JcrOldEntity parent, String name, Node node, CommonVersion projectVersion)
            throws RepositoryException {
        super(parent, name, node);
        checkNodeType(JcrNT.NT_FOLDER);

        this.projectVersion = projectVersion;
    }

    public RFile createFile(String name) throws RRepositoryException {
        notSupported();
        return null;
    }

    public RFolder createFolder(String name) throws RRepositoryException {
        notSupported();
        return null;
    }

    public List<RFile> getFiles() throws RRepositoryException {
        List<RFile> result = new LinkedList<RFile>();
        listNodes(result, true);
        return result;
    }

    public List<RFolder> getFolders() throws RRepositoryException {
        List<RFolder> result = new LinkedList<RFolder>();
        listNodes(result, false);
        return result;
    }

    // --- protected

    protected void listNodes(List list2add, boolean isFiles) throws RRepositoryException {
        try {
            NodeIterator ni = node().getNodes();
            while (ni.hasNext()) {
                Node n = ni.nextNode();
                String name = n.getName();

                Node frozenNode = NodeUtil.normalizeOldNode(n, projectVersion);
                String frozenNodeType = frozenNode.getProperty("jcr:frozenPrimaryType").getString();

                // TODO: use search? But looking through direct child nodes
                // seems faster
                if (JcrNT.NT_FOLDER.equals(frozenNodeType)) {
                    if (!isFiles) {
                        list2add.add(new JcrOldFolder(this, name, frozenNode, projectVersion));
                    }
                } else {
                    if (isFiles) {
                        list2add.add(new JcrOldFile(this, name, frozenNode));
                    }
                }
            }
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to list nodes.", e);
        }
    }
}
