package org.openl.rules.ruleservice.storelogdata.cassandra;

import org.openl.rules.ruleservice.storelogdata.StoreLogData;
import org.openl.rules.ruleservice.storelogdata.StoreLogDataConvertor;

import com.datastax.driver.core.utils.UUIDs;

public class TimeBasedUUID implements StoreLogDataConvertor<String> {
    @Override
    public String convert(StoreLogData storeLogData) {
        return UUIDs.timeBased().toString();
    }
}
