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
     * @return the number of elements left to iterate, or UNKNOWN_SIZE if it is not known, this method is "const"
     */
    int size();
}
