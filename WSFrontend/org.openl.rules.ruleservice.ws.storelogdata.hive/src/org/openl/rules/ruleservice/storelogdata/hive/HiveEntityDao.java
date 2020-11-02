package org.openl.rules.ruleservice.storelogdata.hive;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Comparator;

public class HiveEntityDao {

    private final PreparedStatement preparedStatement;
    private final Field[] sortedFields;

    public HiveEntityDao(Connection connection, Class<?> entityClass) throws SQLException {
        preparedStatement = new HiveQueryBuilder().withConnection(connection).withClass(entityClass).build();
        sortedFields = entityClass.getDeclaredFields();
        Arrays.sort(sortedFields, Comparator.comparing(Field::getName));
        Arrays.stream(sortedFields).forEach(f -> f.setAccessible(true));
    }

    public void insert(Object entity) throws IllegalAccessException, SQLException {
        for (int index = 0; index < sortedFields.length; index++) {
            setValue(index + 1, sortedFields[index], entity);
        }
        preparedStatement.execute();
    }

    private void setValue(int index, Field field, Object entity) throws IllegalAccessException {
        try {
            if (Integer.class.equals(field.getType())) {
                preparedStatement.setInt(index, field.getInt(entity));
            } else if (Long.class.equals(field.getType())) {
                preparedStatement.setLong(index, field.getLong(entity));
            } else if (Boolean.class.equals(field.getType())) {
                preparedStatement.setBoolean(index, field.getBoolean(entity));
            } else if (Short.class.equals(field.getType())) {
                preparedStatement.setShort(index, field.getShort(entity));
            } else if (Byte.class.equals(field.getType())) {
                preparedStatement.setByte(index, field.getByte(entity));
            } else if (Double.class.equals(field.getType())) {
                preparedStatement.setDouble(index, field.getDouble(entity));
            } else if (Float.class.equals(field.getType())) {
                preparedStatement.setFloat(index, field.getFloat(entity));
            } else if (ZonedDateTime.class.equals(field.getType())) {
                preparedStatement.setTimestamp(index,
                    java.sql.Timestamp.valueOf(((ZonedDateTime) field.get(entity)).toLocalDateTime()));
            } else {
                preparedStatement.setObject(index, field.get(entity));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
