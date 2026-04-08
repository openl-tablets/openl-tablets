package org.openl.rules.runtime;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoggingHandler {
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
        return cap.serializer() != null && (cap.loggingEnabled() || log.isDebugEnabled());
    }

    static String convert(Object obj) {
        return INSTANCE.get().serializer().apply(obj);
    }

    static void log(CharSequence text) {
        if (INSTANCE.get().loggingEnabled()) {
            log.info("\n{}", text);
        } else {
            log.debug("\n{}", text);
        }
    }
}
