package org.openl.rules.repository.jcr;

import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.openl.rules.repository.CommonVersion;
import org.openl.rules.repository.RFile;
import org.openl.rules.repository.exceptions.RRepositoryException;

/**
 * Implementation of JCR File.
 *
 * @author Aleh Bykhavets
 *
 */
public class JcrFile extends JcrEntity implements RFile {

    /**
     * Creates a new instance of file.
     *
     * @param parentNode parent node (files or folder)
     * @param nodeName name of new node
     * @return newly created instance
     * @throws RepositoryException if fails
     */
    protected static JcrFile createFile(Node parentNode, String nodeName) throws RepositoryException {
        Node n = NodeUtil.createFileNode(parentNode, nodeName);

        parentNode.save();
        n.save();

        return new JcrFile(n);
    }

    public JcrFile(Node node) throws RepositoryException {
        super(node);

        NodeUtil.checkNodeType(node, JcrNT.NT_FILE);
    }

    /** {@inheritDoc} */
    public InputStream getContent() throws RRepositoryException {
        return NodeUtil.getFileNodeContent(node());
    }

    /** {@inheritDoc} */
    public InputStream getContent4Version(CommonVersion version) throws RRepositoryException {
        try {
            Node frozen = NodeUtil.getNode4Version(node(), version);

            Node resNode = frozen.getNode("jcr:content");
            InputStream result = resNode.getProperty("jcr:data").getStream();

            return result;
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to get Content for specified version.", e);
        }
    }

    /** {@inheritDoc} */
    public String getMimeType() {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    public long getSize() {
        return NodeUtil.getFileNodeSize(node());
    }

    /** {@inheritDoc} */
    public void revertToVersion(String versionName) throws RRepositoryException {
        try {
            Node n = node();
            // TODO check whether we need here 'false' or 'true'
            n.restore(versionName, true);
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to revert to specified version.", e);
        }
    }

    /** {@inheritDoc} */
    public void setContent(InputStream inputStream) throws RRepositoryException {
        try {
            Node n = node();
            NodeUtil.smartCheckout(n, false);

            Node resNode = n.getNode("jcr:content");

            long lastModifiedTime = System.currentTimeMillis();
            Calendar lastModified = Calendar.getInstance();
            lastModified.setTimeInMillis(lastModifiedTime);

            resNode.setProperty("jcr:data", inputStream);
            resNode.setProperty("jcr:lastModified", lastModified);

            n.save();
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to set Content.", e);
        }
    }
}
