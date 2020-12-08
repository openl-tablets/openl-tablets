package org.openl.rules.webstudio.filter;

/**
 * Base interface for filters.
 */
public interface IFilter<T> {
    /**
     * If given class is supported by this filter.
     *
     * @param aClass a class to check.
     * @return if <code>aClass</code> is supported.
     */
    boolean supports(Class<?> aClass);

    boolean select(T obj);
}