package org.openl.rules.webstudio.web.repository.diff;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Holds information about structured diff in session scope.
 *
 * @author Andrey Naumenko
 */
public class StructuredDiffState {
    private static Log log = LogFactory.getLog(StructuredDiffState.class);
    private String version = "0.0.0";

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
