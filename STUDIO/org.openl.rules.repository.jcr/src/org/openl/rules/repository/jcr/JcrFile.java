package org.openl.rules.repository.jcr;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;

import org.openl.rules.repository.RFile;
import org.openl.rules.repository.exceptions.RModifyException;
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
        Node n = NodeUtil.createNode(parentNode, nodeName, JcrNT.NT_FILE, true);

        String mimeType = "text/plain";
        String encoding = "UTF-8";
        long lastModifiedTime = System.currentTimeMillis();
        Calendar lastModified = Calendar.getInstance ();
        lastModified.setTimeInMillis(lastModifiedTime);

        //create the file node - see section 6.7.22.6 of the spec
        //create the mandatory child node - jcr:content
        Node resNode = n.addNode (JcrNT.PROP_RES_CONTENT, JcrNT.NT_RESOURCE);
        resNode.setProperty (JcrNT.PROP_RES_MIMETYPE, mimeType);
        resNode.setProperty (JcrNT.PROP_RES_ENCODING, encoding);
        // TODO add real init-content
        resNode.setProperty (JcrNT.PROP_RES_DATA, new ByteArrayInputStream (new byte[0]));
        resNode.setProperty (JcrNT.PROP_RES_LASTMODIFIED, lastModified);

        parentNode.save();
        n.save();

        return new JcrFile(n);
    }

    public JcrFile(Node node) throws RepositoryException {
        super(node);

        checkNodeType(JcrNT.NT_FILE);
    }

    /** {@inheritDoc} */
    public String getMimeType() {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    public long getSize() {
        long result;

        try {
            Node n = node();
            Node resNode = n.getNode ("jcr:content");
            result = resNode.getProperty("jcr:data").getLength();
        } catch (RepositoryException e) {
            // TODO: log exception
            result = -1;
        }

        return result;
    }

    /** {@inheritDoc} */ 
    public void setContent(InputStream inputStream) throws RModifyException {
        try {
            Node n = node();
            NodeUtil.smartCheckout(n, false);

            Node resNode = n.getNode ("jcr:content");

            long lastModifiedTime = System.currentTimeMillis();
            Calendar lastModified = Calendar.getInstance ();
            lastModified.setTimeInMillis(lastModifiedTime);

            resNode.setProperty ("jcr:data", inputStream);
            resNode.setProperty ("jcr:lastModified", lastModified);

            //FIXME: I don't think we really need this. It's good to use creation time of particular version
            n.setProperty (JcrNT.PROP_MODIFIED_TIME, lastModified);

            n.save();
        } catch (RepositoryException e) {
            throw new RModifyException("Failed to set Content", e);
        }
    }

    /** {@inheritDoc} */
    public InputStream getContent() throws RRepositoryException {
        try {
            Node n = node();
            Node resNode = n.getNode ("jcr:content");
            InputStream result = resNode.getProperty("jcr:data").getStream();

            return result;
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to get Content", e);
        }
    }

    /** {@inheritDoc} */
    public InputStream getContent4Version(String versionName) throws RRepositoryException {
        try {
            Node n = node();
            Version v = n.getVersionHistory().getVersion(versionName);
            
            Node frozen = v.getNode("jcr:frozenNode");

            Node resNode = frozen.getNode("jcr:content");
            InputStream result = resNode.getProperty("jcr:data").getStream();

            return result;
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to get Content for specified version", e);
        }
    }

    /** {@inheritDoc} */
    public void revertToVersion(String versionName) throws RRepositoryException {
        try {
            Node n = node();
            // TODO check whether we need here 'false' or 'true'
            n.restore(versionName, true);
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to revert to specified version", e);
        }
    }
}
