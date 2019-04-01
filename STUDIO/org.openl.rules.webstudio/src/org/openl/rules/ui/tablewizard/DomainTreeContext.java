package org.openl.rules.ui.tablewizard;

import java.util.HashMap;
import java.util.Map;

/**
 * A context against which dot-path expressions are computed in <code>DomainTree</code>. E.g. if the context contains an
 * object <i>driver</i> of type <code>Driver</code>, a user can construct expressions like <code>driver.age</code>,
 * <code>driver.address.zipcode</code> provided the corresponding classes contain required properties.
 *
 * @author Aliaksandr Antonik.
 */
public class DomainTreeContext {
    /**
     * Contains object names as keys, and their types as values.
     */
    private Map<String, String> rootObjects = new HashMap<>();

    public boolean containsObject(String name) {
        return rootObjects.containsKey(name);
    }

    public String getObjectType(String name) {
        return rootObjects.get(name);
    }

    public void setObjectType(String name, String typename) {
        rootObjects.put(name, typename);
    }
}
