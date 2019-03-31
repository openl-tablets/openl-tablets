package org.openl.rules.repository.factories;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.jackrabbit.rmi.client.ClientRepositoryFactory;
import org.openl.rules.repository.exceptions.RRepositoryException;

/**
 * Remote Jackrabbit Repository Factory. It accesses remote Jackrabbit instance via RMI.
 *
 * @author Aleh Bykhavets
 *
 */
public class RmiJackrabbitRepositoryFactory extends AbstractJcrRepositoryFactory {

    /** {@inheritDoc} */
    @Override
    public void initialize() throws RRepositoryException {
        ClientRepositoryFactory clientRepositoryFactory = new ClientRepositoryFactory();

        try {
            Repository repository;
            String rmiUrl = this.uri;
            try {
                repository = clientRepositoryFactory.getRepository(rmiUrl);
            } catch (Exception e) {
                throw new RepositoryException(e);
            }

            setRepository(repository);
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to initialize JCR: " + e.getMessage(), e);
        }
        super.initialize();
    }

    /** {@inheritDoc} */
    @Override
    protected void initNodeTypes(NodeTypeManager ntm) throws RepositoryException {
        throw new RepositoryException(
            "Cannot initialize node types via RMI." + "\nPlease, add OpenL node types definition manually or via command line tool.");
    }
}
