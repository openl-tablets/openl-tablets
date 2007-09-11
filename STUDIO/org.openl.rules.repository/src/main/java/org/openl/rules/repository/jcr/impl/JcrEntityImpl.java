package org.openl.rules.repository.jcr.impl;

import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import org.openl.rules.repository.jcr.JcrEntity;
import org.openl.rules.repository.jcr.JcrVersion;

/**
 * Implementation of JCR Entity.
 * It is linked with node in JCR implementation, always.
 * 
 * @author Aleh Bykhavets
 *
 */
public class JcrEntityImpl implements JcrEntity {

	/** node in JCR that corresponds to this entity */
	private Node node;
	
	public JcrEntityImpl(Node node) {
		this.node = node;
	}
	
    /** {@inheritDoc} */
	public String getName() throws RepositoryException {
		return node.getName();
	}
	
    /** {@inheritDoc} */
	public List<JcrVersion> getVersions() throws RepositoryException {
		VersionHistory vh = node().getVersionHistory();
		VersionIterator vi = vh.getAllVersions();
		
		LinkedList<JcrVersion> result = new LinkedList<JcrVersion>();
		while (vi.hasNext()) {
			Version v = vi.nextVersion();
			
			if (NodeUtil.isRootVersion(v)) {
				//TODO Shall we add first (0) version? (It is marker like, no real values)
			} else {
				JcrVersionImpl jvi = new JcrVersionImpl(v);
				result.add(jvi);
			}
		}
		
		return result;
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
	 * @throws RepositoryException
	 */
	protected void checkNodeType(String nodeType) throws RepositoryException {
		if (!node.isNodeType(nodeType)) {
			throw new RepositoryException("Invalid NodeType. Expects " + nodeType);
		}
	}
}
