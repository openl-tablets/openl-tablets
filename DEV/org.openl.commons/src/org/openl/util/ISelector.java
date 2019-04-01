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
public interface ISelector<T> {

    ISelector<T> and(ISelector<T> isel);

    ISelector<T> not();

    ISelector<T> or(ISelector<T> isel);

    boolean select(T obj);

    ISelector<T> xor(ISelector<T> isel);
}
