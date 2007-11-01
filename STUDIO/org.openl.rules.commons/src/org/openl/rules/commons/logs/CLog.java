package org.openl.rules.commons.logs;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.text.MessageFormat;
import java.util.Formatter;

public class CLog {
    public static final CLogLevel FATAL = CLogLevel.FATAL;
    public static final CLogLevel ERROR = CLogLevel.ERROR;
    public static final CLogLevel WARNING = CLogLevel.WARNING;
    public static final CLogLevel INFO = CLogLevel.INFO;
    public static final CLogLevel DEBUG = CLogLevel.DEBUG;
    public static final CLogLevel TRACE = CLogLevel.TRACE;

    private static final Log log = LogFactory.getLog("default");

    public static boolean log(CLogLevel level, String msg) {
        return log(level, msg, (Throwable)null);
    }

    public static boolean log(CLogLevel level, String pattern, Object... params) {
        return log(level, pattern, null, params);
    }

    public static boolean logf(CLogLevel level, String pattern, Object... params) {
        return logf(level, pattern, null, params);
    }

    public static boolean log(CLogLevel level, String pattern, Throwable cause, Object... params) {
        if (!isEnabled(level)) {
            return false;
        }

        String msg = MessageFormat.format(pattern, params);

        return log(level, msg, cause);
    }

    public static boolean logf(CLogLevel level, String format, Throwable cause, Object... params) {
        if (!isEnabled(level)) {
            return false;
        }

        Formatter f = new Formatter();
        Formatter f2 = f.format(format, params);
        String msg = f2.toString();

        return log(level, msg, cause);
    }

    public static boolean log(CLogLevel level, String msg, Throwable cause) {
        if (!isEnabled(level)) {
            return false;
        }

        switch (level) {
            case FATAL:
                log.fatal(msg, cause);
                break;
            case ERROR:
                log.error(msg, cause);
                break;
            case WARNING:
                log.warn(msg, cause);
                break;
            case INFO:
                log.info(msg, cause);
                break;
            case DEBUG:
                log.debug(msg, cause);
                break;
            case TRACE:
                log.trace(msg, cause);
                break;
            default:
                log.error(msg, cause);
        }

        return true;
    }

    public static boolean isEnabled(CLogLevel level) {
        boolean enabled;

        switch (level) {
            case FATAL:
                enabled = log.isFatalEnabled();
                break;
            case ERROR:
                enabled = log.isErrorEnabled();
                break;
            case WARNING:
                enabled = log.isWarnEnabled();
                break;
            case INFO:
                enabled = log.isInfoEnabled();
                break;
            case DEBUG:
                enabled = log.isDebugEnabled();
                break;
            case TRACE:
                enabled = log.isTraceEnabled();
                break;
            default:
                // just in case
                enabled = log.isErrorEnabled();
        }

        return enabled;
    }
}
