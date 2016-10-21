package org.openl.rules.repository.factories;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.jackrabbit.jcr2spi.RepositoryImpl;
import org.openl.rules.repository.exceptions.RRepositoryException;

/**
 * 
 * @author PUdalau
 */
public class WebDavRepositoryFactory extends AbstractJcrRepositoryFactory {

    /** {@inheritDoc} */
    @Override
    public void initialize(String uri, String login, String password, boolean designMode) throws RRepositoryException {
        super.initialize(uri, login, password, designMode);

        try {
            Repository repository;
            String webDavUrl = this.uri;
            try {
                //FIXME Doesn't work on the secure mode
                repository = RepositoryImpl.create(new DavexRepositoryConfigImpl(webDavUrl));
                //repository = JcrUtils.getRepository(confWebdavUrl.getValue());
            } catch (Exception e) {
                throw new RepositoryException(e);
            }

            setRepository(repository);
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
}
