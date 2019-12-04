package org.openl.rules.common;

import java.io.Serializable;
import java.util.Date;

public interface VersionInfo extends Serializable {
    Date getCreatedAt();

    String getCreatedBy();
}
