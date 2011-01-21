/*
 * Created on May 21, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.util;

import java.util.Collection;

/**
 * @author snshor
 *
 */
public class Appender<T> implements IAppender<T> {

    static class ArrayAppender<T> implements IAppender<T> {
        T[] ary;
        int from;
        int to;

        ArrayAppender(T[] ary) {
            this(ary, 0, ary.length);
        }

        ArrayAppender(T[] ary, int from, int to) {
            this.ary = ary;
            this.from = from;
            this.to = to;
        }

        public boolean add(T obj) {
            if (from >= to) {
                return false;
            }
            ary[from++] = obj;
            return true;
        }

    }

    static class CollectionAppender<T> implements IAppender<T> {
        Collection<T> collection;

        CollectionAppender(Collection<T> collection) {
            this.collection = collection;
        }

        public boolean add(T obj) {
            collection.add(obj);
            return true;
        }

    }

    public static final IAppender<?> NUL = new Appender<Object>();

    /**
     * Stores data in array
     *
     * @param ary
     * @return
     */
    public static <T> IAppender<T> toArray(T[] ary) {
        return new ArrayAppender<T>(ary);
    }

    /**
     * Stores data in array within selected range
     *
     * @param ary
     * @param from
     * @param to
     * @return
     */
    public static <T> IAppender<T> toArray(T[] ary, int from, int to) {
        return new ArrayAppender<T>(ary, from, to);
    }

    public static <T> IAppender<T> toCollection(Collection<T> cc) {
        return new CollectionAppender<T>(cc);
    }

    /**
     * NUL implementation, will append everything
     */

    public boolean add(Object obj) {
        return true;
    }

}
