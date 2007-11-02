package org.openl.rules.workspace.lw;

import org.openl.rules.workspace.abstracts.ProjectException;

public class LocalWorkspaceException extends ProjectException {
    public LocalWorkspaceException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public LocalWorkspaceException(String msg, Object... params) {
        super(msg, params);
    }
}
