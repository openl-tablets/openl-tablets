package org.openl.rules.ruleservice.logging.elasticsearch;

import java.util.UUID;

import org.openl.rules.ruleservice.logging.StoreLogData;
import org.openl.rules.ruleservice.logging.StoreLogDataConvertor;

public class RandomUUID implements StoreLogDataConvertor<String> {
    @Override
    public String convert(StoreLogData storeLogData) {
        return UUID.randomUUID().toString();
    }
}
