package org.openl.rules.repository.factories;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.openl.config.ConfigPropertyString;
import org.openl.config.ConfigSet;
import org.openl.rules.repository.RRepository;
import org.openl.rules.repository.RRepositoryFactory;
import org.openl.rules.repository.RTransactionManager;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.repository.jcr.JcrNT;
import org.openl.rules.repository.jcr.JcrProductionRepository;
import org.openl.rules.repository.jcr.JcrRepository;
import org.xml.sax.InputSource;

/**
 * This is Abstract class with common code for Local and RMI methods of
 * accessing any JCR-170 compliant instance.
 * <p>
 * It performs basic insanity checks. For example, it verifies that OpenL node
 * types are registered in using JCR.
 *
 * @author Aleh Bykhavets
 *
 */
public abstract class AbstractJcrRepositoryFactory implements RRepositoryFactory {

    protected static final String DEFAULT_NODETYPE_FILE = "/org/openl/rules/repository/openl_nodetypes.xml";

    /** Default path where new project should be created */
    protected final ConfigPropertyString confRulesProjectsLocation = new ConfigPropertyString(
            "design-repository.rules.path", "/rules");
    protected final ConfigPropertyString confDeploymentProjectsLocation = new ConfigPropertyString(
            "design-repository.deployments.path", "/deployments");

    protected Repository repository;
    protected String repositoryName;
    private RRepository rulesRepository;

    protected ConfigPropertyString login;
    protected ConfigPropertyString password;
    protected ConfigPropertyString uri;
    private boolean productionRepositoryMode = false;


    /**
     * Checks whether the JCR instance is prepared for OpenL. If it is the first
     * time, then there are no openL node types, yet.
     *
     * @throws RepositoryException if failed
     */
    protected void checkOnStart() throws RepositoryException {
        Session systemSession = null;
        try {
            // FIXME: do not hardcode system credentials
            systemSession = createSession();
            NodeTypeManager ntm = systemSession.getWorkspace().getNodeTypeManager();

            boolean initNodeTypes = false;
            try {
                // Does JCR know anything about OpenL?
                ntm.getNodeType(JcrNT.NT_REPOSITORY);
            } catch (Exception e) {
                // No, it doesn't.
                initNodeTypes = true;
            }

            if (initNodeTypes) {
                // Add OpenL node definitions
                initNodeTypes(ntm);
            } else {
                checkSchemaVersion(ntm);
            }
        } finally {
            if (systemSession != null) {
                systemSession.logout();
            }
        }
    }

    /**
     * Checks whether schema version of the repository is valid. If check failed
     * then it throws exception.
     *
     * @param ntm Node Type Manager
     * @throws RepositoryException if check failed
     */
    protected void checkSchemaVersion(NodeTypeManager ntm) throws RepositoryException {
        String schemaVersion = getCurrentSchemaVersion(ntm);
        // compare expected and repository schema versions
        String expectedVersion = getExpectedSchemaVersion();
        if (!expectedVersion.equals(schemaVersion)) {
            throw new RepositoryException("Schema version is different. Has (" + schemaVersion + ") when ("
                    + expectedVersion + ") expected.");
        }
    }
    
    /**
     * Creates JCR Session.
     *
     * @return new JCR session
     * @throws RepositoryException if fails or user credentials are not correct
     */
    protected Session createSession() throws RepositoryException {
        String loginValue = login.getValue();
        String passwordValue = password.getValue();
        if (loginValue == null) {
            loginValue = "";
        }
        if (passwordValue == null) {
            passwordValue = "";
        }
        Credentials credencials = new SimpleCredentials(loginValue, passwordValue.toCharArray());
        return repository.login(credencials);
    }

    // ------ protected methods ------

