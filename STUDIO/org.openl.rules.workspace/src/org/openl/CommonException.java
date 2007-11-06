package org.openl;

import java.text.MessageFormat;

/**
 * There are no CommonException(String pattern, Object... params)
 * constructor since it will lead to ambiguous case with Throwable.
 * Thus, Throwable can be placed in "Object..." and will be treated 
 * like parameter for result message, not a <code>cause</code>.
 * That kind of bug is hard to detect.
 * 
 * If you cannot provide Throwable just use <code>null</code>.
 * 
 * @author Aleh Bykhavets
 *
 */
public class CommonException extends Exception {
    private static final long serialVersionUID = 9016192638697492055L;

    public CommonException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public CommonException(String pattern, Throwable cause, Object... params) {
        super(format(pattern, params), cause);
    }

    // --- private
    
    private static String format(String pattern, Object... params) {
        return MessageFormat.format(pattern, params);
    }
}
