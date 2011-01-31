package org.openl.rules.ui;

/**
 * @author Andrei Astrouski
 */
public interface ProjectHistoryManager {

    void save(String tableUri);

    long[] getVersions();

    boolean revert(long version);

}
