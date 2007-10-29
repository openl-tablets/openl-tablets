package org.openl.rules.commons;

import java.text.MessageFormat;

public class CommonException extends Exception {
    public CommonException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public CommonException(String msg, Object... params) {
        super(format(msg, params));
    }

    private static String format(String msg, Object... params) {
        return MessageFormat.format(msg, params);
    }
}
