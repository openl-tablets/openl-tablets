/*
 * Created on May 20, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.util;

/**
 * @author snshor
 *
 */
public interface IConvertor<T, C> {
    static class SameAs<X> implements IConvertor<X, X> {
        public X convert(X obj) {
            return obj;
        }
    }

    static final public IConvertor<Object, Object> SAME_AS = new SameAs<Object>();

    public C convert(T obj);

}
