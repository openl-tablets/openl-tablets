package org.openl.rules.ruleservice.logging.cassandra;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.QueryExecutionException;
import com.datastax.driver.core.exceptions.QueryValidationException;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import com.datastax.driver.mapping.annotations.Table;
import com.google.common.util.concurrent.ListenableFuture;

public class CassandraOperations implements InitializingBean {
    private final Logger log = LoggerFactory.getLogger(CassandraOperations.class);

    private Cluster cluster;
    private Session session;
    private MappingManager mappingManager;
    private String contactpoints;
    private String port;
    private String keyspace;
    private String username;
    private String password;
    private boolean createShemaEnabled = false;
    private ProtocolVersion protocolVersion = ProtocolVersion.V3;

    private void init() {
        Cluster.Builder clusterBuilder = Cluster.builder();
        clusterBuilder.addContactPoints(contactpoints.split("\\s*,\\s*"));
        clusterBuilder.withPort(Integer.valueOf(port));
        if (username != null && password != null) {
            clusterBuilder.withCredentials(username, password);
        }
        cluster = clusterBuilder.build();
        PoolingOptions poolingOptions = cluster.getConfiguration().getPoolingOptions();
        poolingOptions.setConnectionsPerHost(HostDistance.LOCAL, 5, 12)
            .setConnectionsPerHost(HostDistance.REMOTE, 2, 4);
        poolingOptions.setMaxRequestsPerConnection(HostDistance.LOCAL, 2048)
            .setMaxRequestsPerConnection(HostDistance.REMOTE, 512);
        session = cluster.connect(keyspace);
        mappingManager = new MappingManager(session, getProtocolVersion());
    }

    public Session connect() {
        if (session == null) {
            init();
        }
        return session;
    }

    private Set<String> alreadyCreatedTableNames = new HashSet<String>();

    public void save(Object entity) {
        try {
            createShemaIfNeeded(entity.getClass());
            @SuppressWarnings("unchecked")
            Mapper<Object> mapper = (Mapper<Object>) mappingManager.mapper(entity.getClass());
            mapper.save(entity);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Save operation was failure!", e);
            }
        }
    }

    private ExecutorService sinleThreadExecuror = Executors.newSingleThreadExecutor();

    public void saveAsync(Object entity) {
        try {
            createShemaIfNeeded(entity.getClass());
            @SuppressWarnings("unchecked")
            Mapper<Object> mapper = (Mapper<Object>) mappingManager.mapper(entity.getClass());
            final ListenableFuture<Void> listenableFuture = mapper.saveAsync(entity);
            listenableFuture.addListener(new Runnable() {
                @Override
                public void run() {
                    try {
                        listenableFuture.get();
                    } catch (Exception e) {
                        if (log.isErrorEnabled()) {
                            log.error("Save operation was failure!", e);
                        }
                    }
                }
            }, sinleThreadExecuror);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Save operation was failure!", e);
            }
        }
    }

    public void createShemaIfNeeded(Class<?> entityClass) {
        if (isCreateShemaEnabled()) {
            Table table = entityClass.getAnnotation(Table.class);
            if (table != null) {
                String tableName = table.caseSensitiveTable() ? table.name() : table.name().toLowerCase();
                if (!alreadyCreatedTableNames.contains(tableName)) {
                    synchronized (this) {
                        String ksName = table.caseSensitiveKeyspace() ? table.keyspace()
                                                                      : table.keyspace().toLowerCase();
                        if (ksName == null || ksName.isEmpty()) {
                            ksName = keyspace;
                        }
                        if (!alreadyCreatedTableNames.contains(tableName)) {
                            if (session.getCluster().getMetadata().getKeyspace(ksName).getTable(tableName) == null) {
                                try {
                                    String cqlQuery = extractCqlQueryForClass(entityClass);
                                    String[] queries = cqlQuery.split(";");
                                    for (String q : queries) {
                                        session.execute(removeCommentsInStatement(q.trim()));
                                    }
                                } catch (IOException e) {
                                    if (log.isErrorEnabled()) {
                                        log.error("Table creation was failure!", e);
                                    }
                                } catch (QueryExecutionException e) {
                                    throw new SchemaCreationException(
                                        "Schema creation fails for '" + entityClass.getSimpleName() + "'",
                                        e);
                                } catch (QueryValidationException e) {
                                    throw new SchemaCreationException(
                                        "Schema creation fails for '" + entityClass.getSimpleName() + "'",
                                        e);
                                }
                            }
                            alreadyCreatedTableNames.add(tableName);
                        }
                    }
                }
            }
        }
    }

    protected static String removeCommentsInStatement(String statement) {
        return statement.replaceAll("(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)|(?:--.*)", "");
    }

    private String extractCqlQueryForClass(Class<?> entityClass) throws IOException {
        InputStream inputStream = entityClass
            .getResourceAsStream("/" + entityClass.getName().replaceAll("\\.", "/") + ".cql");
        if (inputStream == null) {
            throw new FileNotFoundException("/" + entityClass.getName().replaceAll("\\.", "/") + ".cql");
        }
        return IOUtils.toString(inputStream, "UTF-8");
    }

    public ProtocolVersion getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(ProtocolVersion protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    public Cluster getCluster() {
        return cluster;
    }

    public void setContactpoints(String contactpoints) {
        this.contactpoints = contactpoints;
    }

    public String getContactpoints() {
        return contactpoints;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getPort() {
        return port;
    }

    public void setKeyspace(String keyspace) {
        this.keyspace = keyspace;
    }

    public String getKeyspace() {
        return keyspace;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public boolean isCreateShemaEnabled() {
        return createShemaEnabled;
    }

    public void setCreateShemaEnabled(boolean createShemaEnabled) {
        this.createShemaEnabled = createShemaEnabled;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (contactpoints == null || contactpoints.trim().isEmpty()) {
            throw new BeanInitializationException(
                "Property 'contactpoints' must be defined! Please, check your configuration.");
        } else {
            contactpoints = contactpoints.trim();
        }
        if (port == null || port.trim().isEmpty()) {
            throw new BeanInitializationException("Property 'port' must be defined! Please, check your configuration.");
        } else {
            port = port.trim();
        }
        if (keyspace == null || keyspace.trim().isEmpty()) {
            throw new BeanInitializationException(
                "Property 'keyspace' must be defined! Please, check your configuration.");
        } else {
            keyspace = keyspace.trim();
        }
        if (username != null && username.trim().isEmpty()) {
            username = null;
        }
        if (password == null) {
            password = "";
        }
        try {
            init();
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Cassandra initialization failure!", e);
            }
            cluster.close();

        }
    }
}
