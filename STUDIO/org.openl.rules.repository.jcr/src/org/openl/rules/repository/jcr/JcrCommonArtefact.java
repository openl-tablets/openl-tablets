package org.openl.rules.repository.jcr;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.openl.rules.repository.exceptions.RRepositoryException;

public class JcrCommonArtefact {
    private String name;
    private Node node;
    
    protected JcrCommonArtefact(Node node) throws RepositoryException {
        this.node = node;
        
        name = node.getName();
    }
    
    public String getName() {
        return name;
    }
    
    // --- protected 
    
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

    protected void delete() throws RRepositoryException {
        try {
            Node n = node();

            NodeUtil.smartCheckout(n, true);

            n.remove();
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to Delete", e);
        }
    }
}
