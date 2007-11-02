package org.openl;

import java.text.MessageFormat;

public class CommonException extends Exception {
    private static final long serialVersionUID = 9016192638697492055L;

    public CommonException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public CommonException(String pattern, Throwable cause, Object... params) {
        super(format(pattern, params), cause);
    }

    public CommonException(String pattern, Object... params) {
        super(format(pattern, params));
    }

    // --- private
    
    private static String format(String pattern, Object... params) {
        return MessageFormat.format(pattern, params);
    }
}
