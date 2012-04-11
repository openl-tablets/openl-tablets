/*
 * Created on Jun 4, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.util;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author snshor
 *
 */
public interface IOpenIterator<T> extends Iterator<T> {
    public static final int UNKNOWN_SIZE = -1;

    public IOpenIterator<T> append(IOpenIterator<T> it);

    public Iterator<T> append(Iterator<T> it);

    public List<T> asList();

    public Set<T> asSet();

    /**
     * Legacy (Smalltalk) name, same as convert
     *
     * @param col
     * @return
     */
    public <C> IOpenIterator<C> collect(IConvertor<T, C> col);

    /**
     * Same as collect
     *
     * @param col
     * @return
     */
    public <C> IOpenIterator<C> convert(IConvertor<T, C> col);

    /**
     * @return the number of elements in iterator, it is not a "const" method,
     *         performs it by actual enumeration
     */

    public int count();

    public void evaluate(IBlock block);

    public <E> IOpenIterator<E> extend(IOpenIteratorExtender<E, T> mod);

    public IOpenIterator<T> reverse() throws UnsupportedOperationException;

    public IOpenIterator<T> select(ISelector<T> sel);

    /**
     * @return the number of elements left to iterate, or UNKNOWN_SIZE if it is
     *         not known, this method is "const"
     */

    public int size();

    public int skip(int n);

    public IOpenIterator<T> sort(Comparator<T> cmp);

}
