package org.openl.rules.ruleservice.storelogdata.hive;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.openl.rules.ruleservice.storelogdata.hive.annotation.Entity;

public class HiveQueryBuilder {

    private static final String query = "INSERT INTO TABLE %s (%s) VALUES (%s)";
    private Class<?> entityClass;
    private Connection connection;

    public HiveQueryBuilder() {
    }

    public HiveQueryBuilder withClass(Class entityClass) {
        this.entityClass = entityClass;
        return this;
    }

    public HiveQueryBuilder withConnection(Connection connection) {
        this.connection = connection;
        return this;
    }

    public PreparedStatement build() throws SQLException {
        return connection.prepareStatement(buildQuery());
    }

    public String buildQuery() {
        return String.format(query, getTableName(), getFields(), getParameters());
    }

    private String getTableName() {
        Entity entity = entityClass.getAnnotation(Entity.class);
        return entity == null ? entityClass.getName() : entity.value();
    }

    private String getFields() {
        return Arrays.stream(entityClass.getDeclaredFields()).map(f -> f.getName().toLowerCase()).sorted().collect(Collectors.joining(","));
    }

    private String getParameters() {
        return Arrays.stream(entityClass.getDeclaredFields()).map(f -> "?").collect(Collectors.joining(","));
    }
}
