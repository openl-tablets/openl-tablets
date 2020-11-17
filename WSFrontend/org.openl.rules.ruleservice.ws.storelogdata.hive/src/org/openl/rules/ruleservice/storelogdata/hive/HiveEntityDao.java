package org.openl.rules.ruleservice.storelogdata.hive;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class HiveEntityDao {
    private final PreparedStatement preparedStatement;
    private final ArrayList<Field> sortedFields;
    private final Set<Class<?>> supportedTypes = new HashSet<>(Arrays.asList(Byte.class,
        byte.class,
        Short.class,
        short.class,
        Integer.class,
        int.class,
        Long.class,
        long.class,
        Float.class,
        float.class,
        Double.class,
        double.class,
        BigDecimal.class,
        Boolean.class,
        boolean.class,
        String.class,
        LocalDateTime.class,
        LocalDate.class,
        ZonedDateTime.class,
        Date.class));

    public HiveEntityDao(Connection connection, Class<?> entityClass) throws SQLException,
                                                                      UnsupportedFieldTypeException {
        checkTypes(entityClass);
        preparedStatement = new HiveStatementBuilder(connection, entityClass).buildInsertStatement();
        sortedFields = Arrays.stream(entityClass.getDeclaredFields())
                .filter(f -> !f.isSynthetic())
                .sorted(Comparator.comparing(Field::getName))
                .peek(f -> f.setAccessible(true))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private void checkTypes(Class<?> entityClass) throws UnsupportedFieldTypeException {
        for (Field field : entityClass.getDeclaredFields()) {
            if (!field.isSynthetic() && !supportedTypes.contains(field.getType())) {
                throw new UnsupportedFieldTypeException(String.format(
                    "Entity '%s' contains field '%s' with unsupported type '%s'. Allowed types are '%s'.",
                    entityClass.getTypeName(),
                    field.getName(),
                    field.getType().getTypeName(),
                    supportedTypes.stream().map(Class::getName).collect(Collectors.joining(", "))));
            }
        }
    }

    public void insert(Object entity) throws IllegalAccessException, SQLException, UnsupportedFieldTypeException {
        for (int index = 0; index < sortedFields.size(); index++) {
            setValue(index + 1, sortedFields.get(index), entity);
        }
        preparedStatement.execute();
    }

    private void setValue(int index, Field field, Object entity) throws IllegalAccessException,
                                                                 SQLException,
                                                                 UnsupportedFieldTypeException {
        if (String.class.equals(field.getType())) {
            preparedStatement.setString(index, (String) field.get(entity));
        } else if (Integer.class.equals(field.getType()) || int.class.equals(field.getType())) {
            preparedStatement.setInt(index, field.getInt(entity));
        } else if (Long.class.equals(field.getType()) || long.class.equals(field.getType())) {
            preparedStatement.setLong(index, field.getLong(entity));
        } else if (Boolean.class.equals(field.getType()) || boolean.class.equals(field.getType())) {
            preparedStatement.setBoolean(index, field.getBoolean(entity));
        } else if (Short.class.equals(field.getType()) || short.class.equals(field.getType())) {
            preparedStatement.setShort(index, field.getShort(entity));
        } else if (Byte.class.equals(field.getType()) || byte.class.equals(field.getType())) {
            preparedStatement.setByte(index, field.getByte(entity));
        } else if (Double.class.equals(field.getType()) || double.class.equals(field.getType())) {
            preparedStatement.setDouble(index, field.getDouble(entity));
        } else if (Float.class.equals(field.getType()) || float.class.equals(field.getType())) {
            preparedStatement.setFloat(index, field.getFloat(entity));
        } else if (BigDecimal.class.equals(field.getType())) {
            preparedStatement.setBigDecimal(index, (BigDecimal) field.get(entity));
        } else if (ZonedDateTime.class.equals(field.getType())) {
            ZonedDateTime zonedDateTime = (ZonedDateTime) field.get(entity);
            preparedStatement.setTimestamp(index,
                zonedDateTime == null ? null : java.sql.Timestamp.valueOf(zonedDateTime.toLocalDateTime()));
        } else if (LocalDateTime.class.equals(field.getType())) {
            LocalDateTime localDateTime = (LocalDateTime) field.get(entity);
            preparedStatement.setTimestamp(index, localDateTime == null ? null : Timestamp.valueOf(localDateTime));
        } else if (LocalDate.class.equals(field.getType())) {
            LocalDate date = (LocalDate) field.get(entity);
            preparedStatement.setDate(index, date == null ? null : java.sql.Date.valueOf(date));
        } else if (Date.class.equals(field.getType())) {
            Date date = (Date) field.get(entity);
            preparedStatement.setDate(index, date == null ? null : new java.sql.Date(date.getTime()));
        } else {
            throw new UnsupportedFieldTypeException(
                String.format("Field '%s' of class '%s' can not be stored. Unsupported field type '%s'.",
                    field.getName(),
                    field.getType().getTypeName(),
                    field.getType()));
        }
    }

}
