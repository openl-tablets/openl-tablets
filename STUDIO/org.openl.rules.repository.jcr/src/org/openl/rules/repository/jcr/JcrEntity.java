package org.openl.rules.repository.jcr;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import org.openl.rules.repository.REntity;
import org.openl.rules.repository.RProperty;
import org.openl.rules.repository.RPropertyType;
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
    private HashMap<String, RProperty> properties;
    
    public JcrEntity(Node node) throws RepositoryException {
        this.node = node;
        name = node.getName();
        
        properties = new HashMap<String, RProperty>();
        initProperties();
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

    /** {@inheritDoc} */
    public void delete() throws RDeleteException {
        try {
            Node n = node();
            
            NodeUtil.smartCheckout(n, true);
            
            n.remove();
        } catch (RepositoryException e) {
            throw new RDeleteException("Failed to Delete", e);
        }
    }

    public Collection<RProperty> getProperties() {
        return properties.values();
    }
    
    public void addProperty(String name, RPropertyType type, Object value) throws RRepositoryException {
        try {
            NodeUtil.smartCheckout(node(), false);
        } catch (RepositoryException e) {
            throw new RRepositoryException("Internal error", e);
        }        

        if (hasProperty(name)) {
            removeProperty(name);
        }
        
        JcrProperty jp = new JcrProperty(this, name, type, value);
        properties.put(name, jp);
    }
    
    public void removeProperty(String name) throws RDeleteException {
        RProperty rp = properties.get(name);

        if (rp == null) {
            throw new RDeleteException("No such property {0}", null, name);
        }
        
        Node n = node();
        try {
            NodeUtil.smartCheckout(n, false);
        } catch (RepositoryException e) {
            throw new RDeleteException("Internal error", e);
        }        

        try {
            n.getProperty(name).remove();
        } catch (RepositoryException e) {
            throw new RDeleteException("Cannot remove property {0}", e, name);
        }        
        properties.remove(name);
    }
    
    public boolean hasProperty(String name) {
        return (properties.get(name) != null);
    }
    
    public RProperty getProperty(String name) throws RRepositoryException {
        RProperty rp = properties.get(name);
        
        if (rp == null) {
            throw new RRepositoryException("No such property {0}", null, name);
        }
        
        return rp;
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
    
    private static final String[] ALLOWED_PROPS = {"effectiveDate", "expirationDate", "LOB"};
    private void initProperties() throws RepositoryException {
        properties.clear();
        
        Node n = node();
        for (String s : ALLOWED_PROPS) {
            if (n.hasProperty(s)) {
                Property p = n.getProperty(s);

                JcrProperty prop = new JcrProperty(this, p);
                properties.put(prop.getName(), prop);
            }
        }
    }
}
