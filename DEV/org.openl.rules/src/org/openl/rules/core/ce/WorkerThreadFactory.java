package org.openl.rules.core.ce;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

/**
 * Custom implementation of {@code ForkJoinPool.ForkJoinWorkerThreadFactory} to fix
 * {@link java.security.AccessControlException} when application run with custom {@code java.security.policy} file.
 *
 * Default {@link ForkJoinPool} factory implementation initializes thread with ProtectionDomain with no privileges:
 * 
 * <pre>
 *  access:
 *  domain that failed ProtectionDomain null
 *  null
 *  <no principals>
 *  null
 * </pre>
 * 
 * that cause permission check failing
 *
 * @link <a href="https://github.com/opensearch-project/OpenSearch/issues/1649">Is it possible to remove
 *       "modifyThreadGroup" checking from SecureSM?</a>
 * @link <a href="https://stackoverflow.com/q/63059618">How to handle AccessControlException with ProtectionDomain
 *       null</a>
 *
 * @author Vladyslav Pikus
 */
public class WorkerThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {

    @Override
    public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
        return AccessController.doPrivileged(new PrivilegedAction<ForkJoinWorkerThread>() {
            public ForkJoinWorkerThread run() {
                return new WorkerThread(pool, ClassLoader.getSystemClassLoader());
            }
        });
    }

    private static class WorkerThread extends ForkJoinWorkerThread {

        protected WorkerThread(ForkJoinPool pool, ClassLoader cl) {
            super(pool);
            setContextClassLoader(cl);
        }
    }
}
