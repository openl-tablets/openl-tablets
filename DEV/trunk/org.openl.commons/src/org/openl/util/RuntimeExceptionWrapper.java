/*
 * Created on May 20, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.util;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.NestableRuntimeException;

/**
 * @author snshor
 *
 */
public class RuntimeExceptionWrapper {

    static public RuntimeException wrap(String msg, Throwable cause) {
        return new NestableRuntimeException(msg, cause);
    }

    static public RuntimeException wrap(Throwable cause) {
        if (cause instanceof RuntimeException) {
            return (RuntimeException) cause;
        }
        return new NestableRuntimeException(getErrorMessage(cause), cause);
    }
    
    /**
     * Gets the error message from the given exception. If it is empty, gets the message from its cause.
     * @param error error
     * @return the message from the error, or from its cause.
     */
    private static String getErrorMessage(Throwable error) {
        String message;
        message = error.getMessage();
        if (StringUtils.isBlank(message) && error.getCause() != null) {
            message = error.getCause().getMessage();
        }
        return message;
    }

}
