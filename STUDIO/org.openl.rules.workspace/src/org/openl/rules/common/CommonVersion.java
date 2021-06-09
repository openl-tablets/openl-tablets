package org.openl.rules.common;

public interface CommonVersion extends Comparable<CommonVersion> {
    int MAX_MM_INT = 32767;

    String getRevision();

    int getMajor();

    int getMinor();

    String getVersionName();

    String getShortVersion();
}
