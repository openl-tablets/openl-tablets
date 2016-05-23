/*
 * Created on May 18, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * @author snshor
 */

public abstract class AOpenIterator<T> implements IOpenIterator<T> {

    static final class CollectIterator<T, C> extends IteratorWrapper<T, C> {
        IConvertor<T, C> collector;

        CollectIterator(Iterator<T> it, IConvertor<T, C> convertor) {
            super(it);
            this.collector = convertor;
        }

        @Override
        public C next() {
            return collector.convert(it.next());
        }
    }

    static final class EmptyIterator<T> extends AOpenIterator<T> {
        @Override
        @SuppressWarnings("unchecked")
        public <C> IOpenIterator<C> collect(IConvertor<T, C> ic) {
            return (IOpenIterator<C>) this;
        }

        public boolean hasNext() {
            return false;
        }

        public T next() {
            throw new NoSuchElementException("EmptyIterator");
        }

        @Override
        public IOpenIterator<T> reverse() {
            return this;
        }

        @Override
        public int size() {
            return 0;
        }

    }

    static abstract class IteratorWrapper<T, C> extends AOpenIterator<C> {
        protected Iterator<T> it;

        IteratorWrapper(Iterator<T> it) {
            this.it = it;
        }

        public boolean hasNext() {
            return it.hasNext();
        }

        public abstract C next();
    }

    static final class SelectIterator<T> extends IteratorWrapper<T, T> {
        ISelector<T> selector;
        T next;
        boolean hasNext = false;

        SelectIterator(Iterator<T> it, ISelector<T> selector) {
            super(it);
            this.selector = selector;
        }

        void findNext() {
            while (it.hasNext()) {
                T obj = it.next();
                if (selector.select(obj)) {
                    next = obj;
                    hasNext = true;
                    return;
                }
            }

            next = null;
            hasNext = false;
        }

        @Override
        public boolean hasNext() {
            if (!hasNext) {
                findNext();
            }
            return hasNext;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new IllegalStateException();
            }
            hasNext = false;
            return next;
        }

    }

    static class SimpleIteratorWrapper<T> extends IteratorWrapper<T, T> {
        SimpleIteratorWrapper(Iterator<T> it) {
            super(it);
        }

        @Override
        public T next() {
            return it.next();
        }

    }

    /**
     * Iterates over single object exactly one time.
     */
    static final class SingleIterator<T> extends AOpenIterator<T> {
        T value;

        SingleIterator(T value) {
            this.value = value;
        }

        public boolean hasNext() {
            return value != null;
        }

        public T next() {
            if (value == null) {
                throw new NoSuchElementException();
            }
            T tmp = value;
            value = null;
            return tmp;
        }

        @Override
        public IOpenIterator<T> reverse() {
            if (size() == 0) {
                return empty();
            }
            return this;
        }

        @Override
        public final int size() {
            return value == null ? 0 : 1;
        }

    }

    public static final EmptyIterator<?> EMPTY = new EmptyIterator<Object>();

    public static <T> List<T> asList(Iterator<T> it) {
        int size = size(it);
        List<T> result = null;

        switch (size) {
            case 0:
                return Collections.emptyList();
            case UNKNOWN_SIZE:
                result = new ArrayList<T>();
                break;
            default:
                result = new ArrayList<T>(size);
        }

        for (; it.hasNext();) {
            result.add(it.next());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static <T> IOpenIterator<T> empty() {
        return (IOpenIterator<T>) EMPTY;
    }

    public static boolean isEmpty(Iterator<?> it) {
        return it == null || it == EMPTY;
    }

    static public <X> IOpenIterator<X> reverse(Iterator<X> it) {
        if (it instanceof IOpenIterator<?>) {
            return ((IOpenIterator<X>) it).reverse();
        }
        throw new UnsupportedOperationException();
    }

    static public <X> IOpenIterator<X> select(Iterator<X> it, ISelector<X> is) {
        return new SelectIterator<X>(it, is);
    }

    /**
     * 
     * @param value
     * @return iterator over single value, if value != null, empty iterator
     *         otherwise
     */
    public static <T> IOpenIterator<T> single(T value) {
        if (value == null) {
            return empty();
        }
        return new SingleIterator<T>(value);
    }

    public static <T> int size(Iterator<T> it) {
        if (it instanceof IOpenIterator<?>) {
            return ((IOpenIterator<T>) it).size();
        }
        return UNKNOWN_SIZE;
    }

    public List<T> asList() {
        return asList(this);
    }

    public <C> IOpenIterator<C> collect(IConvertor<T, C> ic) {
        return new CollectIterator<T, C>(this, ic);
    }

    // ////////////////////////////// Some useful OpenIterators
    // ///////////////////////////////////////////

    /**
     * Calculates the number of iterated elements. Unfortunately, destroys the
     * iterator
     * 
     * @see #size
     * @return
     */

    public int count() {
        int cnt = 0;
        for (; hasNext(); ++cnt) {
            next();
        }
        return cnt;
    }

    public void remove() {
        throw new IllegalStateException();
    }

    /**
     * Returns reverse iterator ri such as last(this) == first(ri), last-1(this)
     * == first+1(ri), this.size() = ri.size(), this.count() = ri.count() etc.
     * 
     * @return
     */

    public IOpenIterator<T> reverse() {
        throw new UnsupportedOperationException();
    }

    public IOpenIterator<T> select(ISelector<T> is) {
        return new SelectIterator<T>(this, is);
    }

    /**
     * Calculates the remaining size of iterated collection without destroying
     * itself(const in c++ terminology), -1 if it can not be known in advance.
     * Not every iterator is capable of doing it.
     * 
     * @see count
     */

    public int size() {
        return UNKNOWN_SIZE;
    }
}
