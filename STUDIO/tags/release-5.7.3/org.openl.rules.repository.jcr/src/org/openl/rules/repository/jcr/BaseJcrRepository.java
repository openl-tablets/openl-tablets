package org.openl.rules.repository.jcr;

import javax.jcr.Session;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

public class BaseJcrRepository {
    protected final String name;
    /** JCR Session */
    protected final Session session;

    public BaseJcrRepository(String name, Session session) {
        this.name = name;
        this.session = session;
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

    /**
     * Releases resources allocated by this Rules Repository instance.
     */
    public void release() {
        session.logout();
    }

}
