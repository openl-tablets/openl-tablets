package org.openl.rules.ext.cassandra;

/*-
 * #%L
 * OpenL - STUDIO - Cassandra
 * %%
 * Copyright (C) 2016 - 2019 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.Session;

public class CassandraOperations {

    private static class CassandraOperationsHolder {
        private static final CassandraOperations INSTANCE = new CassandraOperations();
    }

    private static final String CASSANDRA_PROPERTIES_FILE_NAME = "cassandra.properties";

    public static CassandraOperations getInstance() {
        return CassandraOperationsHolder.INSTANCE;
    }

    private volatile Cluster cluster;
    private volatile Session session;
    private volatile Properties props;

    public Properties getProperties() throws IOException {
        if (props == null) {
            synchronized (this) {
                if (props == null) {
                    InputStream inStream = CassandraOperations.class.getClassLoader()
                        .getResourceAsStream(CASSANDRA_PROPERTIES_FILE_NAME);
                    Properties props1 = new Properties();
                    props1.load(inStream);
                    props = props1;
                }
            }
        }
        return props;
    }

    public Session getSession() throws IOException {
        if (session == null) {
            synchronized (this) {
                if (session == null) {
                    Properties props = getProperties();

                    String keyspace = props.getProperty("keyspace");
                    if (keyspace != null) {
                        keyspace = keyspace.trim();
                    }

                    Cluster cluster1 = getCluster();
                    if (cluster1 != null) {
                        session = cluster1.connect(keyspace);
                    }
                }
            }
        }
        return session;
    }

    public Cluster getCluster() throws IOException {
        if (cluster == null) {
            synchronized (this) {
                if (cluster == null) {
                    Properties props = getProperties();

                    Cluster.Builder clusterBuilder = Cluster.builder();

                    String contactpoints = props.getProperty("contactpoints");
                    if (contactpoints != null) {
                        contactpoints = contactpoints.trim();
                    }

                    Integer port = null;
                    if (props.getProperty("port") == null) {
                        port = 9042;
                    } else {
                        port = Integer.valueOf(props.getProperty("port").trim());
                    }

                    clusterBuilder.addContactPoint(contactpoints).withPort(port);

                    String username = props.getProperty("username");
                    if (username != null) {
                        username = username.trim();
                    }
                    String password = props.getProperty("password");
                    if (password == null) {
                        password = "";
                    }
                    if (username != null) {
                        clusterBuilder.withCredentials(username, password);
                    }
                    Cluster cluster1 = clusterBuilder.build();

                    PoolingOptions poolingOptions = cluster1.getConfiguration().getPoolingOptions();
                    poolingOptions.setConnectionsPerHost(HostDistance.LOCAL, 5, 12)
                        .setConnectionsPerHost(HostDistance.REMOTE, 2, 4);
                    poolingOptions.setMaxRequestsPerConnection(HostDistance.LOCAL, 2048)
                        .setMaxRequestsPerConnection(HostDistance.REMOTE, 512);

                    cluster = cluster1;
                }
            }
        }
        return cluster;
    }
}
