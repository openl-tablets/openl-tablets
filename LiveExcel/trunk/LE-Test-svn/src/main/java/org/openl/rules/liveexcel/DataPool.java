package org.openl.rules.liveexcel;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Class stores objects associated with some UUID.
 * 
 * @author PUdalau
 */
public class DataPool {
    private Map<String, Object> data = new HashMap<String, Object>();
    public static final String SERVICE_OBJECT_UUID_FORMAT = "%s-%s";

    /**
     * @param newElement New object to add.
     * @return Generated UUID for this object.
     */
    public String add(Object newElement) {
        String id = generateUUID(newElement);
        data.put(id, newElement);
        return id;
    }

    private String generateUUID(Object object) {
        return String.format(SERVICE_OBJECT_UUID_FORMAT, object.getClass().getName(), UUID.randomUUID().toString());
    }

    /**
     * @param id UUID of previously added object;
     * @return Object associated with specified UUID;
     */
    public Object get(String id) {
        return data.get(id);
    }

    /**
     * Cleans up pool.
     */
    public void removeAll() {
        data.clear();
    }
}
