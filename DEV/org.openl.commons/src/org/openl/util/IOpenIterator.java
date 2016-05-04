/*
 * Created on Jun 4, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.util;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author snshor
 * 
 */
public interface IOpenIterator<T> extends Iterator<T> {

    int UNKNOWN_SIZE = -1;

    IOpenIterator<T> append(IOpenIterator<T> it);

    Iterator<T> append(Iterator<T> it);

    List<T> asList();

    Set<T> asSet();

    /**
     * Legacy (Smalltalk) name, same as convert
     * 
     * @param col
     * @return
     */
    <C> IOpenIterator<C> collect(IConvertor<T, C> col);

    /**
     * Same as collect
     * 
     * @param col
     * @return
     */
    <C> IOpenIterator<C> convert(IConvertor<T, C> col);

    /**
     * @return the number of elements in iterator, it is not a "const" method,
     *         performs it by actual enumeration
     */
    int count();

    void evaluate(IBlock block);

    <E> IOpenIterator<E> extend(IOpenIteratorExtender<E, T> mod);

    IOpenIterator<T> reverse() throws UnsupportedOperationException;

    IOpenIterator<T> select(ISelector<T> sel);

    /**
     * @return the number of elements left to iterate, or UNKNOWN_SIZE if it is
     *         not known, this method is "const"
     */
    int size();

    int skip(int n);
}
