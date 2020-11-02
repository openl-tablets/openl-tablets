package org.openl.rules.ruleservice.storelogdata.hive;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class HiveQueryBuilderTest {

    @Test
    public void testCreateInsertStatement_defaultEntity() {
        HiveQueryBuilder builder = new HiveQueryBuilder();
        builder.withClass(DefaultHiveEntity.class);
        String insertStatement = builder.buildQuery();
        String expectedStatement = "INSERT INTO TABLE openl_log_data (id,incomingtime,methodname,outcomingtime," +
                "publishertype,request,response,servicename,url) VALUES (?,?,?,?,?,?,?,?,?)";
        assertEquals(expectedStatement, insertStatement);
    }

}
