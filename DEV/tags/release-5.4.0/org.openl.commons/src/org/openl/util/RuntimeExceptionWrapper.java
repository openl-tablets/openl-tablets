/*
 * Created on May 20, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.util;

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
        return new NestableRuntimeException(cause.getMessage(), cause);
    }

}
