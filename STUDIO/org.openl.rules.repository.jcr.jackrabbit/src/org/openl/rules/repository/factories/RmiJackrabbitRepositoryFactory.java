package org.openl.rules.repository.factories;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.jackrabbit.rmi.client.ClientRepositoryFactory;
import org.openl.config.ConfigPropertyString;
import org.openl.config.ConfigSet;
import org.openl.rules.repository.exceptions.RRepositoryException;

/**
 * Remote Jackrabbit Repository Factory. It accesses remote Jackrabbit instance
 * via RMI.
 *
 * @author Aleh Bykhavets
 *
 */
public class RmiJackrabbitRepositoryFactory extends AbstractJackrabbitRepositoryFactory {
    private ConfigPropertyString confRmiUrl = new ConfigPropertyString(
            "repository.jackrabbit.rmi.url", "//localhost:1099/jackrabbit.repository");

    /** {@inheritDoc} */
    @Override
    public void initialize(ConfigSet confSet) throws RRepositoryException {
        super.initialize(confSet);
        confSet.updateProperty(confRmiUrl);
        ClientRepositoryFactory clientRepositoryFactory = new ClientRepositoryFactory();

        try {
            Repository repository;
            try {
                repository = clientRepositoryFactory.getRepository(confRmiUrl.getValue());
            } catch (Exception e) {
                throw new RepositoryException(e);
            }

            setRepository(repository, "Jackrabbit RMI " + confRmiUrl.getValue());
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to initialize JCR: " + e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void initNodeTypes(NodeTypeManager ntm) throws RepositoryException {
        throw new RepositoryException("Cannot initialize node types via RMI."
                + "\nPlease, add OpenL node types definition manually or via command line tool.");
    }

    public ConfigPropertyString getConfRmiUrl() {
        return confRmiUrl;
    }

    public void setConfRmiUrl(ConfigPropertyString confRmiUrl) {
        this.confRmiUrl = confRmiUrl;
    }

}
