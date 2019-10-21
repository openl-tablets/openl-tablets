/*
 * Created on Apr 30, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.domain;

import java.util.HashMap;
import java.util.Map;

/**
 * @author snshor
 */
public class Enum<T> {

    T[] allObjects;

    public Map<T, Integer> indexMap;

    public Enum(T[] objs) {
        this.allObjects = objs;
        indexMap = new HashMap<>(objs.length);

        for (int i = 0; i < objs.length; i++) {
            indexMap.put(objs[i], Integer.valueOf(i));
        }
    }

    public boolean contains(Object obj) {
        return indexMap.containsKey(obj);
    }

    public T[] getAllObjects() {
        return allObjects;
    }

    /**
     *
     * @param obj
     * @return
     * @throws RuntimeException if object is outside of a valid domain.
     */
    public int getIndex(T obj) {
        Integer idx = indexMap.get(obj);
        if (idx == null) {
            throw new RuntimeException(String.format("Object '%s' is outside of a valid domain.", obj));
        }
        return idx.intValue();
    }

    /**
     *
     */
    public int size() {
        return allObjects.length;
    }

}
