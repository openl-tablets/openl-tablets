/*
 * Created on May 18, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

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

    static public class MergeIterator<T> extends AOpenIterator<T> {
        Iterator<T>[] itt;
        int current = 0;

        @SuppressWarnings("unchecked")
        public MergeIterator(Iterator<T> it1, Iterator<T> it2) {
            this.itt = (Iterator<T>[]) Array.newInstance(Iterator.class, 2);
            itt[0] = it1;
            itt[1] = it2;
        }

        public MergeIterator(Iterator<T>[] itt) {
            this.itt = itt;
        }

        public boolean hasNext() {
            for (; current < itt.length; ++current) {
                if (itt[current].hasNext()) {
                    return true;
                }
            }

            return false;
        }

        public T next() {
            return itt[current].next();
        }

        @Override
        public void remove() {
            itt[current].remove();
        }

        @Override
        public int size() {

            int total = 0;
            for (int i = current; i < itt.length; i++) {
                int size = size(itt[i]);
                if (size == UNKNOWN_SIZE) {
                    return UNKNOWN_SIZE;
                }
                total += size;
            }

            return total;

        }
    }

    static class ModifierIterator<E, T> extends AOpenIterator<E> {
        Iterator<T> baseIterator;
        IOpenIteratorExtender<E, T> modifier;
        boolean hasNext = false;
        E next = null;
        Iterator<E> modifierIterator;

        ModifierIterator(Iterator<T> baseIterator, IOpenIteratorExtender<E, T> modifier) {
            this.baseIterator = baseIterator;
            this.modifier = modifier;
        }

        protected void findNextObject() {
            while (modifierIterator == null || !modifierIterator.hasNext()) {
                if (modifierIterator == null) {
                    modifierIterator = getNextIterator();
                    if (modifierIterator == null) {
                        return;
                    }
                }

                if (!modifierIterator.hasNext()) {
                    modifierIterator = null;
                }
            }

            next = modifierIterator.next();
            hasNext = true;
        }

        protected Iterator<E> getNextIterator() {
            while (baseIterator.hasNext()) {
                Iterator<E> it = modifier.extend(baseIterator.next());
                if (it != null) {
                    return it;
                }
            }
            return null;
        }

        public boolean hasNext() {
            if (hasNext) {
                return true;
            }
            findNextObject();
            return hasNext;
        }

        public E next() {
            if (!hasNext()) {
                throw new IllegalStateException();
            }
            hasNext = false;
            return next;
        }

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

    public static <T> IOpenIterator<T> asOpenIterator(Iterator<T> it) {
        if (it == null) {
            return empty();
        }

        if (it instanceof IOpenIterator<?>) {
            return (IOpenIterator<T>) it;
        }

        return new SimpleIteratorWrapper<T>(it);
    }

    public static <T> Set<T> asSet(Iterator<T> it) {
        int size = size(it);
        Set<T> result = null;

        switch (size) {
            case 0:
                return Collections.emptySet();
            case UNKNOWN_SIZE:
                result = new HashSet<T>();
                break;
            default:
                result = new HashSet<T>(size);
        }

        for (; it.hasNext();) {
            result.add(it.next());
        }
        return result;
    }

    static public <T, C> IOpenIterator<C> collect(Iterator<T> it, IConvertor<T, C> ic) {
        return new CollectIterator<T, C>(it, ic);
    }

    @SuppressWarnings("unchecked")
    public static <T> IOpenIterator<T> empty() {
        return (IOpenIterator<T>) EMPTY;
    }

    @SuppressWarnings("unchecked")
    public static <T> Iterator<T> emptyIterator() {
        return (Iterator<T>) EMPTY;
    }

    public static <X> void evaluate(IBlock block, Iterator<X> it) {
        while (it.hasNext()) {
            block.evaluate(it.next());
        }
    }

    static public <E, T> IOpenIterator<E> extend(Iterator<T> it, IOpenIteratorExtender<E, T> mod) {
        return new ModifierIterator<E, T>(it, mod);
    }

    public static boolean isEmpty(Iterator<?> it) {
        return it == null || it == EMPTY;
    }

    public static <T> IOpenIterator<T> merge(IOpenIterator<T> it1, IOpenIterator<T> it2) {
        if (isEmpty(it1)) {
            if (it2 == null) {
                return empty();
            }
            return it2;
        }

        if (isEmpty(it2)) {
            return it1;
        }

        return new MergeIterator<T>(it1, it2);
    }

    public static <T> Iterator<T> merge(Iterator<T> it1, Iterator<T> it2) {
        if (isEmpty(it1)) {
            if (it2 == null) {
                return emptyIterator();
            }
            return it2;
        }

        if (isEmpty(it2)) {
            return it1;
        }

        return new MergeIterator<T>(it1, it2);
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

    static public <T> int store(Iterator<T> it, IAppender<T> appender) {
        int cnt = 0;
        for (; it.hasNext() && appender.add(it.next()); ++cnt) {
            ;
        }
        return cnt;
    }

    public IOpenIterator<T> append(IOpenIterator<T> it) {
        return it == null || it == EMPTY ? this : merge(this, it);
    }

    public Iterator<T> append(Iterator<T> it) {
        return it == null || it == EMPTY ? this : merge(this, it);
    }

    public List<T> asList() {
        return asList(this);
    }

    public Set<T> asSet() {
        return asSet(this);
    }

    public <C> IOpenIterator<C> collect(IConvertor<T, C> ic) {
        return new CollectIterator<T, C>(this, ic);
    }

    // ////////////////////////////// Some useful OpenIterators
    // ///////////////////////////////////////////

    public <C> IOpenIterator<C> convert(IConvertor<T, C> ic) {
        return collect(ic);
    }

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

    public void evaluate(IBlock block) {
        evaluate(block, this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.openl.util.IOpenIterator#modify(org.openl.util.IOpenIteratorModifier)
     */
    public <E> IOpenIterator<E> extend(IOpenIteratorExtender<E, T> mod) {
        return extend(this, mod);
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

    /**
     * Skips up to n elements.
     * 
     * @param n
     * @return actual number of skipped elements
     */
    public int skip(int n) {
        int x = n;
        for (; n > 0 && hasNext(); n--) {
            next();
        }

        return x - n;

    }

    public int store(IAppender<T> appender) {
        return store(this, appender);
    }

}
