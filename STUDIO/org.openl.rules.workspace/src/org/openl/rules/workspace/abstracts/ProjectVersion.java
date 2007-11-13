package org.openl.rules.workspace.abstracts;

import java.io.Serializable;

public interface ProjectVersion extends Serializable, Comparable<ProjectVersion> {
    int getMajor();
    int getMinor();
    int getRevision();
    
    String getVersionName();

    VersionInfo getVersionInfo();
}
