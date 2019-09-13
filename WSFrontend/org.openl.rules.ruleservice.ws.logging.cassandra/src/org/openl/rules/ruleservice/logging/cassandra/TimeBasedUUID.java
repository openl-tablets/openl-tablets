package org.openl.rules.ruleservice.logging.cassandra;

import org.openl.rules.ruleservice.logging.StoreLoggingData;
import org.openl.rules.ruleservice.logging.StoreLoggingDataConvertor;

import com.datastax.driver.core.utils.UUIDs;

public class TimeBasedUUID implements StoreLoggingDataConvertor<String> {
    @Override
    public String convert(StoreLoggingData storeLoggingData) {
        return UUIDs.timeBased().toString();
    }
}
