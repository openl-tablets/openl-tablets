package org.openl.rules.common;

import java.util.Date;
import java.io.Serializable;

public interface VersionInfo extends Serializable {
    Date getCreatedAt();

    String getCreatedBy();

    Date getModifiedAt();

    String getModifiedBy();
}
