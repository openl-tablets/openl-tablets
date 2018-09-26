package org.openl.rules.ui;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public class ObjectRegistry<T> {
    private IdentityHashMap<T, Integer> value2id = new IdentityHashMap<>();
    private Map<Integer, T> id2value = new HashMap<>();

    private int uniqueId = 0;

    public Integer getId(T value) {
        return value2id.get(value);
    }

    public T getValue(Integer id) {
        return id2value.get(id);
    }

    public Integer putIfAbsent(T value) {
        Integer id = getId(value);
        if (id != null) {
            return id;
        }

        id = ++uniqueId;
        value2id.put(value, id);
        id2value.put(id, value);
        return id;
    }
}
