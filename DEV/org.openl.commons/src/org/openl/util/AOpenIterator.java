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

    static final class EmptyIterator<T> extends AOpenIterator<T> {

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public T next() {
            throw new NoSuchElementException("EmptyIterator");
        }

        @Override
        public int size() {
            return 0;
        }

    }

    abstract static class IteratorWrapper<T, C> extends AOpenIterator<C> {
        protected final Iterator<T> it;

        IteratorWrapper(Iterator<T> it) {
            this.it = it;
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public abstract C next();
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

    public static final EmptyIterator<?> EMPTY_ITERATOR = new EmptyIterator<>();

    @SuppressWarnings("unchecked")
    public static <T> IOpenIterator<T> empty() {
        return (IOpenIterator<T>) EMPTY_ITERATOR;
    }

    // ////////////////////////////// Some useful OpenIterators
    // ///////////////////////////////////////////

    @Override
    public void remove() {
        throw new IllegalStateException();
    }

    /**
     * Calculates the remaining size of iterated collection without destroying itself(const in c++ terminology), -1 if
     * it cannot be known in advance. Not every iterator is capable of doing it.
     *
     * @see count
     */

    @Override
    public int size() {
        return UNKNOWN_SIZE;
    }
}
