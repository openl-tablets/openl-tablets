package org.openl.rules.repository.factories;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeTypeManager;

import org.openl.rules.repository.SmartProps;
import org.openl.rules.repository.exceptions.RRepositoryException;

import com.exigen.cm.RepositoryProvider;

/**
 * Exigen JCR Repository Factory.
 * 
 * @author Aleh Bykhavets
 *
 */
public class ExigenJcrRepository extends AbstractJcrRepositoryFactory {

    /** {@inheritDoc} */
    public void initialize(SmartProps props) throws RRepositoryException {
        super.initialize(props);

        try {
            Repository repository = RepositoryProvider.getInstance().getRepository();
            // TODO: do not hardcode repository name
            setRepository(repository, "Exigen JCR");
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to initialize JCR: " + e.getMessage(), e);
        }
    }

    /**
     * Release all allocated resources.
     * <p>
     * Note: There is no 100% that {@link #finalize()} will be invoked by JVM
     */
    @Override
    protected void finalize() throws Throwable {
        // TODO: close open sessions
        super.finalize();
    }

    /** {@inheritDoc} */
    protected void initNodeTypes(NodeTypeManager ntm) throws RepositoryException {
        throw new RepositoryException("Nodetypes can be initialized by this program."
                + "\nPlease, add OpenL node types definition manually or via command line tool.");
    }
}
