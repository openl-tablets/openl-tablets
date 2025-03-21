package org.openl.rules.ruleservice.storelogdata.hive;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HiveEntityDaoTest {
    Connection connection;
    PreparedStatement preparedStatement;

    @BeforeEach
    public void init() throws SQLException {
        connection = mock(Connection.class);
        preparedStatement = mock(PreparedStatement.class);
        doReturn(true).when(preparedStatement).execute();
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    }

    @Test
    public void insertTest_defaultEntity() throws SQLException, IllegalAccessException, UnsupportedFieldTypeException {
        HiveEntityDao hiveEntityDao = new HiveEntityDao(DefaultHiveEntity.class);
        DefaultHiveEntity defaultHiveEntity = getDefaultHiveEntity();
        hiveEntityDao.insert(connection, defaultHiveEntity);
    }

    @Test
    public void insertTest_partitionedEntity() throws SQLException, IllegalAccessException, UnsupportedFieldTypeException {
        HiveEntityDao hiveEntityDao = new HiveEntityDao(PartitionedHiveEntity.class);
        PartitionedHiveEntity partitionedEntity = getPartitionedEntity();
        hiveEntityDao.insert(connection, partitionedEntity);
    }

    @Test
    public void insertTest_defaultEntity_NullField() throws SQLException, IllegalAccessException, UnsupportedFieldTypeException {
        HiveEntityDao hiveEntityDao = new HiveEntityDao(DefaultHiveEntity.class);
        DefaultHiveEntity defaultHiveEntity = getDefaultHiveEntity();
        defaultHiveEntity.setId(null);
        defaultHiveEntity.setIncomingTime(null);
        hiveEntityDao.insert(connection, defaultHiveEntity);
    }

    @Test
    public void insertTest_supportTypes() throws SQLException, IllegalAccessException, UnsupportedFieldTypeException {
        HiveEntityDao hiveEntityDao = new HiveEntityDao(SimpleEntity.class);
        SimpleEntity simpleEntity = getSimpleEntity();
        hiveEntityDao.insert(connection, simpleEntity);
    }

    @Test
    public void insertTest_nullValue() throws SQLException, IllegalAccessException, UnsupportedFieldTypeException {
        HiveEntityDao hiveEntityDao = new HiveEntityDao(DefaultHiveEntity.class);
        DefaultHiveEntity defaultHiveEntity = getDefaultHiveEntity();
        defaultHiveEntity.setOutcomingTime(null);
        defaultHiveEntity.setId(null);
        hiveEntityDao.insert(connection, defaultHiveEntity);
    }

    @Test
    public void insertTest_unsupportedType() throws UnsupportedFieldTypeException {
        assertThrows(UnsupportedFieldTypeException.class, () -> {
            new HiveEntityDao(WrongTypeEntity.class);
        });
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

    private PartitionedHiveEntity getPartitionedEntity() {
        PartitionedHiveEntity entity = new PartitionedHiveEntity();
        entity.setId("id");
        entity.setIncomingTime(ZonedDateTime.now());
        entity.setOutcomingTime(ZonedDateTime.now());
        entity.setRequest("request");
        return entity;
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
        simpleEntity.setLocalDateValue(LocalDate.now());
        simpleEntity.setDateValue(Date.from(Instant.now()));
        return simpleEntity;
    }
}
