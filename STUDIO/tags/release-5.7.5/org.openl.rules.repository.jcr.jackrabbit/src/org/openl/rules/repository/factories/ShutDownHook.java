package org.openl.rules.repository.factories;

import java.lang.ref.WeakReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.repository.exceptions.RRepositoryException;

/**
 * Shut Down Hook to close/release JCR.
 *
 * @author Aleh Bykhavets
 */
public class ShutDownHook extends Thread {
    private static Log log = LogFactory.getLog(ShutDownHook.class);

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
