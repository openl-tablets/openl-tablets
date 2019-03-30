/*
 * Created on May 3, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.domain;

import org.openl.util.IOpenIterator;

/**
 * @author snshor
 */
public interface IIntIterator extends IOpenIterator<Integer> {
    int nextInt();

    IIntIterator select(IIntSelector selector);

    /**
     * 
     * @return true if iterator can be reset for repeated use
     * @see #reset()
     */

    boolean isResetable();

    /**
     * resets iterator to it's initial state
     * 
     * @see #isResetable()
     */
    void reset();

}
