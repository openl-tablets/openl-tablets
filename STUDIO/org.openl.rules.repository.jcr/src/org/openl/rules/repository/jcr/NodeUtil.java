package org.openl.rules.repository.jcr;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import org.openl.rules.repository.CommonVersion;
import org.openl.rules.repository.CommonVersionImpl;


/**
 * JCR Node Utility
 * 
 * @author Aleh Bykhavets
 *
 */
public class NodeUtil {

    /**
     * Inquire whether given version is 'root'.
     * 
     * @param v version to be checked
     * @return <code>true</code> if the version is root;
     *         <code>false</code> otherwise;
     * @throws RepositoryException if operation failed
     */
    protected static boolean isRootVersion(Version v) throws RepositoryException {
        String name = v.getName();
        return "jcr:rootVersion".equals(name);
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
    protected static Node createNode(Node parentNode, String name, String type, boolean isVersionable) throws RepositoryException {
        if (parentNode.hasNode(name)) {
            throw new RepositoryException("Node '" + name + "' exists at '" + parentNode.getPath() + "' already.");
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

    /**
     * Checkout node and parent (if needed).
     * 
     * @param node reference on node to be checked in
     * @param openParent whether parent should be checked out
     * @throws RepositoryException if operation failed
     */
    protected static void smartCheckout(Node node, boolean openParent) throws RepositoryException {
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
    
    protected static Node getNode4Version(Node node, CommonVersion version) throws RepositoryException {
        Node result = null;

        VersionHistory vh = node.getVersionHistory();
        VersionIterator vi = vh.getAllVersions();
        
        while (vi.hasNext()) {
            Version jcrVersion = vi.nextVersion();

            if (NodeUtil.isRootVersion(jcrVersion)) {
                //TODO Shall we add first (0) version? (It is marker like, no real values)
            } else {
                JcrVersion jvi = new JcrVersion(jcrVersion);
                CommonVersionImpl cv = new CommonVersionImpl(jvi.getMajor(), jvi.getMinor(), jvi.getRevision());
                
                if (cv.compareTo(version) == 0) {
                    result = jcrVersion.getNode("jcr:frozenNode");
                    break;
                }
            }
        }
        
        if (result == null) {
            throw new RepositoryException("Cannot find version " + version.getVersionName());
        }
        
        return result;
    }
}
