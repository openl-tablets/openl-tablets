package org.openl.rules.ruleservice.storelogdata.hive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;

import org.junit.Test;
import org.mockito.Mockito;

public class HiveStatementBuilderTest {

    @Test
    public void testCreateInsertStatement_defaultEntity() {
        Connection connection = Mockito.mock(Connection.class);
        HiveStatementBuilder builder = new HiveStatementBuilder(connection, DefaultHiveEntity.class);
        String insertStatement = builder.buildQuery();
        String expectedStatement = "INSERT INTO TABLE openl_log_data (id,incomingtime,methodname,outcomingtime,"
                + "publishertype,request,response,servicename,url) VALUES (?,?,?,?,?,?,?,?,?)";
        assertEquals(expectedStatement, insertStatement);
    }

    @Test
    public void testCreateInsertStatement_emptyTableName() {
        Connection connection = Mockito.mock(Connection.class);
        HiveStatementBuilder builder = new HiveStatementBuilder(connection, SimpleEntity.class);
        String insertStatement = builder.buildQuery();
        System.out.println(insertStatement);
        assertNotNull(insertStatement);
        assertTrue(insertStatement.startsWith("INSERT INTO TABLE SimpleEntity"));
    }

    @Test(expected = NullPointerException.class)
    public void constructorTest_nullConnection() {
        new HiveStatementBuilder(null, SimpleEntity.class);
    }

    @Test(expected = NullPointerException.class)
    public void constructorTest_nullEntity() {
        Connection connection = Mockito.mock(Connection.class);
        new HiveStatementBuilder(connection, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorTest_notAnnotated() {
        Connection connection = Mockito.mock(Connection.class);
        new HiveStatementBuilder(connection, NotAnnotatedEntity.class);
    }

}
