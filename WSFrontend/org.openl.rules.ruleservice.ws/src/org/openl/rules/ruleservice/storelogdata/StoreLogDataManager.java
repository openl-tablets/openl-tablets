package org.openl.rules.ruleservice.storelogdata;

import java.util.Collection;

public interface StoreLogDataManager {
    void store(StoreLogData storeLogData);

    Collection<StoreLogDataService> getServices();

    boolean isEnabled();
}
