package org.openl.rules.ruleservice.storelogdata.hive;

import org.junit.Assert;
import org.junit.Test;

public class HiveOperationTest {

    @Test
    public void removeCommentsTest() {
        String linesWithComments = "CREATE TABLE rating_issued (" + System
                .lineSeparator() + "/*incomingtime timestamp,*/incomingtime2 timestamp," + System
                .lineSeparator() + "outcomingtime timestamp," + System.lineSeparator() + "id text," + System
                .lineSeparator() + "--request text," + System
                .lineSeparator() + "response text, --response text," + System
                .lineSeparator() + "servicename text,//servicename text," + System
                .lineSeparator() + "//servicename text," + System
                .lineSeparator() + "PRIMARY KEY (id) /*disable novalidate*/";

        String lines = "CREATE TABLE rating_issued (" + System.lineSeparator() + "incomingtime2 timestamp," + System
                .lineSeparator() + "outcomingtime timestamp," + System.lineSeparator() + "id text," + System
                .lineSeparator() + "" + System.lineSeparator() + "response text, " + System
                .lineSeparator() + "servicename text," + System
                .lineSeparator() + "" + System.lineSeparator() + "PRIMARY KEY (id) ";

        Assert.assertEquals(lines, HiveOperations.removeCommentsInStatement(linesWithComments));
    }
}
