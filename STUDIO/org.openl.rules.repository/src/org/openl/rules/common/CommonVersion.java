package org.openl.rules.common;

public interface CommonVersion extends Comparable<CommonVersion> {
    final static int MAX_MM_INT = 32767;

    int getRevision();

    int getMajor();

    int getMinor();

    String getVersionName();
}
