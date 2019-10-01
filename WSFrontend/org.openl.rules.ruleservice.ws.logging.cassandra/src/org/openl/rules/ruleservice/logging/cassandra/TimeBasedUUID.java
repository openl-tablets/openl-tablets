package org.openl.rules.ruleservice.logging.cassandra;

import org.openl.rules.ruleservice.logging.StoreLogData;
import org.openl.rules.ruleservice.logging.StoreLogDataConvertor;

import com.datastax.driver.core.utils.UUIDs;

public class TimeBasedUUID implements StoreLogDataConvertor<String> {
    @Override
    public String convert(StoreLogData storeLogData) {
        return UUIDs.timeBased().toString();
    }
}
