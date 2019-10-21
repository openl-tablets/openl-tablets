package org.openl.rules.repository.factories;

import javax.jcr.*;
import javax.jcr.nodetype.NodeTypeManager;

import org.openl.rules.repository.RRepositoryFactory;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.repository.jcr.JcrNT;
import org.openl.rules.repository.jcr.ZipJcrRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is Abstract class with common code for Local and RMI methods of accessing any JCR-170 compliant instance.
 * <p>
 * It performs basic insanity checks. For example, it verifies that OpenL node types are registered in using JCR.
 *
 * @author Aleh Bykhavets
 *
 */
public abstract class AbstractJcrRepositoryFactory extends ZipJcrRepository implements RRepositoryFactory {

    private final Logger log = LoggerFactory.getLogger(AbstractJcrRepositoryFactory.class);
    protected static final String DEFAULT_NODETYPE_FILE = "/org/openl/rules/repository/openl_nodetypes.xml";

    protected Repository repository;

    protected String uri;
    protected String login;
    protected String password;

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Checks whether the JCR instance is prepared for OpenL. If it is the first time, then there are no openL node
     * types, yet.
     *
     * @throws RepositoryException if failed
     */
    protected void checkOnStart() throws RepositoryException {
        Session systemSession = null;
        try {
            // FIXME: do not hardcode system credentials
            systemSession = createSession();
            NodeTypeManager ntm = systemSession.getWorkspace().getNodeTypeManager();

            try {
                // Does JCR know anything about OpenL?
                ntm.getNodeType(JcrNT.NT_REPOSITORY);
            } catch (Exception e) {
                // No, it does not.
                initNodeTypes(ntm);
            }
        } finally {
            if (systemSession != null) {
                systemSession.logout();
            }
        }
    }

    /**
     * Creates JCR Session.
     *
     * @return new JCR session
     * @throws RepositoryException if fails or user credentials are not correct
     */
    protected Session createSession() throws RepositoryException {
        String loginValue = login;
        String passwordValue = password;
        if (loginValue == null) {
            loginValue = "";
        }
        if (passwordValue == null) {
            passwordValue = "";
        }
        Credentials credencials = new SimpleCredentials(loginValue, passwordValue.toCharArray());
        return repository.login(credencials);
    }

    @Override
    public void initialize() throws RRepositoryException {
        Session session = null;
        try {
            session = createSession();
            init(session);
        } catch (RepositoryException e) {
            if (session != null) {
                session.logout();
            }
            throw new RRepositoryException("Failed to get Repository Instance", e);
        }

        // Register shut down hook
        ShutDownHook shutDownHook = new ShutDownHook(this);
        Runtime.getRuntime().addShutdownHook(shutDownHook);
    }

    /**
     * Registers OpenL node types in JCR.
     * <p>
     * Usually it can be done on local JCR instance.
     * <p>
     * This operation may not be supported via RMI.
     *
     * @param ntm node type manager
     * @throws RepositoryException if failed
     */
    protected abstract void initNodeTypes(NodeTypeManager ntm) throws RepositoryException;

    /**
     * @deprecated Correct implementation shouldn't rely on finalize() method, it's a bad practice. Close repository
     *             yourself when it's not needed, don't rely on JVM.
     */
    @Deprecated
    @Override
    protected void finalize() throws Throwable {
        try {
            close();
        } catch (Exception e) {
            try {
                log.error("finalize", e);
            } catch (Throwable ignored) {
            }
        } catch (Throwable ignored) {
        } finally {
            super.finalize();
        }
    }

    /**
     * Sets repository reference. Must be called before invoking {@link #initialize()} method.
     *
     * @param rep implementation specific repository
     * @throws RepositoryException if fails to check first start
     */
    protected void setRepository(Repository rep) throws RepositoryException {
        repository = rep;

        checkOnStart();
    }

}
