/*
 * Created on May 6, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.util;

import java.util.Collection;

/**
 * @author snshor
 */
public interface IOpenCollection<T> extends Collection<T> {
    public IOpenIterator<T> openIterator();

}
