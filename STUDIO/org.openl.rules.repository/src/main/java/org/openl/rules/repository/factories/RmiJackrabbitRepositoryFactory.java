package org.openl.rules.repository.factories;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.jackrabbit.rmi.client.ClientRepositoryFactory;
import org.openl.rules.repository.SmartProps;

public class RmiJackrabbitRepositoryFactory extends AbstractRepositoryFactory {
	public static final String PROP_RMI_URL = "JCR.rmi.url";
	public static final String DEF_RMI_URL = "//localhost:1099/jackrabbit.repository";

	private String rmiUrl;
	
    /** {@inheritDoc} */
	public void initialize(SmartProps props) throws RepositoryException {
		super.initialize(props);
		
		rmiUrl = props.getStr(PROP_RMI_URL, DEF_RMI_URL);

		ClientRepositoryFactory clientRepositoryFactory = new ClientRepositoryFactory();
		Repository repository;
		try {
			repository = clientRepositoryFactory.getRepository(rmiUrl);
		} catch (Exception e) {
			throw new RepositoryException(e);
		}
		
		setRepository(repository);
	}

    /** {@inheritDoc} */
	protected void initNodeTypes(NodeTypeManager ntm) throws RepositoryException {
		throw new RepositoryException("Cannot initialize node types via RMI." 
				+ "\nPlease, add OpenL node types definition manually or via command line tool.");
	}
}
