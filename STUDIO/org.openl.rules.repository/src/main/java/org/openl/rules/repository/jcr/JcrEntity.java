package org.openl.rules.repository.jcr;

import javax.jcr.RepositoryException;

/**
 * Defines JCR Entity.
 * It is not used directly.
 * 
 * @author Aleh Bykhavets
 *
 */
public interface JcrEntity {
	/**
	 * Returns name of the JCR entity.
	 * 
	 * @return name of the entity
	 * @throws RepositoryException
	 */
	public String getName() throws RepositoryException;
}
