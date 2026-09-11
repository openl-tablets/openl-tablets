package org.openl.studio.projects.service.project.compile;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Starts a module's compilation away from the request that asked for it.
 *
 * <p>Opening a module compiles it, which on a large project takes minutes. The caller is answered as soon as the
 * work is handed over and follows the compilation on the project's status channel, instead of holding a request
 * open for the whole of it.
 *
 * <p>The work arrives as a task rather than as a module to open, because the session-scoped collaborators it needs
 * can only be resolved by the request thread.
 */
@Slf4j
@Service
public class ModuleCompilationLauncher {

    /**
     * Runs the given work on a background thread.
     *
     * <p>A failure is logged and goes no further. The caller has already been answered, and what the compilation
     * ended as is what the project's status channel reports.
     *
     * @param moduleName module being opened, named in the log when the work fails
     * @param open       opens the module, and with it compiles it
     */
    @Async("moduleCompileExecutor")
    public void launch(String moduleName, Runnable open) {
        try {
            open.run();
        } catch (RuntimeException | LinkageError e) {
            log.warn("Failed to compile module '{}'", moduleName, e);
        }
    }

}
