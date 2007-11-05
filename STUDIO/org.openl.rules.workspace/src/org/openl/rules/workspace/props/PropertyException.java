package org.openl.rules.workspace.props;

import org.openl.CommonException;

public class PropertyException extends CommonException {
    public PropertyException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public PropertyException(String pattern, Throwable cause, Object... params) {
        super(pattern, cause, params);
    }
}
