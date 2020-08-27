package org.openl.source;

import java.io.File;
import java.util.List;
import java.util.SortedMap;

/**
 * @author Andrei Astrouski
 *
 *         TODO Refactor - add Source object to encapsulate source data
 */
public interface SourceHistoryManager<T> {

    void save(T source);

    void restore(String version) throws Exception;

    T get(String version);

    void init(File sourceFile);

}
