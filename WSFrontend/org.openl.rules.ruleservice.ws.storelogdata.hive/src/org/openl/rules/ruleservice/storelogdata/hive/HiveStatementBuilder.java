package org.openl.rules.ruleservice.storelogdata.hive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openl.rules.ruleservice.storelogdata.hive.annotation.Entity;
import org.openl.rules.ruleservice.storelogdata.hive.annotation.Partition;
import org.openl.util.StringUtils;

public final class HiveStatementBuilder {

    private static final String INSERT_QUERY = "INSERT INTO TABLE %s %s (%s) VALUES (%s)";

    private final Class<?> entityClass;

    public HiveStatementBuilder(Class<?> entityClass) {
        this.entityClass = Objects.requireNonNull(entityClass, "entityClass cannot be null");
        if (!entityClass.isAnnotationPresent(Entity.class)) {
            throw new IllegalArgumentException(
                    String.format("Expected class annotated with '%s'", Entity.class.getTypeName()));
        }
    }

    String buildQuery() {
        return String.format(INSERT_QUERY, getTableName(), getPartitions(), getFields(), getParameters());
    }

    private String getTableName() {
        Entity entity = entityClass.getAnnotation(Entity.class);
        return entity != null && StringUtils.isNotBlank(entity.value()) ? entity.value() : entityClass.getSimpleName();
    }

    private String getPartitions() {
        List<String> partitionFields = getPartitionFields();
        return partitionFields.size() == 0 ? "" :
                partitionFields.stream().map(f -> f + "=?")
                        .collect(Collectors.joining(",", "PARTITION (", ")"));
    }

    private List<String> getPartitionFields() {
        return Arrays.stream(entityClass.getDeclaredFields())
                .filter(f -> !f.isSynthetic() && f.isAnnotationPresent(Partition.class))
                .sorted(Comparator.comparingInt(f -> f.getAnnotation(Partition.class).value()))
                .map(f -> f.getName().toLowerCase())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private String getFields() {
        return Arrays.stream(entityClass.getDeclaredFields())
                .filter(f -> !f.isSynthetic() && !f.isAnnotationPresent(Partition.class))
                .map(f -> f.getName().toLowerCase())
                .sorted()
                .collect(Collectors.joining(","));
    }

    private String getParameters() {
        return Arrays.stream(entityClass.getDeclaredFields())
                .filter(f -> !f.isSynthetic() && !f.isAnnotationPresent(Partition.class))
                .map(f -> "?")
                .collect(Collectors.joining(","));
    }
}
