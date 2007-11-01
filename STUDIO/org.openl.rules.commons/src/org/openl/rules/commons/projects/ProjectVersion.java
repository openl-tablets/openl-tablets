package org.openl.rules.commons.projects;

public interface ProjectVersion {
    int getMajor();
    int getMinor();
    int getRevision();
    
    String getVersionName();

    VersionInfo getVersionInfo();
}
