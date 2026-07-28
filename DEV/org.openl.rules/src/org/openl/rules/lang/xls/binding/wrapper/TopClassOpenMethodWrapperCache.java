package org.openl.rules.lang.xls.binding.wrapper;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

import lombok.RequiredArgsConstructor;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

@RequiredArgsConstructor
class TopClassOpenMethodWrapperCache {

    private final IRulesMethodWrapper methodWrapper;

    final Map<IOpenClass, WeakReference<IOpenMethod>> cache = new WeakHashMap<>();

    void put(IOpenClass openClass, IOpenMethod openMethod) {
        cache.put(openClass, new WeakReference<>(openMethod));
    }

    IOpenMethod get(IOpenClass openClass) {
        WeakReference<IOpenMethod> w = cache.get(openClass);
        if (w != null) {
            return w.get();
        }
        return null;
    }

    public IOpenMethod getTopOpenClassMethod(IOpenClass topOpenClass) {
        var openMethod = get(topOpenClass);
        if (openMethod == null) {
            openMethod = topOpenClass.getMethod(methodWrapper.getDelegate().getName(),
                    methodWrapper.getDelegate().getSignature().getParameterTypes());
            put(topOpenClass, openMethod);
        }
        return openMethod;
    }
}
