package org.openl.studio.projects.service.project.compile;

import java.time.Duration;
import java.util.UUID;

/**
 * Outcome of a successfully completed {@link CompilationJob}.
 *
 * @param jobId      compilation job identifier
 * @param duration   total elapsed time spent compiling
 * @param modulesCompiled number of modules compiled
 * @param modulesTotal    total number of modules in the project
 * @author Vladyslav Pikus
 */
public record CompilationResult(
        UUID jobId,
        Duration duration,
        int modulesCompiled,
        int modulesTotal
) {
}
