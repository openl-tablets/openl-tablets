package org.openl.rules.repository;

import javax.jcr.RepositoryException;

import org.openl.rules.repository.jcr.JcrRepository;

/**
 * Interface for concrete repository factories.
 * 
 * @author Aleh Bykhavets
 *
 */
public interface JcrRepositoryFactory {
	/**
	 * Gets new instance of JCR Repository.
	 * 
	 * @return new instance of JCR Repository
	 * @throws RepositoryException
	 */
	public JcrRepository getRepositoryInstance() throws RepositoryException;
	/**
	 * Initialize factory.
	 * 
	 * @param props properties
	 * @throws RepositoryException
	 */
	public void initialize(SmartProps props) throws RepositoryException;
}
