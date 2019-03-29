/*
 * Created on May 18, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.util;

import java.util.Iterator;
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

    abstract static class IteratorWrapper<T, C> extends AOpenIterator<C> {
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

    public static final EmptyIterator<?> EMPTY = new EmptyIterator<Object>();

    @SuppressWarnings("unchecked")
    public static <T> IOpenIterator<T> empty() {
        return (IOpenIterator<T>) EMPTY;
    }

    public static <X> IOpenIterator<X> reverse(Iterator<X> it) {
        if (it instanceof IOpenIterator<?>) {
            return ((IOpenIterator<X>) it).reverse();
        }
        throw new UnsupportedOperationException();
    }

    public static <X> IOpenIterator<X> select(Iterator<X> it, ISelector<X> is) {
        return new SelectIterator<X>(it, is);
    }

    public <C> IOpenIterator<C> collect(IConvertor<T, C> ic) {
        return new CollectIterator<T, C>(this, ic);
    }

    // ////////////////////////////// Some useful OpenIterators
    // ///////////////////////////////////////////

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
