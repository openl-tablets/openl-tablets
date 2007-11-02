package org.openl.rules.workspace.props;

public class PropertyTypeException extends PropertyException {
    public PropertyTypeException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public PropertyTypeException(String msg, Object... params) {
        super(msg, params);
    }
}
