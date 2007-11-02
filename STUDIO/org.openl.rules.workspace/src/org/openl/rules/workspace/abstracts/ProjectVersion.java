package org.openl.rules.workspace.abstracts;

public interface ProjectVersion {
    int getMajor();
    int getMinor();
    int getRevision();
    
    String getVersionName();

    VersionInfo getVersionInfo();
}
