package org.openl.studio.common.utils;

import java.util.ConcurrentModificationException;
import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.ServletRequestListener;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.ThreadLocalUtil;

/**
 * Clears the data Apache POI caches on the current thread.
 *
 * <p>POI keeps data of the workbooks a thread reads in {@code ThreadLocal}s of that thread, and only that thread can
 * clear them. A pooled thread keeps the data after its work is done, and Tomcat reports it as a leak when the
 * application stops while a thread still holds it. So read workbooks only on virtual threads or on pooled threads that
 * call {@link #clear()} when their work is done. As a request listener, this class does it for the request threads of
 * the container.
 *
 * <p>A POI class loaded for the first time on another thread at the same moment can make POI fail the clearing. The
 * failure is logged at debug level, and the next clearing on this thread takes what is left.
 */
@Slf4j
public class WorkbookThreadLocals implements ServletRequestListener {

    public static void clear() {
        try {
            ThreadLocalUtil.clearAllThreadLocals();
        } catch (ConcurrentModificationException e) {
            log.debug("Apache POI registered a thread local cleaner while the thread locals were cleared.", e);
        }
    }

    @Override
    public void requestDestroyed(ServletRequestEvent event) {
        clear();
    }
}
