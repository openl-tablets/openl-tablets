package org.openl.rules.ruleservice.storelogdata;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractStoreLogDataService implements StoreLogDataService {
    private final Logger log = LoggerFactory.getLogger(AbstractStoreLogDataService.class);

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    protected abstract void save(StoreLogData storeLogData, boolean sync) throws StoreLogDataException;

    @Override
    public final void save(StoreLogData storeLogData) throws StoreLogDataException {
        if (isSync(storeLogData)) {
            save(storeLogData, true);
        } else {
            executorService.submit(() -> {
                try {
                    save(storeLogData, false);
                } catch (StoreLogDataException e) {
                    log.error("Failed on data store operation.", e);
                }
            });
        }
    }
}
