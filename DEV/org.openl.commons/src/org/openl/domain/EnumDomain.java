/*
 * Created on Apr 30, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.domain;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;

/**
 * @author snshor
 */
public class EnumDomain<T> implements IDomain<T> {

    private final LinkedHashSet<T> index;
    private final Class<?> componentType;

    public EnumDomain(T[] elements) {
        componentType = elements == null ? Object.class : elements.getClass().getComponentType();
        index = new LinkedHashSet<>(elements == null ? Collections.emptyList() : Arrays.asList(elements));
    }

    public boolean contains(T obj) {
        return index.contains(obj);
    }

    @Override
    public IType getElementType() {
        return null;
    }

    public T[] getAllObjects() {
        return index.toArray((T[]) Array.newInstance(componentType, 0));
    }

    @Override
    public Iterator<T> iterator() {
        return Collections.unmodifiableSet(index).iterator();
    }

    @Override
    public boolean selectObject(T obj) {
        return contains(obj);
    }

    public int size() {
        return index.size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean f = false;
        for (Object o : index) {
            if (f) {
                sb.append(",");
            } else {
                f = true;
            }
            sb.append(o.toString());
        }
        return "[" + sb + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        EnumDomain<?> that = (EnumDomain<?>) o;
        if (componentType.equals(that.componentType)) {
            return index.equals(that.index);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = index.hashCode();
        result = 31 * result + componentType.hashCode();
        return result;
    }
}
