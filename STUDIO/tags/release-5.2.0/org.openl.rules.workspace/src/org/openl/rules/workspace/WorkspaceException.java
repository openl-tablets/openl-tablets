package org.openl.rules.workspace;

import org.openl.CommonException;

public class WorkspaceException extends CommonException {
    private static final long serialVersionUID = -7485609617006473079L;

    public WorkspaceException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public WorkspaceException(String pattern, Throwable cause, Object... params) {
        super(pattern, cause, params);
    }
}
