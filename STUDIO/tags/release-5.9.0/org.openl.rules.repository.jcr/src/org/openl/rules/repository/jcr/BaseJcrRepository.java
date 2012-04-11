package org.openl.rules.repository.jcr;

import javax.jcr.Session;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.observation.EventListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.repository.RRepository;
import org.openl.rules.repository.RTransactionManager;

public abstract class BaseJcrRepository implements RRepository, EventListener{
    private static final Log LOG = LogFactory.getLog(BaseJcrRepository.class);
    private final String name;
    /** JCR Session */
    private final Session session;
    private final RTransactionManager transactionManager;

    public BaseJcrRepository(String name, Session session, RTransactionManager transactionManager) {
        this.name = name;
        this.session = session;
        this.transactionManager = transactionManager;
    }

    protected Node checkPath(String aPath) throws RepositoryException {
        Node node = session.getRootNode();
        String[] paths = aPath.split("/");
        for (String path : paths) {
            if (path.length() == 0) {
                continue; // first element (root folder) or illegal path
            }

            if (node.hasNode(path)) {
                // go deeper
                node = node.getNode(path);
            } else {
                // create new
                node = node.addNode(path);
            }
        }

        return node;
    }

    /**
     * Returns name of the repository. It can be type of repository plus
     * location.
     *
     * @return name of repository
     */
    public String getName() {
        return name;
    }

    protected Session getSession() {
        return session;
    }

    public RTransactionManager getTransactionManager() {
        return transactionManager;
    }

    /**
     * Releases resources allocated by this Rules Repository instance.
     */
    public void release() {
        try {
            session.getWorkspace().getObservationManager().removeEventListener(this);
        } catch (RepositoryException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("release", e);
            }
        }
        session.logout();
    }

}
