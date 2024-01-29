package org.openl.rules.ruleservice.storelogdata.hive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

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

    @Test
    public void constructorTest_nullEntity() {
        assertThrows(NullPointerException.class, () -> {
            new HiveStatementBuilder(null);
        });
    }

    @Test
    public void constructorTest_notAnnotated() {
        assertThrows(IllegalArgumentException.class, () -> {
            new HiveStatementBuilder( NotAnnotatedEntity.class);
        });
    }

}
