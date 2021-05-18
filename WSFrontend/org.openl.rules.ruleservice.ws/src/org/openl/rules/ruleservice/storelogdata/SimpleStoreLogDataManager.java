package org.openl.rules.ruleservice.storelogdata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.openl.rules.ruleservice.storelogdata.annotation.AnnotationUtils;
import org.openl.rules.ruleservice.storelogdata.annotation.SkipFault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SimpleStoreLogDataManager implements StoreLogDataManager {
    private final Logger log = LoggerFactory.getLogger(SimpleStoreLogDataManager.class);

    private final Collection<StoreLogDataService> storeLogDataServices;

    public SimpleStoreLogDataManager(Collection<StoreLogDataService> storeLogDataServices) {
        Objects.requireNonNull(storeLogDataServices);
        this.storeLogDataServices = new ArrayList<>(storeLogDataServices);
    }

    ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public void store(StoreLogData storeLogData) {
        if (!storeLogData.isIgnorable()) {
            if (!ignoreByFault(storeLogData)) {
                for (StoreLogDataService storeLoggingService : storeLogDataServices) {
                    if (storeLoggingService.isSync(storeLogData)) {
                        save(storeLoggingService, storeLogData);
                    } else {
                        executorService.submit(() -> {
                            save(storeLoggingService, storeLogData);
                        });
                    }
                }
            }
        }
    }

    private void save(StoreLogDataService storeLogDataService, StoreLogData storeLogData) {
        try {
            storeLogDataService.save(storeLogData);
        } catch (Exception e) {
            log.error("Failed on save operation.", e);
        }
    }

    private boolean ignoreByFault(StoreLogData storeLogData) {
        if (storeLogData.isFault()) {
            return AnnotationUtils.getAnnotationInServiceClassOrServiceMethod(storeLogData, SkipFault.class) != null;
        }
        return false;
    }
}
