package org.openl.rules.workspace.props;

import org.openl.CommonException;

public class PropertyException extends CommonException {
    public PropertyException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public PropertyException(String pattern, Object... params) {
        super(pattern, params);
    }
}
