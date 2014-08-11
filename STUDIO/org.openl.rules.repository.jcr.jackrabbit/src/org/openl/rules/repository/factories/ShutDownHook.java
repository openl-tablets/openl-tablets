package org.openl.rules.repository.factories;

import org.openl.rules.repository.exceptions.RRepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;

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
    private WeakReference<AbstractJcrRepositoryFactory> ref;

    public ShutDownHook(AbstractJcrRepositoryFactory repositoryFactory) {
        ref = new WeakReference<AbstractJcrRepositoryFactory>(repositoryFactory);
    }

    @Override
    public void run() {
        AbstractJcrRepositoryFactory repositoryFactory = ref.get();
        if (repositoryFactory == null) {
            // nothing to do, already finalized by GC
            return;
        }

        try {
            repositoryFactory.release();
        } catch (RRepositoryException e) {
            log.error("shutDownHook", e);
        }
    }
}
