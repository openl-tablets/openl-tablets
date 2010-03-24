package org.openl.rules.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.types.IOpenMethod;
import org.openl.types.impl.AOpenClass.MethodKey;

/**
 * Dictionary of IOpenMethod instances. Categorizes methods using their
 * signatures.
 */
public class OpenMethodGroupsDictionary {

    /**
     * Internal map of groups.
     */
    private Map<MethodKey, List<IOpenMethod>> internalMap = new HashMap<MethodKey, List<IOpenMethod>>();

    /**
     * Checks that method already in dictionary.
     * 
     * @param method IOpenMethod instance
     * @return <code>true</code> if method already exists in dictionary;
     *         <code>false</code> - otherwise
     */
    public boolean contains(IOpenMethod method) {

        MethodKey key = buildKey(method);

        return contains(key);
    }

    /**
     * Adds IOpenMethod instance to dictionary. If method(s) with same signature
     * already exists in dictionary new one will be added to its group;
     * otherwise - new entry will be created.
     * 
     * @param method IOpenMethod instance
     */
    public void add(IOpenMethod method) {

        MethodKey key = buildKey(method);

        if (contains(key)) {
            List<IOpenMethod> value = internalMap.get(key);
            value.add(method);
        } else {
            List<IOpenMethod> value = new ArrayList<IOpenMethod>();
            value.add(method);

            internalMap.put(key, value);
        }
    }

    /**
     * Adds all methods from array to dictionary.
     * 
     * @param methods array of methods
     */
    public void addAll(IOpenMethod[] methods) {

        for (IOpenMethod method : methods) {
            add(method);
        }
    }

    /**
     * Gets group of methods for passed IOpenMethod instance.
     * 
     * @param method IOpenMethod instance
     * @return group of methods
     */
    public List<IOpenMethod> getGroup(IOpenMethod method) {
        MethodKey key = buildKey(method);

        return internalMap.get(key);
    }

    /**
     * Checks that entry with passed key already exists.
     * 
     * @param key key
     * @return <code>true</code> if entry already exists; <code>false</code> -
     *         otherwise
     */
    private boolean contains(MethodKey key) {
        return internalMap.containsKey(key);
    }

    /**
     * Build key for IOpenMethod instance.
     * 
     * @param method IOpenMethod instance
     * @return builded key object
     */
    private MethodKey buildKey(IOpenMethod method) {
        return new MethodKey(method);
    }
}