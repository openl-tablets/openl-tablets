package org.openl.rules.workspace.abstracts;

import org.openl.CommonException;

public class ProjectException extends CommonException {
    private static final long serialVersionUID = -2918146804954398129L;

    public ProjectException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public ProjectException(String pattern, Throwable cause, Object... params) {
        super(pattern, cause, params);
    }
}
