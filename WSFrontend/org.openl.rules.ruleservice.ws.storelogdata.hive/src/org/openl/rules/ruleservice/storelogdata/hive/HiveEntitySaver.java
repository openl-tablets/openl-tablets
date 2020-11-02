package org.openl.rules.ruleservice.storelogdata.hive;

import java.sql.Connection;
import java.sql.SQLException;

public class HiveEntitySaver {
    private final HiveEntityDao hiveEntityDao;

    public HiveEntitySaver(Connection connection, Class<?> entityClass) throws SQLException {
        hiveEntityDao = new HiveEntityDao(connection, entityClass);
    }

    public void insert(Object entity) throws IllegalAccessException, SQLException {
        hiveEntityDao.insert(entity);
    }
}
