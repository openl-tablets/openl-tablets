package org.openl.rules.workspace.props;

public class PropertyTypeException extends PropertyException {
    private static final long serialVersionUID = 1841665870744906959L;

    public PropertyTypeException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public PropertyTypeException(String pattern, Throwable cause, Object... params) {
        super(pattern, cause, params);
    }
}
