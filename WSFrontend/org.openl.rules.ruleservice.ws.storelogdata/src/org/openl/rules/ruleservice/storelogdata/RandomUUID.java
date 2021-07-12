package org.openl.rules.ruleservice.storelogdata;

import java.util.UUID;

public final class RandomUUID implements StoreLogDataConverter<String> {
    @Override
    public String apply(StoreLogData value) {
        return UUID.randomUUID().toString();
    }
}
