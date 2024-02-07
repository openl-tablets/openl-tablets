/*
 * Created on Apr 28, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.domain;

/**
 * @author snshor
 */
public interface IIntDomain {
    boolean contains(int value);

    int getMax();

    int getMin();

    IIntIterator intIterator();

    int size();

}
