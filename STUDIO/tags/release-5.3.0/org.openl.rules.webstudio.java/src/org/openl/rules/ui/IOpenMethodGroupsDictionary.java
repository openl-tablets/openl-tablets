package org.openl.rules.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.types.IOpenMethod;
import org.openl.types.impl.AOpenClass.MethodKey;

public class IOpenMethodGroupsDictionary {

    private Map<MethodKey, List<IOpenMethod>> internalMap = new HashMap<MethodKey, List<IOpenMethod>>();

    public boolean contains(IOpenMethod method) {

        MethodKey key = buildKey(method);

        return contains(key);
    }

    public void add(IOpenMethod method) {

        MethodKey key = buildKey(method);

        if (contains(key)) {
            List<IOpenMethod > value = internalMap.get(key);
            value.add(method);
        } else {
            List<IOpenMethod > value = new ArrayList<IOpenMethod >();
            value.add(method);

            internalMap.put(key, value);
        }
    }
    
    public void addAll(IOpenMethod[] methods) {
        
        for (IOpenMethod method : methods) {
            add(method);
        }
    }
    
    public List<IOpenMethod> getGroup(IOpenMethod method) {
        MethodKey key = buildKey(method);
        
        return internalMap.get(key);
    }

    private boolean contains(MethodKey key) {
        return internalMap.containsKey(key);
    }

    private MethodKey buildKey(IOpenMethod method) {
        return new MethodKey(method);
    }
}