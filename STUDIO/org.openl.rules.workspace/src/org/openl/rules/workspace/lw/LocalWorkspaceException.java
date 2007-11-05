package org.openl.rules.workspace.lw;

import org.openl.rules.workspace.abstracts.ProjectException;

public class LocalWorkspaceException extends ProjectException {
    public LocalWorkspaceException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public LocalWorkspaceException(String pattern, Throwable cause, Object... params) {
        super(pattern, cause, params);
    }
}
