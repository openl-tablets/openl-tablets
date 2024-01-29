package org.openl.rules.ruleservice.storelogdata.cassandra;

import com.datastax.oss.driver.api.core.uuid.Uuids;

import org.openl.rules.ruleservice.storelogdata.StoreLogData;
import org.openl.rules.ruleservice.storelogdata.StoreLogDataConverter;

public final class TimeBasedUUID implements StoreLogDataConverter<String> {
    @Override
    public String apply(StoreLogData value) {
        return Uuids.timeBased().toString();
    }

}
