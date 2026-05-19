package org.openl.studio.projects.service.project.compile;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import org.openl.rules.ui.ProjectModel;
import org.openl.studio.projects.model.ProjectIdModel;

/**
 * Default {@link CompilationJobRegistry} implementation.
 *
 * <p>Holds at most one active job at a time, matching the WebStudio session
 * model where only the current project/module is compiled. Each
 * {@link #acquire(ProjectIdModel, ProjectModel)} call replaces the current
 * entry with a fresh job so that the returned future always observes the model
 * state from now on; the previous job's future is cancelled if it was still
 * polling.
 *
 * @author Vladyslav Pikus
 */
@Component
@Scope(WebApplicationContext.SCOPE_SESSION)
public class CompilationJobRegistryImpl implements CompilationJobRegistry {

    private record Entry(
            @NotNull
            ProjectIdModel projectId,
            @Nullable
            String branch,
            @NotNull
            CompilationJobImpl job) {

        public boolean canReuse(ProjectIdModel projectId, ProjectModel model) {
            return this.projectId.equals(projectId)
                    && Objects.equals(this.branch, model.getProject().getBranch())
                    && this.job.getModel() == model
                    && this.job.tracksCurrentCompilation();
        }
    }

    private final AtomicReference<Entry> ref = new AtomicReference<>();

    @Override
    @NotNull
    public CompilationJob acquire(@NotNull ProjectIdModel projectId, @NotNull ProjectModel model) {
        var previous = ref.get();
        if (previous != null && previous.canReuse(projectId, model)) {
            return previous.job();
        }
        if (previous != null && !previous.job().isFinished()) {
            previous.job().future().cancel(false);
        }
        var entry = new Entry(projectId,
                model.getProject().getBranch(),
                new CompilationJobImpl(model));
        ref.set(entry);
        return entry.job();
    }
}
