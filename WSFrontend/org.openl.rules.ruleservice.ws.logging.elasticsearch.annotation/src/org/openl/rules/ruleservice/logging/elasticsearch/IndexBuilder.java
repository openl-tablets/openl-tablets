package org.openl.rules.ruleservice.logging.elasticsearch;

import org.openl.rules.ruleservice.logging.StoreLogData;

public interface IndexBuilder {
    Object withObject(StoreLogData storeLogData);

    String withSource(StoreLogData storeLogData);

    String withId(StoreLogData storeLogData);

    String withParentId(StoreLogData storeLogData);

    String withIndexName(StoreLogData storeLogData);

    String withType(StoreLogData storeLogData);

    Long withVersion(StoreLogData storeLogData);
}
