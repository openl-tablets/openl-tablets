package org.openl.rules.ruleservice.storelogdata;

import java.util.Collection;

public interface StoreLogDataManager {
    boolean isAtLeastOneSync(StoreLogData storeLogData);

    void store(StoreLogData storeLogData) throws StoreLogDataException;

    Collection<StoreLogDataService> getServices();

    boolean isEnabled();
}
