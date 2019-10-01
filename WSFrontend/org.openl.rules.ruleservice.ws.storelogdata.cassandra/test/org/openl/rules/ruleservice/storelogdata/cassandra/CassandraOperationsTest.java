package org.openl.rules.ruleservice.storelogdata.cassandra;

import org.junit.Assert;
import org.junit.Test;
import org.openl.rules.ruleservice.storelogdata.cassandra.CassandraOperations;

public class CassandraOperationsTest {

    @Test
    public void removeCommentsTest() {
        String linesWithComments = "CREATE TABLE rating_aaa_ss (" + System
            .lineSeparator() + "/*incomingtime timestamp,*/incomingtime2 timestamp," + System
                .lineSeparator() + "outcomingtime timestamp," + System.lineSeparator() + "id text," + System
                    .lineSeparator() + "--request text," + System
                        .lineSeparator() + "response text, --response text," + System
                            .lineSeparator() + "servicename text,//servicename text," + System
                                .lineSeparator() + "//servicename text," + System
                                    .lineSeparator() + "PRIMARY KEY (id) /*WITH CLUSTERING ORDER BY (column1 DESC);*/";

        String lines = "CREATE TABLE rating_aaa_ss (" + System.lineSeparator() + "incomingtime2 timestamp," + System
            .lineSeparator() + "outcomingtime timestamp," + System.lineSeparator() + "id text," + System
                .lineSeparator() + "" + System.lineSeparator() + "response text, " + System
                    .lineSeparator() + "servicename text," + System
                        .lineSeparator() + "" + System.lineSeparator() + "PRIMARY KEY (id) ";

        Assert.assertEquals(lines, CassandraOperations.removeCommentsInStatement(linesWithComments));
    }

}
