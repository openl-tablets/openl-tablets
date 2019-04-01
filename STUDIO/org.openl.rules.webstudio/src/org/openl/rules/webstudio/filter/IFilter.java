package org.openl.rules.webstudio.filter;

import org.openl.util.ISelector;

/**
 * Base interface for filters.
 */
public interface IFilter<T> extends ISelector<T> {
    /**
     * If given class is supported by this filter.
     *
     * @param aClass a class to check.
     * @return if <code>aClass</code> is supported.
     */
    boolean supports(Class<?> aClass);
}