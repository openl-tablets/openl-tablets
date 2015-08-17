package org.openl.rules.repository.factories;

import org.apache.commons.lang3.StringUtils;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.loaders.jdbc.configuration.JdbcStringBasedCacheStoreConfigurationBuilder;
import org.infinispan.schematic.document.ParsingException;
import org.infinispan.transaction.TransactionMode;
import org.modeshape.common.collection.Problems;
import org.modeshape.jcr.JcrNodeTypeManager;
import org.modeshape.jcr.LocalEnvironment;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.RepositoryConfiguration;
import org.openl.config.ConfigPropertyString;
import org.openl.config.ConfigSet;
import org.openl.rules.repository.RTransactionManager;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeManager;
import javax.transaction.UserTransaction;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Local Jackrabbit Repository Factory. It handles own instance of Jackrabbit
 * repository.
 *
 * @author Yury Molchan
 */
public class DBRepositoryFactory extends AbstractJcrRepositoryFactory {
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

        String dbUrl = url.getValue();
        String user = login.getValue();
        String pwd = password.getValue();

        RepositoryConfiguration config = getModeshapeConfiguration(dbUrl, user, pwd);

        // Register shut down hook
        ShutDownHook shutDownHook = new ShutDownHook(this);
        Runtime.getRuntime().addShutdownHook(shutDownHook);

        // Create and start the engine ...
        engine = new ModeShapeEngine();
        engine.start();
        // Deploy the repository ...
        Repository repository = engine.deploy(config);

        setRepository(repository, config.getName());
    }

    private RepositoryConfiguration getModeshapeConfiguration(String url, String user, String password) throws
                                                                                                        SQLException,
                                                                                                        ParsingException,
                                                                                                        FileNotFoundException {
        final String repoName = ("OPENL_" + url).replace('"', '_').replace('\'', '_').replace(':', '_');
        // Create a local environment that we'll set up to own the external
        // components ModeShape needs ...
        LocalEnvironment environment = new LocalEnvironment() {
            @Override
            protected GlobalConfigurationBuilder createGlobalConfigurationBuilder() {
                GlobalConfigurationBuilder global = new GlobalConfigurationBuilder();
                global.globalJmxStatistics().enable().allowDuplicateDomains(true);
                global.transport().defaultTransport().clusterName(repoName);
                return global;
            }
        };

        // Infinispan cache declaration
        Configuration ispnConfig = getInfinispanConfiguration(url, user, password);
        environment.defineCache("OPENL_repository", ispnConfig);
        environment.defineCache("OPENL_BinaryData", ispnConfig);
        environment.defineCache("OPENL_MetaData", ispnConfig);

        // Modeshape's configuration
        RepositoryConfiguration config = RepositoryConfiguration.read("{'name':'" + repoName + "','storage':{'cacheName':'OPENL_repository','binaryStorage':{'type':'cache','dataCacheName':'OPENL_BinaryData','metadataCacheName':'OPENL_MetaData'}},'clustering':{'clusterName':'" + repoName + "'}}");
        config = config.with(environment);

        // Verify the configuration for the repository ...
        Problems problems = config.validate();
        if (problems.hasErrors()) {
            String message = "Problems in the Modeshape configuration";
            log.error(message);
            log.error(problems.toString());
            throw new IllegalArgumentException(message);
        }
        return config;
    }

    private Configuration getInfinispanConfiguration(String url, String user, String password) throws SQLException {
        String driverClass = DriverManager.getDriver(url).getClass().getName();
        String[] types = determineDBDataTypes(url, user, password);

        // Infinispan's configuration
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.transaction().transactionMode(TransactionMode.TRANSACTIONAL);
        configurationBuilder.jmxStatistics()
                .enable()
                .clustering()
                .cacheMode(CacheMode.REPL_SYNC)
                .loaders()
                .shared(true)
                .addLoader(JdbcStringBasedCacheStoreConfigurationBuilder.class)
                .table()
                .tableNamePrefix("CACHE")
                .idColumnName("ID")
                .idColumnType(types[0])
                .dataColumnName("DATA")
                .dataColumnType(types[1])
                .timestampColumnName("TIMESTAMP")
                .timestampColumnType(types[2])
                .connectionPool()
                .connectionUrl(url)
                .username(user)
                .password(password)
                        // Get a driver by url
                .driverClass(driverClass);
        return configurationBuilder.build();
    }

    private String[] determineDBDataTypes(String url, String user, String password) throws SQLException {
        log.info("Determine SQL types by url '{}')", url);
        Connection conn = DriverManager.getConnection(url, user, password);
        DatabaseMetaData metaData = conn.getMetaData();
        ResultSet rs = metaData.getTypeInfo();

        String[] types = new String[3];
        while (rs.next()) {
            // Get the database-specific type name
            String typeName = rs.getString("TYPE_NAME");
            // Get the java.sql.Types type to which this
            // database-specific type is mapped
            int dataType = rs.getInt("DATA_TYPE");
            switch (dataType) {
                case Types.VARCHAR:
                    if (types[0] != null) {
                        break;
                    }
                    int prec = rs.getInt("PRECISION");
                    if (prec > 1000) {
                        prec = 1000;
                    }
                    types[0] = typeName + '(' + prec + ')';
                    break;
                case Types.LONGVARBINARY:
                    if (types[1] == null) {
                        types[1] = typeName;
                    }
                    break;
                case Types.BIGINT:
                    if (types[2] == null) {
                        types[2] = typeName;
                    }
                    break;
            }
        }
        conn.close();

        log.info("Determined SQL types ('{}', '{}', '{}')", types[0], types[1], types[2]);
        // Set defaults
        if (types[0] == null) {
            types[0] = "VARCHAR(1000)";
        }
        if (types[1] == null) {
            types[1] = "BLOB";
        }
        if (types[2] == null) {
            types[2] = "BIGINT";
        }
        log.info("Used SQL types ('{}', '{}', '{}')", types[0], types[1], types[2]);

        return types;
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

    @Override
    public RTransactionManager getTrasactionManager(Session session) {
        return new RTransactionManager() {
            @Override
            public UserTransaction getTransaction() {
                return NO_TRANSACTION;
            }
        };
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
