package org.openl.rules.repository.factories;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeTypeManager;

import org.openl.rules.repository.RRepository;
import org.openl.rules.repository.RRepositoryFactory;
import org.openl.rules.repository.SmartProps;
import org.openl.rules.repository.jcr.JcrRepository;
import org.openl.rules.repository.jcr.JcrNT;
import org.openl.rules.repository.exceptions.RRepositoryException;

/**
 * This is Abstract class with common code for Local and RMI methods of
 * accessing any JCR-170 compliant instance.
 * <p>
 * It performs basic insanity checks.
 * For example, it verifies that OpenL node types are registered in using JCR.
 * 
 * @author Aleh Bykhavets
 *
 */
public abstract class AbstractJcrRepositoryFactory implements RRepositoryFactory {
    public static final String PROP_DEF_PATH = "JCR.default.path";
    public static final String DEF_PATH = "/";

    private Repository repository;
    /** Default path where new project should be created */
    private String defPath;

    /** {@inheritDoc} */
    public RRepository getRepositoryInstance() throws RRepositoryException {
        try {
            // FIXME: do not hardcode credential info
            Session session = createSession("user", "pass");

            JcrRepository jri = new JcrRepository(session, defPath);
            return jri;
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to get Repository Instance", e);
        }
    }

    /** {@inheritDoc} */
    public void initialize(SmartProps props) throws RRepositoryException {
        defPath = props.getStr(PROP_DEF_PATH, DEF_PATH);
        //TODO: add default path support
        // 1. check path -- create if absent
        // 2. pass as parameter or property to JcrRepositoryImpl
    }

    // ------ protected methods ------

    /**
     * Sets repository reference.
     * Must be called before invoking {@link #getRepositoryInstance()} method.
     *
     * @param rep implementation specific repository
     * @throws RepositoryException if fails to check first start
     */
    protected void setRepository(Repository rep) throws RepositoryException {
        repository = rep;

        checkOnStart();
    }

    /**
     * Creates JCR Session.
     *
     * @param user user id
     * @param pass password of user
     * @return new JCR session
     * @throws RepositoryException if fails or user credentials are not correct
     */
    protected Session createSession(String user, String pass) throws RepositoryException {
        char[] password = pass.toCharArray();
        SimpleCredentials sc = new SimpleCredentials(user, password);
        Session session = repository.login(sc);
        return session;
    }

    /**
     * Checks whether the JCR instance is prepeared for OpenL.
     * If it is the first time, then there are no openL node types, yet.
     *
     * @throws RepositoryException if failed
     */
    protected void checkOnStart() throws RepositoryException {
        Session systemSession = null;
        try {
            //FIXME: do not hardcode system credentials
            systemSession = createSession("sys", "secret");
            NodeTypeManager ntm = systemSession.getWorkspace().getNodeTypeManager();

            boolean initNodeTypes = false;
            try {
                // Does JCR know anything about OpenL?
                ntm.getNodeType(JcrNT.NT_PROJECT);
            } catch (NoSuchNodeTypeException e) {
                // No, it doesn't.
                initNodeTypes = true;
            }

            if (initNodeTypes) {
                // Add OpenL node defenitions
                initNodeTypes(ntm);
            }
        } finally {
            if (systemSession != null) {
                systemSession.logout();
            }
        }
    }

    /**
     * Registries OpenL node types in JCR.
     * <p>
     * Usually it can be done on local JCR instance.
     * <p>
     * This operation may not be supported via RMI.
     *
     * @param ntm node type manager
     * @throws RepositoryException if failed
     */
    protected abstract void initNodeTypes(NodeTypeManager ntm) throws RepositoryException;
}
