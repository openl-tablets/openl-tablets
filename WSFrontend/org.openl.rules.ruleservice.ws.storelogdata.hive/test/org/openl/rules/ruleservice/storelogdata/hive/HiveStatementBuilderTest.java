package org.openl.rules.ruleservice.storelogdata.hive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HiveStatementBuilderTest {

    @Test
    public void testCreateInsertStatement_defaultEntity() {
        HiveStatementBuilder builder = new HiveStatementBuilder(DefaultHiveEntity.class);
        String insertStatement = builder.buildQuery();
        String expectedStatement = "INSERT INTO TABLE openl_log_data  (id,incomingtime,methodname,outcomingtime,publishertype,request,response,servicename,url) VALUES (?,?,?,?,?,?,?,?,?)";
        assertEquals(expectedStatement, insertStatement);
    }

    @Test
    public void testCreateInsertStatement_defaultPartitionEntity() {
        HiveStatementBuilder builder = new HiveStatementBuilder(PartitionedHiveEntity.class);
        String insertStatement = builder.buildQuery();
        String expectedStatement = "INSERT INTO TABLE partitioned_data PARTITION (incomingtime=?,outcomingtime=?) (id,request) VALUES (?,?)";
        assertEquals(expectedStatement, insertStatement);
    }

    @Test
    public void testCreateInsertStatement_emptyTableName() {
        HiveStatementBuilder builder = new HiveStatementBuilder(SimpleEntity.class);
        String insertStatement = builder.buildQuery();
        System.out.println(insertStatement);
        assertNotNull(insertStatement);
        assertTrue(insertStatement.startsWith("INSERT INTO TABLE SimpleEntity"));
    }

    @Test(expected = NullPointerException.class)
    public void constructorTest_nullEntity() {
        new HiveStatementBuilder(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorTest_notAnnotated() {
        new HiveStatementBuilder( NotAnnotatedEntity.class);
    }

}
