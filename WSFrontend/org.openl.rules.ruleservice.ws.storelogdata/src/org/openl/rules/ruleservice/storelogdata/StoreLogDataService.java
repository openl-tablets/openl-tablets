package org.openl.rules.ruleservice.storelogdata;

import java.util.Collection;
import java.util.Collections;

/**
 * Interface for service that responsible for storing logging info into external resource.
 *
 * @author Marat Kamalov.
 *
 */
public interface StoreLogDataService {

    void save(StoreLogData storeLogData);

    default Collection<Inject<?>> additionalInjects() {
        return Collections.emptyList();
    }

    default boolean isSync(StoreLogData storeLogData) {
        return false;
    }

    boolean isEnabled();
}
