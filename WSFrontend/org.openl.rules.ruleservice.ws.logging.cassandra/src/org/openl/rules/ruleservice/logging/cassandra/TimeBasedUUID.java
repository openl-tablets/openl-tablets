package org.openl.rules.ruleservice.logging.cassandra;

import org.openl.rules.ruleservice.logging.StoreLoggingData;
import org.openl.rules.ruleservice.logging.StoreLoggingConvertor;

import com.datastax.driver.core.utils.UUIDs;

public class TimeBasedUUID implements StoreLoggingConvertor<String> {
    @Override
    public String convert(StoreLoggingData storeLoggingData) {
        return UUIDs.timeBased().toString();
    }
}
