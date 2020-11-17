package org.openl.rules.ruleservice.storelogdata.hive;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openl.rules.ruleservice.storelogdata.hive.annotation.Entity;
import org.openl.util.StringUtils;

public final class HiveStatementBuilder {

    private static final String INSERT_QUERY = "INSERT INTO TABLE %s (%s) VALUES (%s)";
    private final Class<?> entityClass;
    private final Connection connection;

    public HiveStatementBuilder(Connection connection, Class<?> entityClass) {
        this.connection = Objects.requireNonNull(connection, "connection cannot be null");
        this.entityClass = Objects.requireNonNull(entityClass, "entityClass cannot be null");
        if (!entityClass.isAnnotationPresent(Entity.class)) {
            throw new IllegalArgumentException(
                String.format("Expected class annotated with '%s'", Entity.class.getTypeName()));
        }
    }

    public PreparedStatement buildInsertStatement() throws SQLException {
        return connection.prepareStatement(buildQuery());
    }

    String buildQuery() {
        return String.format(INSERT_QUERY, getTableName(), getFields(), getParameters());
    }

    String getTableName() {
        Entity entity = entityClass.getAnnotation(Entity.class);
        return entity != null && StringUtils.isNotBlank(entity.value()) ? entity.value() : entityClass.getSimpleName();
    }

    private String getFields() {
        return Arrays.stream(entityClass.getDeclaredFields())
            .filter(f -> !f.isSynthetic())
            .map(f -> f.getName().toLowerCase())
            .sorted()
            .collect(Collectors.joining(","));
    }

    private String getParameters() {
        return Arrays.stream(entityClass.getDeclaredFields())
            .filter(f -> !f.isSynthetic())
            .map(f -> "?")
            .collect(Collectors.joining(","));
    }
}
