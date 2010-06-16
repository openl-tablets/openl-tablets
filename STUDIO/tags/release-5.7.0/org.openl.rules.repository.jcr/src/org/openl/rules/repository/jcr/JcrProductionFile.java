package org.openl.rules.repository.jcr;

import org.openl.rules.repository.RFile;
import org.openl.rules.repository.CommonVersion;
import org.openl.rules.repository.exceptions.RRepositoryException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.InputStream;
import java.util.Calendar;

public class JcrProductionFile extends JcrProductionEntity implements RFile {
    /**
     * Creates a new instance of file.
     *
     * @param parentNode parent node (files or folder)
     * @param nodeName name of new node
     * @return newly created instance
     * @throws RepositoryException if fails
     */
    protected static JcrProductionFile createFile(Node parentNode, String nodeName) throws RepositoryException {
        Node n = NodeUtil.createProdFileNode(parentNode, nodeName);

        parentNode.save();
        n.save();

        return new JcrProductionFile(n);
    }

    public JcrProductionFile(Node n) throws RepositoryException {
        super(n);
    }

    /**
     * Gets content of the file. It is highly apreciated to close stream right
     * after it is no longer needed.
     *
     * @return content stream with content of file
     * @throws org.openl.rules.repository.exceptions.RRepositoryException if
     *             failed
     */
    public InputStream getContent() throws RRepositoryException {
        return NodeUtil.getFileNodeContent(node());
    }

    /**
     * Returns content of specified version of the file. As production files are
     * not versioned, <code>version</code> parameter is ignored.
     *
     * @param version ignored
     * @return content of specified version
     * @throws org.openl.rules.repository.exceptions.RRepositoryException if
     *             failed
     */
    public InputStream getContent4Version(CommonVersion version) throws RRepositoryException {
        return getContent();
    }

    /**
     * Gets mime type of the file.
     *
     * @return mime type
     */
    public String getMimeType() {
        return null;
    }

    /**
     * Returns size of the file's content in bytes.
     *
     * @return size of content or <code>-1</code> if cannot determine it.
     */
    public long getSize() {
        return NodeUtil.getFileNodeSize(node());
    }

    /**
     * Reverts the file to specified version.
     *
     * @param versionName name of version
     * @throws org.openl.rules.repository.exceptions.RRepositoryException if
     *             failed
     */
    public void revertToVersion(String versionName) throws RRepositoryException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets/Updates content of the file. At the end input stream will be closed.
     *
     * @param inputStream stream with new content of the file
     * @throws RRepositoryException if failed
     */
    public void setContent(InputStream inputStream) throws RRepositoryException {
        try {
            Node n = node();

            Node resNode = n.getNode("jcr:content");

            long lastModifiedTime = System.currentTimeMillis();
            Calendar lastModified = Calendar.getInstance();
            lastModified.setTimeInMillis(lastModifiedTime);

            resNode.setProperty("jcr:data", inputStream);
            resNode.setProperty("jcr:lastModified", lastModified);

            n.save();
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to set Content", e);
        }

    }
}
