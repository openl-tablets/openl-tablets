package org.openl.rules.repository.factories;

import java.io.IOException;
import java.io.InputStream;
import java.sql.DriverManager;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.commons.lang3.StringUtils;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.persistence.jdbc.configuration.JdbcStringBasedStoreConfigurationBuilder;
import org.infinispan.transaction.TransactionMode;
import org.modeshape.common.collection.Problems;
import org.modeshape.jcr.JcrNodeTypeManager;
import org.modeshape.jcr.LocalEnvironment;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.RepositoryConfiguration;
import org.openl.config.ConfigPropertyString;
import org.openl.config.ConfigSet;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Local Jackrabbit Repository Factory. It handles own instance of Jackrabbit
 * repository.
 *
 * @author Aleh Bykhavets
 */
public class DBRepositoryFactory extends AbstractJackrabbitRepositoryFactory {
    private final Logger log = LoggerFactory.getLogger(DBRepositoryFactory.class);

    private ConfigPropertyString confNodeTypeFile = new ConfigPropertyString("repository.jcr.nodetypes",
        DEFAULT_NODETYPE_FILE);

    private ConfigPropertyString url = new ConfigPropertyString("repository.db.url", "jdbc:mysql://localhost/repo");

    /**
     * Jackrabbit local repository
     */
    private ModeShapeEngine engine;
    private String nodeTypeFile;

    @Override
    protected void finalize() throws Throwable {
        try {
            release();
        } catch (RRepositoryException e) {
            try {
                log.error("finalize", e);
            } catch (Throwable ignored) {
            }
        } catch (Throwable ignored) {
        } finally {
            super.finalize();
        }
    }

    // ------ private methods ------

    /**
     * Starts modeshape JCR repository over DataBase. If there was no repository
     * it will be created automatically.
     */
    private void init() throws Exception {
        registerDrivers();
        // Create and start the engine ...
        engine = new ModeShapeEngine();
        engine.start();

        // Register shut down hook
        ShutDownHook shutDownHook = new ShutDownHook(this);
        Runtime.getRuntime().addShutdownHook(shutDownHook);

        // Infinispan's configuration
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.transaction().transactionMode(TransactionMode.TRANSACTIONAL);
        configurationBuilder.jmxStatistics()
            .enable()
            .clustering()
            .cacheMode(CacheMode.REPL_SYNC)
            .persistence()
            .addStore(JdbcStringBasedStoreConfigurationBuilder.class)
            .shared(true)
            .table()
            .dropOnExit(false)
            .createOnStart(true)
            .tableNamePrefix("OPENL")
            .idColumnName("ID")
            .idColumnType("VARCHAR(255)")
            .dataColumnName("DATA")
            .dataColumnType("BLOB")
            .timestampColumnName("TIMESTAMP")
            .timestampColumnType("BIGINT")
            .connectionPool()
            .connectionUrl(url.getValue())
            .username(login.getValue())
            .password(password.getValue())
            // Get a driver by url
            .driverClass(DriverManager.getDriver(url.getValue()).getClass().getName());
        Configuration ispnConfig = configurationBuilder.build();

        // Create a local environment that we'll set up to own the external
        // components ModeShape needs ...
        LocalEnvironment environment = new LocalEnvironment() {
            @Override
            protected GlobalConfigurationBuilder createGlobalConfigurationBuilder() {
                GlobalConfigurationBuilder global = new GlobalConfigurationBuilder();
                global.globalJmxStatistics().enable().allowDuplicateDomains(true);
                global.transport().defaultTransport().clusterName("OPENL_CLUSTER_" + url.getValue());
                return global;
            }
        };
        environment.defineCache("persisted_repository", ispnConfig);

        // Modeshape's configuration
        RepositoryConfiguration config = RepositoryConfiguration.read("my-repository-config.json");
        config = config.with(environment);

        // Verify the configuration for the repository ...
        Problems problems = config.validate();
        if (problems.hasErrors()) {
            String message = "Problems starting the engine.";
            log.error(message);
            log.error(problems.toString());
            throw new IllegalArgumentException(message);
        }

        // Deploy the repository ...
        Repository repository = engine.deploy(config);

        setRepository(repository, config.getName() + "_" + url.getValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(ConfigSet confSet) throws RRepositoryException {
        setRepoConfigFile(new ConfigPropertyString("db.repository-config", null));
        super.initialize(confSet);

        confSet.updateProperty(confNodeTypeFile);
        confSet.updateProperty(url);

        nodeTypeFile = confNodeTypeFile.getValue();

        try {
            init();
        } catch (Exception e) {
            throw new RRepositoryException("Failed to initialize DataBase: " + e.getMessage(), e);
        }
    }

    @Override
    protected Session createSession() throws RepositoryException {
        return repository.login("default");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initNodeTypes(NodeTypeManager ntm) throws RepositoryException {
        JcrNodeTypeManager ntmi = (JcrNodeTypeManager) ntm;

        try {
            InputStream is = null;
            try {
                is = this.getClass().getResourceAsStream(nodeTypeFile);
                ntmi.registerNodeTypes(is, true);
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        } catch (IOException e) {
            throw new RepositoryException("Failed to init NodeTypes: " + e.getMessage(), e);
        }
    }

    public void setConfNodeTypeFile(ConfigPropertyString confNodeTypeFile) {
        this.confNodeTypeFile = confNodeTypeFile;
    }

    public void setUrl(ConfigPropertyString url) {
        this.url = url;
    }

    @Override
    public void release() throws RRepositoryException {
        super.release();
        try {
            engine.shutdown().get();
        } catch (Exception e) {
            throw new RRepositoryException("Shutdown has failed.", e);
        }
    }

    private void registerDrivers() {
        // Defaults drivers
        String[] drivers = { "com.mysql.jdbc.Driver",
                "com.ibm.db2.jcc.DB2Driver",
                "oracle.jdbc.OracleDriver",
                "org.postgresql.Driver",
                "org.hsqldb.jdbcDriver",
                "org.h2.Driver",
                "com.microsoft.sqlserver.jdbc.SQLServerDriver" };
        registerDrivers(drivers);
        drivers = StringUtils.split(System.getProperty("jdbc.drivers"), ':');
        registerDrivers(drivers);
    }

    private void registerDrivers(String... drivers) {
        if (drivers == null) {
            return;
        }
        for (String driver : drivers) {
            try {
                Class.forName(driver);
                log.info("JDBC Driver: '{}' - OK.", driver);
            } catch (ClassNotFoundException e) {
                log.info("JDBC Driver: '{}' - NOT FOUND.", driver);
            }
        }
    }
}
