package org.openl.rules.repository.jcr.impl;

import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.openl.rules.repository.jcr.JcrNT;
import org.openl.rules.repository.jcr.JcrProject;
import org.openl.rules.repository.jcr.JcrRepository;

/**
 * Implementation for JCR Repository.
 * One JCR Repository instance per user.
 * 
 * @author Aleh Bykhavets
 *
 */
public class JcrRepositoryImpl implements JcrRepository {
	private static final String QUERY_PROJECTS = "//element(*, " + JcrNT.NT_PROJECT + ")";
	private static final String QUERY_PROJECTS_4_DEL = "//element(*, " + JcrNT.NT_PROJECT + ") [@" + JcrNT.PROP_PRJ_MARKED_4_DELETION + "]";

	/** JCR Session */
	private Session session;
	
	public JcrRepositoryImpl(Session session) {
		this.session = session;
	}
	
    /** {@inheritDoc} */
	public List<JcrProject> listProjects() throws RepositoryException {
		// TODO list all or only that are active (not marked4deletion)?
		return runQuery(QUERY_PROJECTS);
	}

    /** {@inheritDoc} */
	public List<JcrProject> listProjects4Deletion() throws RepositoryException {
		return runQuery(QUERY_PROJECTS_4_DEL);
	}

    /** {@inheritDoc} */
	public JcrProject createProject(Node parentNode, String nodeName) throws RepositoryException {
		return JcrProjectImpl.createProject(parentNode, nodeName);
	}
	
    /** {@inheritDoc} */
	public void release() {
		session.logout();
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
	 * @throws RepositoryException
	 */
	protected List<JcrProject> runQuery(String statement) throws RepositoryException {
		QueryManager qm = session.getWorkspace().getQueryManager();
		Query query = qm.createQuery(statement, Query.XPATH);
		
		QueryResult qr = query.execute();
		
		LinkedList<JcrProject> result = new LinkedList<JcrProject>();
		for (NodeIterator ni = qr.getNodes(); ni.hasNext(); ) {
			Node n = ni.nextNode();
			
			JcrProjectImpl p = new JcrProjectImpl(n);
			result.add(p);
		}
		
		return result;
	}
}
