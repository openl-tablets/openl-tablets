package org.openl.rules.webstudio.web.repository.diff;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * DiffController state.
 *
 * @author Andrey Naumenko
 */
public class DiffState {
    private static Log log = LogFactory.getLog(DiffState.class);
    private String version = "0.0.0";

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

}
