package org.openl.rules.ruleservice.storelogdata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import org.openl.rules.ruleservice.storelogdata.annotation.AnnotationUtils;
import org.openl.rules.ruleservice.storelogdata.annotation.SkipFault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SimpleStoreLogDataManager implements StoreLogDataManager {
    private final Logger log = LoggerFactory.getLogger(SimpleStoreLogDataManager.class);

    private final Collection<StoreLogDataService> storeLogDataServices;

    private final boolean enabled;

    public SimpleStoreLogDataManager(Collection<StoreLogDataService> storeLogDataServices) {
        Objects.requireNonNull(storeLogDataServices);
        this.storeLogDataServices = new ArrayList<>(storeLogDataServices);
        this.enabled = !storeLogDataServices.isEmpty();
    }

    public Collection<StoreLogDataService> getServices() {
        return storeLogDataServices;
    }

    @Override
    public void store(StoreLogData storeLogData) {
        if (!storeLogData.isIgnorable()) {
            if (!ignoreByFault(storeLogData)) {
                for (StoreLogDataService storeLogDataService : storeLogDataServices) {
                    try {
                        storeLogDataService.save(storeLogData);
                    } catch (Exception e) {
                        log.error("Failed on save operation.", e);
                    }
                }
            }
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    private boolean ignoreByFault(StoreLogData storeLogData) {
        if (storeLogData.isFault()) {
            return AnnotationUtils.getAnnotationInServiceClassOrServiceMethod(storeLogData, SkipFault.class) != null;
        }
        return false;
    }
}
