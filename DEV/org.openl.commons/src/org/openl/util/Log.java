/*
 * Created on May 14, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.util;

import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In case of methods with format pattern you must follow conventions/restrictions of undeground formatter. For example,
 * you need to double single quote (') character if you wish to see it in output.
 * <p/>
 *
 * <pre>
 * Log.error(&quot;File ''{0}'' is absent!&quot;, fileName);
 * </pre>
 *
 * @author snshor
 * @author abykhavets
 */
public class Log {

    public static void debug(Object message) {
        final Logger log = LoggerFactory.getLogger(Log.class);
        log.debug(String.valueOf(message));
    }

    public static void debug(Object message, Throwable t) {
        final Logger log = LoggerFactory.getLogger(Log.class);
        log.debug(String.valueOf(message), t);
    }

    public static void debug(String pattern, Object... params) {
        final Logger log = LoggerFactory.getLogger(Log.class);
        if (!isDebugEnabled()) {
            return;
        }

        String message = format(pattern, params);
        log.debug(message);
    }

    public static void debug(String pattern, Throwable t, Object... params) {
        final Logger log = LoggerFactory.getLogger(Log.class);
        if (!isDebugEnabled()) {
            return;
        }

        String message = format(pattern, params);
        log.debug(message, t);
    }

    public static void error(Object message) {
        final Logger log = LoggerFactory.getLogger(Log.class);
        log.error(String.valueOf(message));
    }

    public static void error(Object message, Throwable t) {
        final Logger log = LoggerFactory.getLogger(Log.class);
        log.error(String.valueOf(message), t);
    }

    public static void error(String pattern, Object... params) {
        final Logger log = LoggerFactory.getLogger(Log.class);
        if (!isErrorEnabled()) {
            return;
        }

        String message = format(pattern, params);
        log.error(message);
    }

    public static void error(String pattern, Throwable t, Object... params) {
        final Logger log = LoggerFactory.getLogger(Log.class);
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
        final Logger log = LoggerFactory.getLogger(Log.class);
        log.info(String.valueOf(message));
    }

    public static void info(Object message, Throwable t) {
        final Logger log = LoggerFactory.getLogger(Log.class);
        log.info(String.valueOf(message), t);
    }

    public static void info(String pattern, Object... params) {
        final Logger log = LoggerFactory.getLogger(Log.class);
        if (!isInfoEnabled()) {
            return;
        }

        String message = format(pattern, params);
        log.info(message);
    }

    public static void info(String pattern, Throwable t, Object... params) {
        final Logger log = LoggerFactory.getLogger(Log.class);
        if (!isInfoEnabled()) {
            return;
        }

        String message = format(pattern, params);
        log.info(message, t);
    }

    public static boolean isDebugEnabled() {
        final Logger log = LoggerFactory.getLogger(Log.class);
        return log.isDebugEnabled();
    }

    public static boolean isErrorEnabled() {
        final Logger log = LoggerFactory.getLogger(Log.class);
        return log.isErrorEnabled();
    }

    public static boolean isInfoEnabled() {
        final Logger log = LoggerFactory.getLogger(Log.class);
        return log.isInfoEnabled();
    }

    public static boolean isTraceEnabled() {
        final Logger log = LoggerFactory.getLogger(Log.class);
        return log.isTraceEnabled();
    }

    public static boolean isWarnEnabled() {
        final Logger log = LoggerFactory.getLogger(Log.class);
        return log.isWarnEnabled();
    }

    public static void trace(Object message) {
        final Logger log = LoggerFactory.getLogger(Log.class);
        log.trace(String.valueOf(message));
    }

    public static void trace(Object message, Throwable t) {
        final Logger log = LoggerFactory.getLogger(Log.class);
        log.trace(String.valueOf(message), t);
    }

    public static void trace(String pattern, Object... params) {
        final Logger log = LoggerFactory.getLogger(Log.class);
        if (!isTraceEnabled()) {
            return;
        }

        String message = format(pattern, params);
        log.trace(message);
    }

    public static void trace(String pattern, Throwable t, Object... params) {
        final Logger log = LoggerFactory.getLogger(Log.class);
        if (!isTraceEnabled()) {
            return;
        }

        String message = format(pattern, params);
        log.trace(message, t);
    }

    public static void warn(Object message) {
        final Logger log = LoggerFactory.getLogger(Log.class);
        log.warn(String.valueOf(message));
    }

    public static void warn(Object message, Throwable t) {
        final Logger log = LoggerFactory.getLogger(Log.class);
        log.warn(String.valueOf(message), t);
    }

    public static void warn(String pattern, Object... params) {
        final Logger log = LoggerFactory.getLogger(Log.class);
        if (!isWarnEnabled()) {
            return;
        }

        String message = format(pattern, params);
        log.warn(message);
    }

    public static void warn(String pattern, Throwable t, Object... params) {
        final Logger log = LoggerFactory.getLogger(Log.class);
        if (!isWarnEnabled()) {
            return;
        }

        String message = format(pattern, params);
        log.warn(message, t);
    }
}
