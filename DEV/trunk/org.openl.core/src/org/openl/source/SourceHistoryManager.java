package org.openl.source;

/**
 * @author Andrei Astrouski
 */
public interface SourceHistoryManager<T> {

    void save(T source);

    long[] getVersions();

    boolean revert(long version);

}
