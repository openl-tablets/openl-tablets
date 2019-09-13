package org.openl.rules.ruleservice.logging.elasticsearch;

import java.util.UUID;

import org.openl.rules.ruleservice.logging.StoreLoggingData;
import org.openl.rules.ruleservice.logging.StoreLoggingDataConvertor;

public class RandomUUID implements StoreLoggingDataConvertor<String> {
    @Override
    public String convert(StoreLoggingData storeLoggingData) {
        return UUID.randomUUID().toString();
    }
}
