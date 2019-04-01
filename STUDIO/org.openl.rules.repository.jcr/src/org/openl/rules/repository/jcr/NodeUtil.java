package org.openl.rules.repository.jcr;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.repository.api.ArtefactProperties;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JCR Node Utility
 *
 * @author Aleh Bykhavets
 */
public final class NodeUtil {
    private NodeUtil() {
    }

    public static boolean isVersionable(Node node) throws RepositoryException {
        return node.isNodeType(JcrNT.MIX_VERSIONABLE);
    }

    protected static Node createFileNode(Node parentNode, String nodeName) throws RepositoryException {
        Node n = createNode(parentNode, nodeName, JcrNT.NT_FILE, true);
        setupFileNode(n);
        return n;
    }

    /**
     * Creates node of given node type.
     *
     * @param parentNode parent node, where new node is going to be added
     * @param name name of new node
     * @param type node type of new node
     * @param isVersionable whether new node is versionable
     * @return reference on newly created node
     * @throws RepositoryException if operation failed
     */
    public static Node createNode(Node parentNode,
            String name,
            String type,
            boolean isVersionable) throws RepositoryException {
        if (parentNode.hasNode(name)) {
            throw new RepositoryException("Node '" + name + "' exists at '" + parentNode.getPath() + "' already!");
        }

        Node p = parentNode;
        while (p != null) {
            if (p.isCheckedOut()) {
                break;
            } else {
                if (p.isNodeType(JcrNT.MIX_VERSIONABLE)) {
                    p.checkout();
                    break;
                }
            }

            p = p.getParent();
        }

        Node n = parentNode.addNode(name, type);
        if (isVersionable) {
            n.addMixin(JcrNT.MIX_VERSIONABLE);
        }

        return n;
    }

    protected static InputStream getFileNodeContent(Node node) throws RRepositoryException {
        try {
            return node.getNode(ArtefactProperties.PROP_RES_CONTENT)
                .getProperty(ArtefactProperties.PROP_RES_DATA)
                .getStream();
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to get Content!", e);
        }
    }

    protected static long getFileNodeSize(Node node) {
        long result;
        final Logger log = LoggerFactory.getLogger(NodeUtil.class);
        try {
            Node resNode = node.getNode(ArtefactProperties.PROP_RES_CONTENT);
            result = resNode.getProperty(ArtefactProperties.PROP_RES_DATA).getLength();
        } catch (RepositoryException e) {
            log.warn("getFileNodeSize", e);
            result = -1;
        }

        return result;
    }

    protected static Node getNode4Version(Node node, CommonVersion version) throws RepositoryException {
        Node result = null;

        VersionHistory vh = node.getVersionHistory();
        VersionIterator vi = vh.getAllVersions();

        while (vi.hasNext()) {
            Version jcrVersion = vi.nextVersion();

            if (NodeUtil.isRootVersion(jcrVersion)) {
                // TODO Shall we add first (0) version? (It is marker like, no
                // real values)
            } else {
                JcrVersion jvi = new JcrVersion(jcrVersion);
                CommonVersionImpl cv = new CommonVersionImpl(jvi.getRevision());

                if (cv.compareTo(version) == 0) {
                    result = jcrVersion.getNode(JcrNT.FROZEN_NODE);
                    break;
                }
            }
        }

        if (result == null) {
            throw new RepositoryException("Cannot find version '" + version.getVersionName() + "'!");
        }

        return result;
    }

    /**
     * Inquire whether given version is 'root'.
     *
     * @param v version to be checked
     * @return <code>true</code> if the version is root; <code>false</code> otherwise;
     * @throws RepositoryException if operation failed
     */
    protected static boolean isRootVersion(Version v) throws RepositoryException {
        String name = v.getName();
        return "jcr:rootVersion".equals(name);
    }

    protected static Node normalizeOldNode(Node node, CommonVersion version) throws RepositoryException {
        if (node.isNodeType(JcrNT.NT_FROZEN_NODE)) {
            // all is OK
            return node;
        }

        if (!node.isNodeType("nt:versionedChild")) {
            // ??? unknown
            return node;
        }

        Node versionHistoryNode = node.getProperty("jcr:childVersionHistory").getNode();

        int projectRevision = Integer.parseInt(version.getRevision());

        int correctVRev = -1;
        Node correctVNode = null;

        NodeIterator versions = versionHistoryNode.getNodes();
        while (versions.hasNext()) {
            Node versionNode = versions.nextNode();
            if (!versionNode.isNodeType("nt:version")) {
                continue;
            }

            // old nodes, should be 1 per versionNode
            NodeIterator oldNodes = versionNode.getNodes();
            while (oldNodes.hasNext()) {
                Node oldNode = oldNodes.nextNode();

                int nodeRevision = 0;
                if (oldNode.hasProperty(ArtefactProperties.PROP_REVISION)) {
                    nodeRevision = (int) oldNode.getProperty(ArtefactProperties.PROP_REVISION).getLong();
                }

                if (nodeRevision <= projectRevision) {
                    if (nodeRevision > correctVRev) {
                        correctVNode = oldNode;
                        correctVRev = nodeRevision;
                    }
                }
            }
        }

        return correctVNode;
    }

    private static void setupFileNode(Node n) throws RepositoryException {
        String mimeType = "text/plain";
        String encoding = "UTF-8";
        long lastModifiedTime = System.currentTimeMillis();
        Calendar lastModified = Calendar.getInstance();
        lastModified.setTimeInMillis(lastModifiedTime);

        // create the file node - see section 6.7.22.6 of the spec
        // create the mandatory child node - jcr:content
        Node resNode = n.addNode(ArtefactProperties.PROP_RES_CONTENT, JcrNT.NT_RESOURCE);
        resNode.setProperty(ArtefactProperties.PROP_RES_MIMETYPE, mimeType);
        resNode.setProperty(ArtefactProperties.PROP_RES_ENCODING, encoding);
        // TODO add real init-content
        resNode.setProperty(ArtefactProperties.PROP_RES_DATA, new ByteArrayInputStream(new byte[0]));
        resNode.setProperty(ArtefactProperties.PROP_RES_LASTMODIFIED, lastModified);
    }

    /**
     * Checkout node and parent (if needed).
     *
     * @param node reference on node to be checked in
     * @param openParent whether parent should be checked out
     * @throws RepositoryException if operation failed
     */
    public static void smartCheckout(Node node, boolean openParent) throws RepositoryException {
        if (!node.isCheckedOut()) {
            if (node.isNodeType(JcrNT.MIX_VERSIONABLE)) {
                node.checkout();
            }
        }

        if (openParent) {
            Node parentNode = node.getParent();

            if (!parentNode.isCheckedOut()) {
                parentNode.checkout();
            }
        }
    }
}
