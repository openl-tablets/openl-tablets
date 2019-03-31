package org.openl.rules.repository.jcr;

import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.openl.rules.common.ArtefactPath;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectException;
import org.openl.rules.repository.api.ArtefactProperties;
import org.openl.rules.repository.api.ResourceAPI;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of JCR File.
 * 
 * @author Aleh Bykhavets
 * 
 */
public class JcrFileAPI extends JcrEntityAPI implements ResourceAPI {
    private final Logger log = LoggerFactory.getLogger(JcrEntityAPI.class);

    /**
     * Creates a new instance of file.
     * 
     * @param parent parent node (files or folder)
     * @param nodeName name of new node
     * @return newly created instance
     * @throws RepositoryException if fails
     */
    protected static JcrFileAPI createFile(JcrFolderAPI parent, String nodeName, ArtefactPath path)
            throws RepositoryException {
        Node parentNode = parent.node();
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
    @Override
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
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
            }
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public long getSize() {
        return NodeUtil.getFileNodeSize(node());
    }

    /** {@inheritDoc} */
    @Override
    public void setContent(InputStream inputStream) throws ProjectException {
        try {
            Node n = node();
            NodeUtil.smartCheckout(n, false);

            Node resNode = n.getNode(ArtefactProperties.PROP_RES_CONTENT);

            long lastModifiedTime = System.currentTimeMillis();
            Calendar lastModified = Calendar.getInstance();
            lastModified.setTimeInMillis(lastModifiedTime);

            resNode.setProperty(ArtefactProperties.PROP_RES_DATA, inputStream);
            resNode.setProperty("jcr:lastModified", lastModified);

            n.save();
        } catch (Exception e) {
            throw new ProjectException("", e);
        }
    }
}
