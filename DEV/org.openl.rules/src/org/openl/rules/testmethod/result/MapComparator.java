package org.openl.rules.testmethod.result;

import java.util.Map;

/**
 * @author Yury Molchan
 */
class MapComparator extends GenericComparator<Map<?, ?>> {

    @Override
    boolean isEmpty(Map<?, ?> object) {
        return object.isEmpty();
    }
}
