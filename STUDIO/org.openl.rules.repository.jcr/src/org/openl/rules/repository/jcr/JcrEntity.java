package org.openl.rules.repository.jcr;

import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import org.openl.rules.repository.REntity;
import org.openl.rules.repository.RVersion;
import org.openl.rules.repository.exceptions.RModifyException;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.repository.exceptions.RDeleteException;

/**
 * Implementation of JCR Entity.
 * It is linked with node in JCR implementation, always.
 * 
 * @author Aleh Bykhavets
 *
 */
public class JcrEntity implements REntity {

    /** node in JCR that corresponds to this entity */
    private Node node;
    private String name;

    public JcrEntity(Node node) throws RepositoryException {
        this.node = node;
        name = node.getName();
    }

    /** {@inheritDoc} */
    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    public List<RVersion> getVersionHistory() throws RRepositoryException {
        try {
            VersionHistory vh = node().getVersionHistory();
            VersionIterator vi = vh.getAllVersions();
            LinkedList<RVersion> result = new LinkedList<RVersion>();
            while (vi.hasNext()) {
                Version v = vi.nextVersion();

                if (NodeUtil.isRootVersion(v)) {
                    //TODO Shall we add first (0) version? (It is marker like, no real values)
                } else {
                    JcrVersion jvi = new JcrVersion(v);
                    result.add(jvi);
                }
            }
            return result;
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to get Version History", e);
        }        
    }

    // ------ protected methods ------

    /**
     * Returns node in JCR that this entity is mapped on. 
     * 
     * @return corresponding JCR node
     */
    protected Node node() {
        return node;
    }

    /**
     * Checks whether type of the JCR node is correct.
     * 
     * @param nodeType expected node type
     * @throws RepositoryException if failed
     */
    protected void checkNodeType(String nodeType) throws RepositoryException {
        if (!node.isNodeType(nodeType)) {
            throw new RepositoryException("Invalid NodeType. Expects " + nodeType);
        }
    }

    public RVersion getBaseVersion() {
        try {
            Version v = node().getBaseVersion();
            RVersion result = new JcrVersion(v);
            return result;
        } catch (RepositoryException e) {
            e.printStackTrace();
            return null;
        }        
    }

    public String getPath() throws RRepositoryException {
        StringBuffer sb = new StringBuffer(128);
        try {
            buildRelPath(sb, node);
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to get relative path", e);
        }

        return sb.toString();
    }

    public void setName(String name) throws RModifyException {
        // TODO: FIX ME!!!
    }

    /** {@inheritDoc} */
    public void delete() throws RDeleteException {
        try {
            Node n = node();
            
            Node parent = n.getParent();
            NodeUtil.smartCheckout(node, true);
            
            n.remove();
            
            NodeUtil.smartCheckinParent(parent);
        } catch (RepositoryException e) {
            throw new RDeleteException("Failed to Delete", e);
        }
    }

    // ------ private ------

    private void buildRelPath(StringBuffer sb, Node n) throws RepositoryException {
        if (!n.isNodeType(JcrNT.NT_PROJECT)) {
            buildRelPath(sb, n.getParent());
        }

        if (!n.isNodeType(JcrNT.NT_FILES)) {
            sb.append('/');
            sb.append(n.getName());
        }
    }
}
