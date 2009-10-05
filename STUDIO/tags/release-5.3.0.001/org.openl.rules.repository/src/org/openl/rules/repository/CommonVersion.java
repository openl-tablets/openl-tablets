package org.openl.rules.repository;

public interface CommonVersion extends Comparable<CommonVersion> {
    int getMajor();

    int getMinor();

    int getRevision();

    String getVersionName();
}
