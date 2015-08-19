package org.openl.rules.security.common.spring.db;

import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.DatabaseSequenceFilter;
import org.dbunit.dataset.CompositeDataSet;
import org.dbunit.dataset.FilteredDataSet;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ReplacementDataSet;
import org.dbunit.dataset.datatype.IDataTypeFactory;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.operation.DatabaseOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Populates database using dbunit.
 *
 * @author Andrey Naumenko
 */
public class DbUnitDatabasePopulator implements InitializingBean {
    private final Logger log = LoggerFactory.getLogger(DbUnitDatabasePopulator.class);
    private Resource[] locations;
    private DataSource dataSource;
    private boolean enabled = true;
    private boolean ordered = true;
    private IDataTypeFactory dataTypeFactory = null;
    private String dbSchema = null;
    private DatabaseOperation databaseOperation = DatabaseOperation.CLEAN_INSERT;

    public void afterPropertiesSet() throws Exception {
        if (enabled) {
            log.info("DbUnitDatabasePopulator is enabled");
            populateDatabase();
        } else {
            log.info("DbUnitDatabasePopulator is disabled");
        }
    }

    private void commitIfNecessary(Connection connection) throws SQLException {
        if (!connection.getAutoCommit()) {
            connection.commit();
        }
    }

    private DatabaseConnection createDbUnitConnection(Connection connection) throws DatabaseUnitException {
        DatabaseConnection databaseConnection;
        if (dbSchema != null && !dbSchema.isEmpty()) {
            databaseConnection = new DatabaseConnection(connection, dbSchema);
        } else {
            databaseConnection = new DatabaseConnection(connection);
        }

        // set DataTypeFactory
        if (dataTypeFactory != null) {
            log.debug("Using {}", dataTypeFactory);
            DatabaseConfig config = databaseConnection.getConfig();
            config.setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, dataTypeFactory);
        }

        return databaseConnection;
    }

    public void importResource(Resource resource) throws IOException, SQLException, DatabaseUnitException {
        Connection connection = dataSource.getConnection();
        DatabaseConnection databaseConnection = createDbUnitConnection(connection);
        try {
            log.debug("Loading data from {}", resource.getFilename());
            IDataSet dataSet = new FlatXmlDataSet(resource.getInputStream());
            if (ordered) {
                // replace with ordered dataset
                dataSet = new FilteredDataSet(new DatabaseSequenceFilter(databaseConnection), dataSet);
            }

            // [null] => NULL
            dataSet = new ReplacementDataSet(dataSet);
            ((ReplacementDataSet) dataSet).addReplacementObject("[null]", null);

            DatabaseOperation.INSERT.execute(databaseConnection, dataSet);
            commitIfNecessary(connection);
        } finally {
            databaseConnection.close();
        }
    }

    private void populateDatabase() throws Exception {
        IDataSet[] dataSets = new IDataSet[locations.length];
        for (int i = 0; i < locations.length; i++) {
            Resource res = locations[i];
            log.info("Loading data from {}", res.getFilename());

            dataSets[i] = new FlatXmlDataSet(res.getInputStream());
        }

        IDataSet dataSet = new CompositeDataSet(dataSets);

        Connection connection = dataSource.getConnection();
        DatabaseConnection databaseConnection = createDbUnitConnection(connection);

        try {
            if (ordered) {
                // replace with ordered dataset
                dataSet = new FilteredDataSet(new DatabaseSequenceFilter(databaseConnection), dataSet);
            }

            // [null] => NULL
            dataSet = new ReplacementDataSet(dataSet);
            ((ReplacementDataSet) dataSet).addReplacementObject("[null]", null);

            databaseOperation.execute(databaseConnection, dataSet);
            commitIfNecessary(connection);
        } finally {
            databaseConnection.close();
        }
    }

    /**
     * {@link DatabaseOperation} that will be used to insert data in database.
     * {@link DatabaseOperation#CLEAN_INSERT} by default. You should use {@link
     * org.dbunit.ext.mssql.InsertIdentityOperation} for MSSQL database.
     *
     * @param databaseOperation databaseOperation.
     */
    public void setDatabaseOperation(DatabaseOperation databaseOperation) {
        this.databaseOperation = databaseOperation;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * DbUnit IDataTypeFactory. See DBUnit docs for more details.
     *
     * @param dataTypeFactory
     */
    public void setDataTypeFactory(IDataTypeFactory dataTypeFactory) {
        this.dataTypeFactory = dataTypeFactory;
    }

    /**
     * Database schema that will be used by DBUnit DatabaseConnection.
     * <code>null</code> by default.
     *
     * @param dbSchema name of schema
     */
    public void setDbSchema(String dbSchema) {
        this.dbSchema = dbSchema;
    }

    /**
     * If <code>false</code> then do nothing. Default value <code>true</code>.
     *
     * @param enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Locations of resources with DbUnit datasets in flat xml format.
     *
     * @param locations
     */
    public void setLocations(Resource[] locations) {
        this.locations = locations;
    }

    /**
     * If <code>true</code> dataset will be ordered topologically before
     * import.
     *
     * @param ordered
     */
    public void setOrdered(boolean ordered) {
        this.ordered = ordered;
    }
}
