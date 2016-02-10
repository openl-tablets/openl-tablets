package org.openl.rules.repository.factories;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.UUID;
import java.util.concurrent.Future;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeManager;
import javax.naming.NamingException;
import javax.transaction.UserTransaction;

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
import org.openl.config.ConfigSet;
import org.openl.rules.repository.RTransactionManager;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DataBase-based Repository Factory. Creates an JCR repository in the DB.
 *
 * @author Yury Molchan
 */
abstract class DBRepositoryFactory extends AbstractJcrRepositoryFactory {
    private final Logger log = LoggerFactory.getLogger(DBRepositoryFactory.class);

    private static final String TABLE_PREFIX = "OPENL";
    private static final String TABLE_NAME = "JCR_CACHE";
    private static final String REPO_TABLE = TABLE_PREFIX + '_' + TABLE_NAME;
    private static final String COL_ID = "ID";
    private static final String COL_DATA = "DATA";
    private static final String COL_TIME = "TIMESTAMP";
    private static final String OPENL_JCR_REPO_ID_KEY = "openl-jcr-repo-id";
    private static final String CREATE_TABLE = "CREATE TABLE " + REPO_TABLE + " (" + COL_ID + " %s NOT NULL, " + COL_DATA + " %s, " + COL_TIME + " %s, PRIMARY KEY (" + COL_ID + "))";
    private static final String INSERT_ID = "INSERT INTO " + REPO_TABLE + " (" + COL_ID + ", " + COL_DATA + ", " + COL_TIME + ") VALUES(?,?,-1)";
    private static final String SELECT_ID = "SELECT " + COL_DATA + " FROM " + REPO_TABLE + " WHERE " + COL_ID + " = ?";

    /**
     * Jackrabbit local repository
     */
    private ModeShapeEngine engine;

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

        String dbUrl = uri.getValue();
        String user = login.getValue();
        String pwd = password.getValue();

        log.info("Checking a connection to DB [{}]", dbUrl);
        Connection conn;
        conn = createConnection(dbUrl, user, pwd);

        log.info("Preparing a repository...");
        initTable(conn);
        String repoID = getRepoID(conn);
        conn.close();
        log.info("The repository for ID=[{}] has been prepared", repoID);

        RepositoryConfiguration config = getModeshapeConfiguration(dbUrl, user, pwd, repoID);

        // Register shut down hook
        ShutDownHook shutDownHook = new ShutDownHook(this);
        Runtime.getRuntime().addShutdownHook(shutDownHook);

        log.info("Starting ModeShape engine...");
        // Create and start the engine ...
        engine = new ModeShapeEngine();
        engine.start();
        // Deploy the repository ...
        engine.deploy(config);

        String repoName = config.getName();
        log.info("Starting ModeShape repository [{}]...", repoName);
        Future<? extends Repository> future = engine.startRepository(repoName);
        Repository repository = future.get();
        log.info("ModeShape repository ID=[{}] has been started", repoName);

