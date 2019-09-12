package org.openl.rules.ruleservice.logging.elasticsearch;

import java.util.UUID;

import org.openl.rules.ruleservice.logging.StoreLoggingData;
import org.openl.rules.ruleservice.logging.StoreLoggingConvertor;

public class RandomUUID implements StoreLoggingConvertor<String> {
    @Override
    public String convert(StoreLoggingData storeLoggingData) {
        return UUID.randomUUID().toString();
    }
}
