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

    boolean isSync(StoreLogData storeLogData);

    void save(StoreLogData storeLogData) throws StoreLogDataException;

    default Collection<Inject<?>> additionalInjects() {
        return Collections.emptyList();
    }

}
