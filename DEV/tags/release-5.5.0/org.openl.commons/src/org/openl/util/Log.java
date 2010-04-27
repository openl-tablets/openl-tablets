/*
 * Created on May 14, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.util;

import java.text.MessageFormat;

import org.apache.commons.logging.LogFactory;

/**
 * In case of methods with format pattern you must follow
 * conventions/restrictions of undeground formatter. For example, you need to
 * double single quote (') character if you wish to see it in output.
 *
 * <pre>
 * Log.error(&quot;File ''{0}'' is absent!&quot;, fileName);
 * </pre>
 *
 * @author snshor
 * @author abykhavets
 *
 */
public class Log {

    static org.apache.commons.logging.Log logger = LogFactory.getLog(Log.class);

    public static void debug(Object message) {
        logger.debug(message);
    }

    public static void debug(Object message, Throwable t) {
        logger.debug(message, t);
    }

    public static void debug(String pattern, Object... params) {
        if (!isDebugEnabled()) {
            return;
        }

        String message = format(pattern, params);
        logger.debug(message);
    }

    public static void debug(String pattern, Throwable t, Object... params) {
        if (!isDebugEnabled()) {
            return;
        }

        String message = format(pattern, params);
        logger.debug(message, t);
    }

    public static void error(Object message) {
        logger.error(message);
    }

    public static void error(Object message, Throwable t) {
        logger.error(message, t);
    }

    public static void error(String pattern, Object... params) {
        if (!isErrorEnabled()) {
            return;
        }

        String message = format(pattern, params);
        logger.error(message);
    }

    public static void error(String pattern, Throwable t, Object... params) {
        if (!isErrorEnabled()) {
            return;
        }

        String message = format(pattern, params);
        logger.error(message, t);
    }

    private static String format(String pattern, Object... params) {
        return MessageFormat.format(pattern, params);
    }

    public static void info(Object message) {
        logger.info(message);
    }

    public static void info(Object message, Throwable t) {
        logger.info(message, t);
    }

    public static void info(String pattern, Object... params) {
        if (!isInfoEnabled()) {
            return;
        }

        String message = format(pattern, params);
        logger.info(message);
    }

    public static void info(String pattern, Throwable t, Object... params) {
        if (!isInfoEnabled()) {
            return;
        }

        String message = format(pattern, params);
        logger.info(message, t);
    }

    public static boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    public static boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    public static boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    public static boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    public static boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    public static void trace(Object message) {
        logger.trace(message);
    }

    public static void trace(Object message, Throwable t) {
        logger.trace(message, t);
    }

    public static void trace(String pattern, Object... params) {
        if (!isTraceEnabled()) {
            return;
        }

        String message = format(pattern, params);
        logger.trace(message);
    }

    public static void trace(String pattern, Throwable t, Object... params) {
        if (!isTraceEnabled()) {
            return;
        }

        String message = format(pattern, params);
        logger.trace(message, t);
    }

    public static void warn(Object message) {
        logger.warn(message);
    }

    public static void warn(Object message, Throwable t) {
        logger.warn(message, t);
    }

    public static void warn(String pattern, Object... params) {
        if (!isWarnEnabled()) {
            return;
        }

        String message = format(pattern, params);
        logger.warn(message);
    }

    public static void warn(String pattern, Throwable t, Object... params) {
        if (!isWarnEnabled()) {
            return;
        }

        String message = format(pattern, params);
        logger.warn(message, t);
    }
}
