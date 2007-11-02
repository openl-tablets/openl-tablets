package org.openl.rules.workspace;

import org.openl.CommonException;

public class WorkspaceException extends CommonException {
    public WorkspaceException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public WorkspaceException(String pattern, Object... params) {
        super(pattern, params);
    }
}
