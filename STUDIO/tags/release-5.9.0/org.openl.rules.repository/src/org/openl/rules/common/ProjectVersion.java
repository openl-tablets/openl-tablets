package org.openl.rules.common;

import java.io.Serializable;

public interface ProjectVersion extends Serializable, CommonVersion {

    VersionInfo getVersionInfo();
}