    protected String getExpectedSchemaVersion() throws RepositoryException {
        String xPathQ = "/nodeTypes/nodeType[@name = 'openl:repository']"
                + "/propertyDefinition[@name = 'schema-version']/defaultValues/defaultValue[1]";

        XPathFactory factory = XPathFactory.newInstance();
        XPath xPath = factory.newXPath();

        try {

            InputSource source = new InputSource(this.getClass().getResourceAsStream(DEFAULT_NODETYPE_FILE));
            String result = xPath.evaluate(xPathQ, source);

            if (result == null || result.length() == 0) {
                throw new Exception("Cannot find node.");
            }

            return result;
        } catch (Exception e) {
            throw new RepositoryException("Cannot read schema version from '" + DEFAULT_NODETYPE_FILE + "': " + e.getMessage());
        }
    }

    protected String getCurrentSchemaVersion(NodeTypeManager ntm) throws RepositoryException {
        String schemaVersion = null;

        // check special node
        NodeType nodeType;
        try {
            nodeType = ntm.getNodeType(JcrNT.NT_REPOSITORY);
        } catch (NoSuchNodeTypeException e) {
            throw new RepositoryException("Cannot determine scheme version: " + e.getMessage());
        }

        PropertyDefinition[] propDefs = nodeType.getPropertyDefinitions();

        // retrieve value of schema version
        for (PropertyDefinition definition : propDefs) {
            if ("schema-version".equals(definition.getName())) {
                Value[] defValues = definition.getDefaultValues();

                if (defValues != null && defValues.length > 0) {
                    // take first only
                    // Note: multiply values are not supported
                    schemaVersion = defValues[0].getString();
                }

                break;
            }
        }

        if (schemaVersion == null) {
            throw new RepositoryException("Cannot determine scheme version: no special property or value!");
        }
        return schemaVersion;

    }

    /** {@inheritDoc} */
    public RRepository getRepositoryInstance() throws RRepositoryException {
        if(rulesRepository == null){
            rulesRepository = createRepository();
        }
        return rulesRepository;
    }

    protected void setProductionRepositoryMode(boolean productionRepositoryMode) {
        String type = productionRepositoryMode ? "production" : "design";
        login = new ConfigPropertyString(type + "-repository.login", null);
        password = new ConfigPropertyString(type + "-repository.password", null);
        this.productionRepositoryMode = productionRepositoryMode;
    }

    public RRepository createRepository() throws RRepositoryException {
        Session session = null;
        try {
            session = createSession();

            RTransactionManager transactionManager = getTrasactionManager(session);
            RRepository theRepository;
            if (productionRepositoryMode) {
                theRepository = new JcrProductionRepository(repositoryName, session, transactionManager);
            } else {
                theRepository = new JcrRepository(repositoryName, session, transactionManager,
                        confRulesProjectsLocation.getValue(), confDeploymentProjectsLocation.getValue());
            }
            return theRepository;
        } catch (RepositoryException e) {
            if (session != null){
                session.logout();
            }
            throw new RRepositoryException("Failed to get Repository Instance", e);
        } 
    }

    /** {@inheritDoc} */
    public void initialize(ConfigSet confSet) throws RRepositoryException {
        confSet.updateProperty(confRulesProjectsLocation); 
        confSet.updateProperty(confDeploymentProjectsLocation);

        confSet.updateProperty(login);
        confSet.updatePasswordProperty(password);
        confSet.updateProperty(uri);

        // TODO: add default path support
        // 1. check path -- create if absent
        // 2. pass as parameter or property to JcrRepository
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

    public void release() throws RRepositoryException {
        // If rulesRepository is not created, we don't need to create it and then release it
        if (rulesRepository != null) {
            rulesRepository.release();
            rulesRepository = null;
        }
    }

    /**
     * Sets repository reference. Must be called before invoking
     * {@link #getRepositoryInstance()} method.
     *
     * @param rep implementation specific repository
     * @throws RepositoryException if fails to check first start
     */
    protected void setRepository(Repository rep, String name) throws RepositoryException {
        repository = rep;
        repositoryName = name;

        checkOnStart();
    }

    public abstract RTransactionManager getTrasactionManager(Session session);

    public void setUri(ConfigPropertyString uri) {
        this.uri = uri;
    }
}
