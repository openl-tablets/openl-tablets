package org.openl.rules.repository.jcr;

import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.openl.rules.repository.RProject;
import org.openl.rules.repository.RRepository;
import org.openl.rules.repository.exceptions.RRepositoryException;

/**
 * Implementation for JCR Repository.
 * One JCR Repository instance per user.
 * 
 * @author Aleh Bykhavets
 *
 */
public class JcrRepository implements RRepository {
    private static final String QUERY_PROJECTS = "//element(*, " + JcrNT.NT_PROJECT + ")";
    private static final String QUERY_PROJECTS_4_DEL = "//element(*, " + JcrNT.NT_PROJECT + ") [@" + JcrNT.PROP_PRJ_MARKED_4_DELETION + "]";

    private String name;
    /** JCR Session */
    private Session session;
    private Node defNewProjectLocation;

    public JcrRepository(String name, Session session, String defPath) throws RepositoryException {
        this.name = name;
        this.session = session;

        Node node = session.getRootNode();
        String[] paths = defPath.split("/");
        for (String path : paths) {
            if (path.length() == 0) continue; // first element (root folder) or illegal path

            if (node.hasNode(path)) {
                // go deeper
                node = node.getNode(path);
            } else {
                // create new
                node = node.addNode(path);
            }
        }

        if (node.isNew()) {
            // save all at once
            session.save();
        }
        defNewProjectLocation = node;
    }

    /** {@inheritDoc} */
    public List<RProject> getProjects() throws RRepositoryException {
        // TODO list all or only that are active (not marked4deletion)?
        return runQuery(QUERY_PROJECTS);
    }

    /** {@inheritDoc} */
    public List<RProject> getProjects4Deletion() throws RRepositoryException {
        return runQuery(QUERY_PROJECTS_4_DEL);
    }

    /** {@inheritDoc} */
    public void release() {
        session.logout();
    }

    /** {@inheritDoc} */
    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    public RProject createProject(String nodeName) throws RRepositoryException {
        try {
            return JcrProject.createProject(defNewProjectLocation, nodeName);
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to Create Project", e);
        }
    }

    // ------ protected methods ------

    /**
     * Gets internal JCR Session.
     *
     * @return JCR Session
     */
    protected Session getSession() {
        return session;
    }

    /**
     * Runs query in JCR.
     *
     * @param statement query statement
     * @return list of OpenL projects
     * @throws RRepositoryException if failed
     */
    protected List<RProject> runQuery(String statement) throws RRepositoryException {
        try {
            QueryManager qm = session.getWorkspace().getQueryManager();
            Query query = qm.createQuery(statement, Query.XPATH);

            QueryResult qr = query.execute();

            LinkedList<RProject> result = new LinkedList<RProject>();
            for (NodeIterator ni = qr.getNodes(); ni.hasNext(); ) {
                Node n = ni.nextNode();

                JcrProject p = new JcrProject(n);
                result.add(p);
            }

            return result;
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to run query", e);
        }
    }
}
