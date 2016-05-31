/*
 * Created on Jun 4, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.util;

import java.util.Enumeration;
import java.util.Iterator;

/**
 * @author snshor
 * 
 */
public class OpenIterator<T> extends AOpenIterator.SimpleIteratorWrapper<T> {

    private static class EnumerationIterator<T> extends AOpenIterator<T> {
        private Enumeration<T> enumeration;

        EnumerationIterator(Enumeration<T> enumeration) {
            this.enumeration = enumeration;
        }

        public boolean hasNext() {
            return enumeration.hasMoreElements();
        }

        public T next() {
            return enumeration.nextElement();
        }

    }

    public static <T> Iterator<T> fromArray(T[] ary) {
        if (ary == null || ary.length == 0) {
            return empty();
        }

        return new AIndexedIterator.ArrayIterator<T>(ary);
    }

    public static Iterator<Object> fromArrayObj(Object ary) {
        if (ary == null) {
            return empty();
        }

        return new AIndexedIterator.AnyArrayIterator(ary);
    }

    public static <T> IOpenIterator<T> fromEnumeration(Enumeration<T> enumeration) {
        if (enumeration == null) {
            return empty();
        }

        return new EnumerationIterator<T>(enumeration);
    }

    public OpenIterator(Iterator<T> it) {
        super(it);
    }

}
