package org.openl.rules.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingHandler {
    private static final Logger LOG = LoggerFactory.getLogger("openl.rules.invoke");
    private static final ThreadLocal<LoggingCapability> INSTANCE = new ThreadLocal<>();

    public static void setup(LoggingCapability value) {
        INSTANCE.set(value);
    }

    public static void remove() {
        INSTANCE.remove();
    }

    static boolean isEnabled() {
        LoggingCapability cap = INSTANCE.get();
        if (cap == null) {
            return false;
        }
        return cap.serializer() != null && (cap.loggingEnabled() || LOG.isDebugEnabled());
    }

    static String convert(Object obj) {
        return INSTANCE.get().serializer().apply(obj);
    }

    static void log(CharSequence text) {
        if (INSTANCE.get().loggingEnabled()) {
            LOG.info("\n{}", text);
        } else {
            LOG.debug("\n{}", text);
        }
    }
}
