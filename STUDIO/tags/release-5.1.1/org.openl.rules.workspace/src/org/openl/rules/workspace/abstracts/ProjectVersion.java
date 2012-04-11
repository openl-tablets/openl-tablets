package org.openl.rules.workspace.abstracts;

import java.io.Serializable;

import org.openl.rules.repository.CommonVersion;

public interface ProjectVersion extends CommonVersion, Serializable {

    VersionInfo getVersionInfo();
}
