package org.openl.rules.ruleservice.storelogdata;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class AbstractStoreLogDataService implements StoreLogDataService {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    protected abstract boolean isSync(StoreLogData storeLogData);

    protected abstract void save(StoreLogData storeLogData, boolean sync);

    @Override
    public final void save(StoreLogData storeLogData) {
        if (isSync(storeLogData)) {
            save(storeLogData, true);
        } else {
            executorService.submit(() -> {
                save(storeLogData, false);
            });
        }
    }
}
