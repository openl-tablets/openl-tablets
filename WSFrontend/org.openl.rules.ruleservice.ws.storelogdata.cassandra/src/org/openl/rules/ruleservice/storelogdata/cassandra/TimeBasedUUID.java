package org.openl.rules.ruleservice.storelogdata.cassandra;

import org.openl.rules.ruleservice.storelogdata.StoreLogData;
import org.openl.rules.ruleservice.storelogdata.StoreLogDataConverter;

import com.datastax.oss.driver.api.core.uuid.Uuids;

public class TimeBasedUUID implements StoreLogDataConverter<String> {
    @Override
    public String convert(StoreLogData storeLogData) {
        return Uuids.timeBased().toString();
    }
}
