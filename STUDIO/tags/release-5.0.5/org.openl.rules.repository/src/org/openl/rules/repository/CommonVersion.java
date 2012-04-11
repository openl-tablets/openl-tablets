package org.openl.rules.repository;

public interface CommonVersion {
    int getMajor();
    int getMinor();
    int getRevision();
    
    String getVersionName();
}
