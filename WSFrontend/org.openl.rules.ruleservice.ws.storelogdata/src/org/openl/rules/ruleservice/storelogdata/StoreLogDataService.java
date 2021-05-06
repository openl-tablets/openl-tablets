package org.openl.rules.ruleservice.storelogdata;

/**
 * Interface for service that responsible for storing logging info into external resource.
 *
 * @author Marat Kamalov.
 *
 */
public interface StoreLogDataService {

    void save(StoreLogData storeLogData);

    default boolean isSync(StoreLogData storeLogData) {
        return false;
    }

    boolean isEnabled();
}
