package org.openl.source;

import java.util.SortedMap;

/**
 * @author Andrei Astrouski
 * 
 * TODO Refactor - add Source object to encapsulate source data 
 */
public interface SourceHistoryManager<T> {

    void save(T source);

    boolean revert(long version);

    T get(long version);

    SortedMap<Long, T> get(long... versions);

    SortedMap<Long, T> get(String... names);

    SortedMap<Long, T> getAll();

}
