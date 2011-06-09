package org.openl.rules.repository.factories;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.jackrabbit.jcr2spi.RepositoryImpl;
import org.openl.config.ConfigPropertyString;
import org.openl.config.ConfigSet;
import org.openl.rules.repository.exceptions.RRepositoryException;

/**
 * 
 * @author PUdalau
 */
public class WebDavJacrabbitRepositoryFactory extends AbstractJackrabbitRepositoryFactory {
    private ConfigPropertyString confWebdavUrl = new ConfigPropertyString("repository.jackrabbit.webdav.url",
            "http://localhost:8080/jcr/server/");

    /** {@inheritDoc} */
    @Override
    public void initialize(ConfigSet confSet) throws RRepositoryException {
        super.initialize(confSet);
        confSet.updateProperty(confWebdavUrl);

        try {
            Repository repository;
            try {
                repository = RepositoryImpl.create(new DavexRepositoryConfigImpl(confWebdavUrl.getValue()));
            } catch (Exception e) {
                throw new RepositoryException(e);
            }

            setRepository(repository, "Jackrabbit WebDav " + confWebdavUrl.getValue());
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to initialize JCR: " + e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void initNodeTypes(NodeTypeManager ntm) throws RepositoryException {
        throw new RepositoryException("Cannot initialize node types via WebDav."
                + "\nPlease, add OpenL node types definition manually or via command line tool.");
    }

    public ConfigPropertyString getConfWebdavUrl() {
        return confWebdavUrl;
    }

    public void setConfWebdavUrl(ConfigPropertyString confWebdavUrl) {
        this.confWebdavUrl = confWebdavUrl;
    }

}