        setRepository(repository);
        log.info("Checking the repository...");
        getRepositoryInstance();
        log.info("The repository has loaded");
    }

    abstract Connection createConnection(String dbUrl, String user, String pwd);

    private RepositoryConfiguration getModeshapeConfiguration(String url,
            String user,
            String password,
            final String repoName) throws SQLException, ParsingException, FileNotFoundException, NamingException {
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
        environment.defineCache(TABLE_NAME, ispnConfig);

        // Modeshape's configuration
        RepositoryConfiguration config = RepositoryConfiguration.read(
            "{'name':'" + repoName + "', 'jndiName':'', 'storage':{'cacheName':'" + TABLE_NAME + "','binaryStorage':{'type':'cache','dataCacheName':'" + TABLE_NAME + "','metadataCacheName':'" + TABLE_NAME + "'}},'clustering':{'clusterName':'" + repoName + "'}}");
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

    private Configuration getInfinispanConfiguration(String url, String user, String password) throws SQLException,
                                                                                               NamingException {

        // Infinispan's configuration
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.transaction().transactionMode(TransactionMode.TRANSACTIONAL);
        JdbcStringBasedCacheStoreConfigurationBuilder jdbcBuilder = configurationBuilder.jmxStatistics()
            .enable()
            .clustering()
            .cacheMode(CacheMode.REPL_SYNC)
            .loaders()
            .shared(true)
            .addLoader(JdbcStringBasedCacheStoreConfigurationBuilder.class);

        buildDBConnection(jdbcBuilder, url, user, password);

        jdbcBuilder.table()
            .createOnStart(false)
            .tableNamePrefix(TABLE_PREFIX)
            .idColumnName(COL_ID)
            .dataColumnName(COL_DATA)
            .timestampColumnName(COL_TIME);
        return configurationBuilder.build();
    }

    abstract void buildDBConnection(JdbcStringBasedCacheStoreConfigurationBuilder jdbcBuilder,
            String url,
            String user,
            String password);

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(ConfigSet confSet) throws RRepositoryException {
        super.initialize(confSet);

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
                is = this.getClass().getResourceAsStream(DEFAULT_NODETYPE_FILE);
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

    @Override
    public void release() throws RRepositoryException {
        try {
            super.release();
        } finally {
            try {
                if (engine != null) {
                    engine.shutdown().get();
                    engine = null;
                }
            } catch (Exception e) {
                throw new RRepositoryException("Shutdown has failed.", e);
            }
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

    private void initTable(Connection conn) throws SQLException {
        if (tableExists(conn)) {
            log.info("Table '{}' already exists", REPO_TABLE);
            return;
        }
        createTable(conn);
        if (tableExists(conn)) {
            log.info("Table '{}' has been created", REPO_TABLE);
            return;
        }
        throw new IllegalStateException("Table '" + REPO_TABLE + "' has not created");
    }

    private boolean tableExists(Connection connection) {
        ResultSet rs = null; 
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            if ("Oracle".equals(metaData.getDatabaseProductName())){
                rs = metaData.getTables(null, metaData.getUserName(), REPO_TABLE, new String[] { "TABLE" });
            }else{
                rs = metaData.getTables(null, null, REPO_TABLE, new String[] { "TABLE" });
            }
            return rs.next();
        } catch (SQLException e) {
            log.debug("SQLException occurs while checking the table {}", REPO_TABLE, e);
            return false;
        } finally {
            safeClose(rs);
        }
    }

    private void createTable(Connection conn) throws SQLException {
        String[] strings = determineDBDataTypes(conn);
        String idType = strings[0];
        String dataType = strings[1];
        String timestampType = strings[2];
        String sql = String.format(CREATE_TABLE, idType, dataType, timestampType);
        log.info("The following SQL script being used [ {} ]", sql);
        Statement statement = null;
        try {
            statement = conn.createStatement();
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            log.warn("SQLException occurs while checking the table {}", REPO_TABLE, e);
        } finally {
            safeClose(statement);
        }
    }

    private String[] determineDBDataTypes(Connection conn) throws SQLException {
        log.info("Determine SQL types");
        DatabaseMetaData metaData = conn.getMetaData();
        ResultSet rs = null;

        String nvarcharType = null;
        String varcharType = null;
        String binaryType = null;
        String bigintType = null;
        try {
            rs = metaData.getTypeInfo();
            while (rs.next()) {
                // Get the database-specific type name
                String typeName = rs.getString("TYPE_NAME");
                // Get the java.sql.Types type to which this
                // database-specific type is mapped
                int dataType = rs.getInt("DATA_TYPE");
                switch (dataType) {
                    case Types.NVARCHAR:
                        if (nvarcharType != null) {
                            break;
                        }
                        nvarcharType = typeName + '(' + getPrecision(rs) + ')';
                        break;
                    case Types.VARCHAR:
                        if (varcharType != null) {
                            break;
                        }
                        varcharType = typeName + '(' + getPrecision(rs) + ')';
                        break;
                    case Types.LONGVARBINARY:
                        if (binaryType == null) {
                            binaryType = typeName;
                        }
                        break;
                    case Types.BIGINT:
                        if (bigintType == null) {
                            bigintType = typeName;
                        }
                        break;
                }
            }
        } finally {
            safeClose(rs);
        }

        log.info("Determined SQL types ('{}', '{}', '{}', '{}')", nvarcharType, varcharType, binaryType, bigintType);
        // Set defaults
        if (nvarcharType != null) {
            varcharType = nvarcharType;
        } else if (varcharType == null) {
            varcharType = "VARCHAR(1000)";
        }
        if (binaryType == null) {
            binaryType = "BLOB";
        }
        if (bigintType == null) {
            bigintType = "BIGINT";
        }
        log.info("Used SQL types ('{}', '{}', '{}')", varcharType, binaryType, bigintType);

        return new String[] { varcharType, binaryType, bigintType };
    }

    private int getPrecision(ResultSet rs) throws SQLException {
        int prec = rs.getInt("PRECISION");
        if (prec > 1000) {
            prec = 1000;
        }
        return prec;
    }

    private String getRepoID(Connection conn) throws SQLException {
        String repoID = selectRepoID(conn);

        if (repoID != null) {
            return repoID;
        }
        createRepoID(conn);
        repoID = selectRepoID(conn);
        if (repoID != null) {
            return repoID;
        }
        throw new IllegalStateException("The row with ID = '" + OPENL_JCR_REPO_ID_KEY + "' has not created");
    }

    private String selectRepoID(Connection conn) throws SQLException {
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            statement = conn.prepareStatement(SELECT_ID);
            statement.setString(1, OPENL_JCR_REPO_ID_KEY);
            rs = statement.executeQuery();
            if (rs.next()) {
                InputStream binaryStream = rs.getBinaryStream(1);
                return IOUtils.toStringAndClose(binaryStream);
            } else {
                return null;
            }
        } catch (IOException e) {
            log.error("Unexpected IO failure", e);
            return null;
        } finally {
            safeClose(rs);
            safeClose(statement);
        }
    }

    private void createRepoID(Connection conn) throws SQLException {
        String repoId = "openl-jcr-repo-" + UUID.randomUUID().toString();
        PreparedStatement statement = null;
        try {
            statement = conn.prepareStatement(INSERT_ID);
            statement.setString(1, OPENL_JCR_REPO_ID_KEY);
            statement.setBytes(2, StringUtils.toBytes(repoId));
            statement.executeUpdate();
        } finally {
            safeClose(statement);
        }
    }

    private void safeClose(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                log.warn("Unexpected sql failure", e);
            }
        }
    }

    private void safeClose(Statement st) {
        if (st != null) {
            try {
                st.close();
            } catch (SQLException e) {
                log.warn("Unexpected sql failure", e);
            }
        }
    }

}
