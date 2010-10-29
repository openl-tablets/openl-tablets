/*
 * Created on Apr 29, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.domain;

import java.util.Iterator;

import org.openl.util.AOpenIterator;

/**
 * @author snshor
 */
public interface ISetDomain<T> {

    class EmptyDomain<T> implements ISetDomain<T> {

        public ISetDomain<T> and(ISetDomain<T> sd) {
            return this;
        }

        public boolean contains(T obj) {
            return false;
        }

        public Iterator<T> iterator() {
            return AOpenIterator.empty();
        }

        public ISetDomain<T> or(ISetDomain<T> sd) {
            return sd;
        }

        public int size() {
            return 0;
        }

        public ISetDomain<T> sub(ISetDomain<T> sd) {
            return this;
        }

    }

    ISetDomain<Object> EMPTY_DOMAIN = new EmptyDomain<Object>();

    ISetDomain<T> and(ISetDomain<T> sd);

    boolean contains(T obj);

    Iterator<T> iterator();

    ISetDomain<T> or(ISetDomain<T> sd);

    int size();

    ISetDomain<T> sub(ISetDomain<T> sd);

}
