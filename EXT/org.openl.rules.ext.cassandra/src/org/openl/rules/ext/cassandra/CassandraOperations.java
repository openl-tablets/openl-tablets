package org.openl.rules.ext.cassandra;

/*-
 * #%L
 * OpenL - EXT - Cassandra
 * %%
 * Copyright (C) 2016 - 2019 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

import java.util.Map;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.core.cql.SimpleStatementBuilder;
import com.datastax.oss.driver.api.core.cql.Statement;

public class CassandraOperations {

    private static class CassandraOperationsHolder {
        private static final CassandraOperations INSTANCE = new CassandraOperations();
    }

    private static final String CASSANDRA_PROPERTIES_FILE_NAME = "cassandra.properties";

    public static CassandraOperations getInstance() {
        return CassandraOperationsHolder.INSTANCE;
    }

    private volatile CqlSession session;

    public CqlSession getSession() {
        if (session == null) {
            synchronized (this) {
                if (session == null) {
                    session = CqlSession.builder()
                        .withConfigLoader(DriverConfigLoader.fromClasspath(CASSANDRA_PROPERTIES_FILE_NAME))
                        .build();
                }
            }
        }
        return session;
    }

    public static final ResultSet cassandraExecute(String query) {
        return CassandraOperations.getInstance().getSession().execute(query);
    }

    public static final ResultSet cassandraExecute(String query, Object... values) {
        SimpleStatement statement = SimpleStatement.builder(query).addPositionalValues(values).build();
        return CassandraOperations.getInstance().getSession().execute(statement);
    }

    public static final ResultSet cassandraExecute(String query, Map<String, Object> values) {
        SimpleStatementBuilder statementBuilder = SimpleStatement.builder(query);
        if (values != null) {
            values.entrySet().forEach(e -> statementBuilder.addNamedValue(e.getKey(), e.getValue()));
        }
        return CassandraOperations.getInstance().getSession().execute(statementBuilder.build());
    }

    public static final ResultSet cassandraExecute(Statement<?> statement) {
        return CassandraOperations.getInstance().getSession().execute(statement);
    }
}
