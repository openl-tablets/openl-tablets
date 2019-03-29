/*
 * Created on Jun 4, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.util;

import java.util.Iterator;

/**
 * @author snshor
 * 
 */
public interface IOpenIterator<T> extends Iterator<T> {

    int UNKNOWN_SIZE = -1;

    /**
     * Legacy (Smalltalk) name, same as convert
     * 
     * @param col
     * @return
     */
    <C> IOpenIterator<C> collect(IConvertor<T, C> col);

    /**
     * @return the number of elements in iterator, it is not a "const" method,
     *         performs it by actual enumeration
     */
    IOpenIterator<T> reverse() throws UnsupportedOperationException;

    IOpenIterator<T> select(ISelector<T> sel);

    /**
     * @return the number of elements left to iterate, or UNKNOWN_SIZE if it is
     *         not known, this method is "const"
     */
    int size();
}
