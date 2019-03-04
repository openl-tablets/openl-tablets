package org.openl.rules.common;

import java.io.Serializable;
import java.util.Map;

public interface ProjectVersion extends Serializable, CommonVersion {
    String getVersionComment();

    boolean isDeleted();

    VersionInfo getVersionInfo();
    
    Map<String, Object> getVersionProperties();
}
