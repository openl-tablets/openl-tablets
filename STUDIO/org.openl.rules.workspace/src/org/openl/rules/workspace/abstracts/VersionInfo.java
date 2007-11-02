package org.openl.rules.workspace.abstracts;

import java.util.Date;

public interface VersionInfo {
    Date getCreatedAt();
    String getCreatedBy();
}
