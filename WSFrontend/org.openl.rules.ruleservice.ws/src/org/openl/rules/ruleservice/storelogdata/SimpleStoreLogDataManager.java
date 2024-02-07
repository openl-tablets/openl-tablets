package org.openl.rules.ruleservice.storelogdata;

import java.util.Collection;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.rules.ruleservice.storelogdata.annotation.AnnotationUtils;
import org.openl.rules.ruleservice.storelogdata.annotation.SkipFault;

public final class SimpleStoreLogDataManager implements StoreLogDataManager {

    private final Logger log = LoggerFactory.getLogger(SimpleStoreLogDataManager.class);
    private final Collection<StoreLogDataService> storeLogDataServices;

    private final boolean enabled;

    public SimpleStoreLogDataManager(Collection<StoreLogDataService> storeLogDataServices) {
        Objects.requireNonNull(storeLogDataServices);
        this.storeLogDataServices = storeLogDataServices;
        this.enabled = !storeLogDataServices.isEmpty();
        storeLogDataServices.stream()
                .map(x -> x.getClass().getTypeName())
                .forEach(x -> log.info("Store log data service '{}' is used.", x));
    }

    public Collection<StoreLogDataService> getServices() {
        return storeLogDataServices;
    }

    @Override
    public void store(StoreLogData storeLogData) throws StoreLogDataException {
        if (!storeLogData.isIgnorable()) {
            if (!ignoreByFault(storeLogData)) {
                for (StoreLogDataService storeLogDataService : storeLogDataServices) {
                    storeLogDataService.save(storeLogData);
                }
            }
        }
    }

    @Override
    public boolean isAtLeastOneSync(StoreLogData storeLogData) {
        for (StoreLogDataService storeLogDataService : storeLogDataServices) {
            if (storeLogDataService.isSync(storeLogData)) {
                return true;
            }
        }
        return false;
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
