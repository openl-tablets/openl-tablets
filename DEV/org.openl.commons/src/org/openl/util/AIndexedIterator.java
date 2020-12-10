/*
 * Created on May 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.util;

import java.lang.reflect.Array;
import java.util.Iterator;

/**
 * @author snshor
 */

public abstract class AIndexedIterator<T> extends AOpenIterator<T> {

    public static Iterator<Object> fromArrayObj(Object ary) {
        if (ary == null) {
            return empty();
        }

        return new AnyArrayIterator(ary);
    }

    static class AnyArrayIterator extends AIndexedIterator<Object> {
        final Object ary;

        AnyArrayIterator(Object ary) {
            super(0, Array.getLength(ary), 1);
            this.ary = ary;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.openl.util.AIndexedIterator#indexedElement(int)
         */
        @Override
        protected Object indexedElement(int i) {
            return Array.get(ary, i);
        }

    }

    final int from;
    int current;

    int step = 1;

    int to = -1;

    protected AIndexedIterator(int from, int to, int step) {
        this.from = from;
        this.to = to;
        this.step = step;
        this.current = from;
    }

    @Override
    public final boolean hasNext() {
        return step > 0 ? (current < to) : (current > to);
    }

    protected abstract T indexedElement(int i);

    @Override
    public T next() {
        int idx = current;
        current += step;
        return indexedElement(idx);
    }

    @Override
    public final int size() {
        return step > 0 ? ((to - from) / step) : ((from - to) / -step);
    }

}
