package org.openl.rules.ruleservice.logging;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SimpleStoreLoggingManager implements StoreLoggingManager {
    private final Logger log = LoggerFactory.getLogger(SimpleStoreLoggingManager.class);

    private Collection<StoreLoggingService> storeLoggingServices;

    public SimpleStoreLoggingManager(Collection<StoreLoggingService> storeLoggingServices) {
        Objects.requireNonNull(storeLoggingServices);
        this.storeLoggingServices = new ArrayList<>(storeLoggingServices);
    }

    @Override
    public void save(StoreLoggingData storeLoggingData) {
        for (StoreLoggingService storeLoggingService : storeLoggingServices) {
            try {
                storeLoggingService.save(storeLoggingData);
            } catch (Exception e) {
                log.error("Failed on save operation.", e);
            }
        }
    }
}
