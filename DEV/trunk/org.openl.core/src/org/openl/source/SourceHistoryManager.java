package org.openl.source;

import java.util.Map;

/**
 * @author Andrei Astrouski
 */
public interface SourceHistoryManager<T> {

    void save(T source);

    boolean revert(long version);

    T get(long version);

    Map<Long, T> getAll();

}
