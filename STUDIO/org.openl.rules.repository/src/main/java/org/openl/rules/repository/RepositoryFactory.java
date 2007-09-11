package org.openl.rules.repository;

import javax.jcr.RepositoryException;

import org.openl.rules.repository.jcr.JcrRepository;

/**
 * Repository Factory.  It is Abstract Factory.
 * <p>
 * Takes init values from repository.properties file.
 * 
 * @author Aleh Bykhavets
 *
 */
public class RepositoryFactory {
	public static final String PROP_FILE = "repository.properties";
	public static final String PROP_JCR_TYPE = "JCR.type";
	
	private static JcrRepositoryFactory repFactory;

	// TODO: add support for other types of concrete factories
	public static synchronized JcrRepository getRepositoryInstance() throws RepositoryException {
		if (repFactory == null) {
			initFactory();
		}
		
		return repFactory.getRepositoryInstance();
	}
	
	private static void initFactory() throws RepositoryException {
		SmartProps props = new SmartProps(PROP_FILE);
		
		String className = props.getStr(PROP_JCR_TYPE);
		try {
			Class klass = Class.forName(className);
			Object obj = klass.newInstance();
			repFactory = (JcrRepositoryFactory) obj;
			// initialize
			repFactory.initialize(props);
		} catch (Exception e) {
			throw new RepositoryException("Failed to initialize Factory: " + e.getMessage(), e);
		}
	}
}
