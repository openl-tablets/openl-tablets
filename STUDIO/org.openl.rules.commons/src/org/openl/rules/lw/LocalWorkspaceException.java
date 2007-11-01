package org.openl.rules.lw;

import org.openl.rules.commons.projects.ProjectException;

public class LocalWorkspaceException extends ProjectException {
    public LocalWorkspaceException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public LocalWorkspaceException(String msg, Object... params) {
        super(msg, params);
    }
}
