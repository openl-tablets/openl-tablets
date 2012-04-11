package org.openl.rules.workspace.abstracts;

import java.util.Date;
import java.io.Serializable;

public interface VersionInfo extends Serializable {
    Date getCreatedAt();

    String getCreatedBy();
}
