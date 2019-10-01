package org.openl.rules.ruleservice.storelogdata.cassandra;

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

import com.datastax.driver.core.*;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
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
    private boolean shemaCreationEnabled = false;
    private ProtocolVersion protocolVersion = ProtocolVersion.V3;

    private void init() {
        Cluster.Builder clusterBuilder = Cluster.builder();
        clusterBuilder.addContactPoint(contactpoints).withPort(Integer.valueOf(port));
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

    private Set<String> alreadyCreatedTableNames = new HashSet<>();

    public void save(Object entity) {
        if (entity == null) {
            return;
        }
        try {
            createShemaIfMissed(entity.getClass());
            @SuppressWarnings("unchecked")
            Mapper<Object> mapper = (Mapper<Object>) mappingManager.mapper(entity.getClass());
            mapper.save(entity);
        } catch (Exception e) {
            log.error("Failed on cassandra entity save operation.", e);
        }
    }

    private ExecutorService singleThreadExecuror = Executors.newSingleThreadExecutor();

    public void saveAsync(Object entity) {
        if (entity == null) {
            return;
        }
        try {
            createShemaIfMissed(entity.getClass());
            @SuppressWarnings("unchecked")
            Mapper<Object> mapper = (Mapper<Object>) mappingManager.mapper(entity.getClass());
            final ListenableFuture<Void> listenableFuture = mapper.saveAsync(entity);
            listenableFuture.addListener(() -> {
                try {
                    listenableFuture.get();
                } catch (Exception e) {
                    log.error("Failed on cassandra entity save operation.", e);
                }
            }, singleThreadExecuror);
        } catch (Exception e) {
            log.error("Failed on cassandra entity save operation.", e);
        }
    }

    public void createShemaIfMissed(Class<?> entityClass) {
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
                                    String cqlQuery = extractCqlQueryForEntity(entityClass);
                                    String[] queries = cqlQuery.split(";");
                                    for (String q : queries) {
                                        session.execute(removeCommentsInStatement(q.trim()));
                                    }
                                } catch (IOException e) {
                                    throw new SchemaCreationException(
                                        String.format("Failed to extract a file with schema creation CQL query for '%s'.",
                                            entityClass.getTypeName()),
                                        e);
                                } catch (QueryExecutionException | QueryValidationException
                                        | NoHostAvailableException e) {
                                    throw new SchemaCreationException(
                                        String.format("Failed to execute schema creation CQL for '%s'",
                                            entityClass.getTypeName()),
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

    private String extractCqlQueryForEntity(Class<?> entityClass) throws IOException {
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
        return shemaCreationEnabled;
    }

    public void setCreateShemaEnabled(boolean createShemaEnabled) {
        this.shemaCreationEnabled = createShemaEnabled;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (contactpoints == null || contactpoints.trim().isEmpty()) {
            throw new BeanInitializationException(
                "Property 'contactpoints' is required. Please, check your configuration.");
        } else {
            contactpoints = contactpoints.trim();
        }
        if (port == null || port.trim().isEmpty()) {
            throw new BeanInitializationException("Property 'port' is required. Please, check your configuration.");
        } else {
            port = port.trim();
        }
        if (keyspace == null || keyspace.trim().isEmpty()) {
            throw new BeanInitializationException("Property 'keyspace' is required! Please, check your configuration.");
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
            log.error("Cassandra initialization failure!", e);
            cluster.close();

        }
    }
}
