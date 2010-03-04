/*
 * Created on May 20, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.util;

/**
 * @author snshor
 */

public interface IAppender<T> {
    /**
     *
     * @param obj Object to add
     * @return false if there is no more room left
     */
    public boolean add(T obj);
}
