package org.openl.rules.repository.jcr;

import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.value.BinaryImpl;
import org.openl.rules.common.ArtefactPath;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectException;
import org.openl.rules.repository.api.ResourceAPI;
import org.openl.rules.repository.exceptions.RRepositoryException;

/**
 * Implementation of JCR File.
 * 
 * @author Aleh Bykhavets
 * 
 */
public class JcrFileAPI extends JcrEntityAPI implements ResourceAPI {

    /**
     * Creates a new instance of file.
     * 
     * @param parentNode parent node (files or folder)
     * @param nodeName name of new node
     * @return newly created instance
     * @throws RepositoryException if fails
     */
    protected static JcrFileAPI createFile(Node parentNode, String nodeName, ArtefactPath path)
            throws RepositoryException {
        Node n = NodeUtil.createFileNode(parentNode, nodeName);

        parentNode.save();
        n.save();

        return new JcrFileAPI(n, path, false);
    }

    public JcrFileAPI(Node node, ArtefactPath path, boolean oldVersion) throws RepositoryException {
        super(node, path, oldVersion);

        // NodeUtil.checkNodeType(node, JcrNT.NT_FILE);
    }

    public JcrFileAPI(Node node, ArtefactPath path) throws RepositoryException {
        this(node, path, false);
    }

    /** {@inheritDoc} */
    public InputStream getContent() throws ProjectException {
        try {
            return NodeUtil.getFileNodeContent(node());
        } catch (RRepositoryException e) {
            throw new ProjectException("", e);
        }
    }

    @Override
    public JcrFileAPI getVersion(CommonVersion version) {
        try {
            Node frozen = NodeUtil.getNode4Version(node(), version);
            return new JcrFileAPI(frozen, getArtefactPath(), true);
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    /** {@inheritDoc} */
    public String getResourceType() {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    public long getSize() {
        return NodeUtil.getFileNodeSize(node());
    }

    /** {@inheritDoc} */
    public void setContent(InputStream inputStream) throws ProjectException {
        try {
            Node n = node();
            NodeUtil.smartCheckout(n, false);

            Node resNode = n.getNode("jcr:content");

            long lastModifiedTime = System.currentTimeMillis();
            Calendar lastModified = Calendar.getInstance();
            lastModified.setTimeInMillis(lastModifiedTime);

            resNode.setProperty("jcr:data", new BinaryImpl(inputStream));
            resNode.setProperty("jcr:lastModified", lastModified);

            n.save();
        } catch (Exception e) {
            throw new ProjectException("", e);
        }
    }
}
