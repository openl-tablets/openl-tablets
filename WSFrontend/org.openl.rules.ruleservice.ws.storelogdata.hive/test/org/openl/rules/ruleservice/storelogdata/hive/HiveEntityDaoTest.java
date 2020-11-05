package org.openl.rules.ruleservice.storelogdata.hive;

import static org.mockito.Matchers.anyString;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class HiveEntityDaoTest {
    Connection connection;
    PreparedStatement preparedStatement;

    @Before
    public void init() throws SQLException {
        connection = Mockito.mock(Connection.class);
        preparedStatement = Mockito.mock(PreparedStatement.class);
        Mockito.doReturn(true).when(preparedStatement).execute();
        Mockito.when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    }

    @Test
    public void insertTest_defaultEntity() throws SQLException, IllegalAccessException {
        HiveEntityDao hiveEntityDao = new HiveEntityDao(connection, DefaultHiveEntity.class);
        DefaultHiveEntity defaultHiveEntity = getDefaultHiveEntity();
        hiveEntityDao.insert(defaultHiveEntity);
    }

    @Test
    public void insertTest_defaultEntity_NullField() throws SQLException, IllegalAccessException {
        HiveEntityDao hiveEntityDao = new HiveEntityDao(connection, DefaultHiveEntity.class);
        DefaultHiveEntity defaultHiveEntity = getDefaultHiveEntity();
        defaultHiveEntity.setId(null);
        defaultHiveEntity.setIncomingTime(null);
        hiveEntityDao.insert(defaultHiveEntity);
    }

    @Test
    public void insertTest_simpleEntity() throws SQLException, IllegalAccessException {
        HiveEntityDao hiveEntityDao = new HiveEntityDao(connection, SimpleEntity.class);
        SimpleEntity simpleEntity = getSimpleEntity();
        hiveEntityDao.insert(simpleEntity);
    }

    private DefaultHiveEntity getDefaultHiveEntity() {
        DefaultHiveEntity defaultHiveEntity = new DefaultHiveEntity();
        defaultHiveEntity.setId("id");
        defaultHiveEntity.setIncomingTime(ZonedDateTime.now());
        defaultHiveEntity.setMethodName("methodName");
        defaultHiveEntity.setOutcomingTime(ZonedDateTime.now());
        defaultHiveEntity.setPublisherType("REST");
        defaultHiveEntity.setRequest("request");
        defaultHiveEntity.setResponse("response");
        defaultHiveEntity.setServiceName("serviceName");
        defaultHiveEntity.setUrl("url");
        return defaultHiveEntity;
    }

    private SimpleEntity getSimpleEntity() {
        SimpleEntity simpleEntity = new SimpleEntity();
        simpleEntity.setId("id");
        simpleEntity.setBooleanValue(Boolean.TRUE);
        simpleEntity.setByteValue(Byte.MAX_VALUE);
        simpleEntity.setLongValue(Long.MAX_VALUE);
        simpleEntity.setIntegerValue(Integer.MAX_VALUE);
        simpleEntity.setLdtValue(LocalDateTime.now());
        simpleEntity.setZdtValue(ZonedDateTime.now());
        simpleEntity.setShortValue(Short.MAX_VALUE);
        simpleEntity.setRefValue(new Object());
        return simpleEntity;
    }
}
