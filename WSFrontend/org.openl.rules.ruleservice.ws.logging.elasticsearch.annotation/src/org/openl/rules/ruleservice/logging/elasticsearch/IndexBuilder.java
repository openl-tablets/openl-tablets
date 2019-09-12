package org.openl.rules.ruleservice.logging.elasticsearch;

import org.openl.rules.ruleservice.logging.StoreLoggingData;

public interface IndexBuilder {
    Object withObject(StoreLoggingData storeLoggingData);

    String withSource(StoreLoggingData storeLoggingData);

    String withId(StoreLoggingData storeLoggingData);

    String withParentId(StoreLoggingData storeLoggingData);

    String withIndexName(StoreLoggingData storeLoggingData);

    String withType(StoreLoggingData storeLoggingData);

    Long withVersion(StoreLoggingData storeLoggingData);
}
