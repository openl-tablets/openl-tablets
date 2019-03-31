package org.openl.rules.repository.factories;

import java.io.Closeable;
import java.lang.ref.WeakReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shut Down Hook to close/release JCR.
 *
 * @author Aleh Bykhavets
 */
public class ShutDownHook extends Thread {
    private final Logger log = LoggerFactory.getLogger(ShutDownHook.class);

    /**
     * Without WeakReference GC will never finalize repository factory.
     */
    private WeakReference<Closeable> ref;

    public ShutDownHook(Closeable closable) {
        ref = new WeakReference<>(closable);
    }

    @Override
    public void run() {
        Closeable closable = ref.get();
        if (closable == null) {
            // nothing to do, already finalized by GC
            return;
        }

        try {
            closable.close();
        } catch (Exception e) {
            log.error("shutDownHook", e);
        }
    }
}
