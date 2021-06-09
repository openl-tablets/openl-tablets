/*
 * Created on Apr 30, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.domain;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author snshor
 */
public class EnumDomain<T> implements IDomain<T> {

    private final T[] elements;
    private final Map<T, Integer> indexMap;

    public EnumDomain(T[] elements) {
        this.elements = elements;
        int i = 0;
        indexMap = new HashMap<>(elements.length);
        for (T el : elements) {
            indexMap.put(el, i++);
        }
    }

    public boolean contains(T obj) {
        return indexMap.containsKey(obj);
    }

    @Override
    public IType getElementType() {
        return null;
    }

    public T[] getAllObjects() {
        return elements;
    }

    @Override
    public Iterator<T> iterator() {
        return Arrays.asList(elements).iterator();
    }

    @Override
    public boolean selectObject(T obj) {
        return contains(obj);
    }

    public int size() {
        return elements.length;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean f = false;
        for (Object o : elements) {
            if (f) {
                sb.append(",");
            } else {
                f = true;
            }
            sb.append(o.toString());
        }
        return "[" + sb.toString() + "]";
    }

}
