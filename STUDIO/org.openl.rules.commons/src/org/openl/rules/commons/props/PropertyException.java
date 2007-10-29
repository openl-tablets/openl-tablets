package org.openl.rules.commons.props;

import org.openl.rules.commons.CommonException;

public class PropertyException extends CommonException {
    public PropertyException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public PropertyException(String msg, Object... params) {
        super(msg, params);
    }
}
