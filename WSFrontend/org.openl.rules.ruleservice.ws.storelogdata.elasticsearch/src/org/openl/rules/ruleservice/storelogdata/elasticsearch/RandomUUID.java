package org.openl.rules.ruleservice.storelogdata.elasticsearch;

import java.util.UUID;

import org.openl.rules.ruleservice.storelogdata.StoreLogData;
import org.openl.rules.ruleservice.storelogdata.StoreLogDataConverter;

public class RandomUUID implements StoreLogDataConverter<String> {
    @Override
    public String convert(StoreLogData storeLogData) {
        return UUID.randomUUID().toString();
    }
}
