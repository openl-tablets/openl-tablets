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

    public static void debug(Object message) {
        final org.apache.commons.logging.Log log = LogFactory.getLog(Log.class);
        log.debug(message);
    }

    public static void debug(Object message, Throwable t) {
        final org.apache.commons.logging.Log log = LogFactory.getLog(Log.class);
        log.debug(message, t);
    }

    public static void debug(String pattern, Object... params) {
        final org.apache.commons.logging.Log log = LogFactory.getLog(Log.class);
        if (!isDebugEnabled()) {
            return;
        }

        String message = format(pattern, params);
        log.debug(message);
    }

    public static void debug(String pattern, Throwable t, Object... params) {
        final org.apache.commons.logging.Log log = LogFactory.getLog(Log.class);
        if (!isDebugEnabled()) {
            return;
        }

        String message = format(pattern, params);
        log.debug(message, t);
    }

    public static void error(Object message) {
        final org.apache.commons.logging.Log log = LogFactory.getLog(Log.class);
        log.error(message);
    }

    public static void error(Object message, Throwable t) {
        final org.apache.commons.logging.Log log = LogFactory.getLog(Log.class);
        log.error(message, t);
    }

    public static void error(String pattern, Object... params) {
        final org.apache.commons.logging.Log log = LogFactory.getLog(Log.class);
        if (!isErrorEnabled()) {
            return;
        }

        String message = format(pattern, params);
        log.error(message);
    }

    public static void error(String pattern, Throwable t, Object... params) {
        final org.apache.commons.logging.Log log = LogFactory.getLog(Log.class);
        if (!isErrorEnabled()) {
            return;
        }

        String message = format(pattern, params);
        log.error(message, t);
    }

    private static String format(String pattern, Object... params) {
        return MessageFormat.format(pattern, params);
    }

    public static void info(Object message) {
        final org.apache.commons.logging.Log log = LogFactory.getLog(Log.class);
        log.info(message);
    }

    public static void info(Object message, Throwable t) {
        final org.apache.commons.logging.Log log = LogFactory.getLog(Log.class);
        log.info(message, t);
    }

    public static void info(String pattern, Object... params) {
        final org.apache.commons.logging.Log log = LogFactory.getLog(Log.class);
        if (!isInfoEnabled()) {
            return;
        }

        String message = format(pattern, params);
        log.info(message);
    }

    public static void info(String pattern, Throwable t, Object... params) {
        final org.apache.commons.logging.Log log = LogFactory.getLog(Log.class);
        if (!isInfoEnabled()) {
            return;
        }

        String message = format(pattern, params);
        log.info(message, t);
    }

    public static boolean isDebugEnabled() {
        final org.apache.commons.logging.Log log = LogFactory.getLog(Log.class);
        return log.isDebugEnabled();
    }

    public static boolean isErrorEnabled() {
        final org.apache.commons.logging.Log log = LogFactory.getLog(Log.class);
        return log.isErrorEnabled();
    }

    public static boolean isInfoEnabled() {
        final org.apache.commons.logging.Log log = LogFactory.getLog(Log.class);
        return log.isInfoEnabled();
    }

    public static boolean isTraceEnabled() {
        final org.apache.commons.logging.Log log = LogFactory.getLog(Log.class);
        return log.isTraceEnabled();
    }

    public static boolean isWarnEnabled() {
        final org.apache.commons.logging.Log log = LogFactory.getLog(Log.class);
        return log.isWarnEnabled();
    }

    public static void trace(Object message) {
        final org.apache.commons.logging.Log log = LogFactory.getLog(Log.class);
        log.trace(message);
    }

    public static void trace(Object message, Throwable t) {
        final org.apache.commons.logging.Log log = LogFactory.getLog(Log.class);
        log.trace(message, t);
    }

    public static void trace(String pattern, Object... params) {
        final org.apache.commons.logging.Log log = LogFactory.getLog(Log.class);
        if (!isTraceEnabled()) {
            return;
        }

        String message = format(pattern, params);
        log.trace(message);
    }

    public static void trace(String pattern, Throwable t, Object... params) {
        final org.apache.commons.logging.Log log = LogFactory.getLog(Log.class);
        if (!isTraceEnabled()) {
            return;
        }

        String message = format(pattern, params);
        log.trace(message, t);
    }

    public static void warn(Object message) {
        final org.apache.commons.logging.Log log = LogFactory.getLog(Log.class);
        log.warn(message);
    }

    public static void warn(Object message, Throwable t) {
        final org.apache.commons.logging.Log log = LogFactory.getLog(Log.class);
        log.warn(message, t);
    }

    public static void warn(String pattern, Object... params) {
        final org.apache.commons.logging.Log log = LogFactory.getLog(Log.class);
        if (!isWarnEnabled()) {
            return;
        }

        String message = format(pattern, params);
        log.warn(message);
    }

    public static void warn(String pattern, Throwable t, Object... params) {
        final org.apache.commons.logging.Log log = LogFactory.getLog(Log.class);
        if (!isWarnEnabled()) {
            return;
        }

        String message = format(pattern, params);
        log.warn(message, t);
    }
}
