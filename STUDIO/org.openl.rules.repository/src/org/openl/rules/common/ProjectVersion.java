package org.openl.rules.common;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public interface ProjectVersion extends Serializable, CommonVersion {

    VersionInfo getVersionInfo();
    
    public Map<String, Object> getVersionProperties();
}
