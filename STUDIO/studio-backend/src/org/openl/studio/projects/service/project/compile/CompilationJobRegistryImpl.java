package org.openl.studio.projects.service.project.compile;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.ui.WorkspaceResetEvent;
import org.openl.rules.workspace.ProjectId;
import org.openl.studio.session.ClientSessionScope;

/**
 * Default {@link CompilationJobRegistry} implementation.
 *
 * <p>Holds at most one active job at a time, matching the WebStudio session
 * model where only the current project/module is compiled. Each
 * {@link #acquire(ProjectId, ProjectModel)} call replaces the current
 * entry with a fresh job so that the returned future always observes the model
 * state from now on; the previous job's future is cancelled if it was still
 * polling.
 *
 * <p>{@link #find(ProjectId, String)} additionally adopts compilations initiated
 * outside of {@link #acquire(ProjectId, ProjectModel)} — JSF flows
 * (Project Tree → {@code WebStudio.init} → {@code setModuleInfo}) drive
 * {@link ProjectModel#compileProject(boolean, boolean)} directly without going through
 * the REST {@code openProject} path. Such compilations register a {@code RegisteredCompilation}
 * on the model; if the WebStudio session's current project matches the requested
 * project/branch, the registry wraps that live cycle in a fresh {@link CompilationJob}
 * so the project status reports the real compile state instead of {@code IDLE}.
 *
 * @author Vladyslav Pikus
 */
@Slf4j
@Component
@ClientSessionScope
@RequiredArgsConstructor
public class CompilationJobRegistryImpl implements CompilationJobRegistry {

    private final WebStudio webStudio;

    private record Entry(
            @NotNull
            ProjectId projectId,
            @Nullable
            String branch,
            @NotNull
            CompilationJobImpl job) {

        public boolean canReuse(ProjectId projectId, ProjectModel model) {
            return this.projectId.equals(projectId)
                    && Objects.equals(this.branch, branchOf(model))
                    && this.job.project() == model
                    && this.job.tracksCurrentCompilation();
        }
    }

    private final AtomicReference<Entry> ref = new AtomicReference<>();

    /** The branch the model's project is on, or null when the model holds no project. */
    private static @Nullable String branchOf(ProjectModel model) {
        var project = model.getProject();
        return project == null ? null : project.getBranch();
    }

    @Override
    @NotNull
    public synchronized CompilationJob acquire(@NotNull ProjectId projectId, @NotNull ProjectModel model) {
        var previous = ref.get();
        if (previous != null && previous.canReuse(projectId, model)) {
            return previous.job();
        }
        if (previous != null && !previous.job().isFinished()) {
            previous.job().future().cancel(false);
        }
        var branch = branchOf(model);
        var entry = new Entry(projectId, branch, new CompilationJobImpl(model));
        ref.set(entry);
        return entry.job();
    }

    /**
     * Drop the cached compilation job so the project status no longer reports a stale
     * compile state after the workspace is reset. Cancels the tracked future if it is
     * still running. The next {@link #acquire(ProjectId, ProjectModel)} registers a
     * fresh job.
     */
    @Override
    public synchronized void clear() {
        var previous = ref.getAndSet(null);
        if (previous != null && !previous.job().isFinished()) {
            previous.job().future().cancel(false);
        }
    }

    @EventListener
    public void onWorkspaceReset(@NonNull WorkspaceResetEvent event) {
        try {
            clear();
        } catch (Exception e) {
            log.warn("onWorkspaceReset failed", e);
        }
    }

    @Override
    @NotNull
    public synchronized Optional<CompilationJob> find(@NotNull ProjectId projectId, @Nullable String branch) {
        var entry = ref.get();
        if (entry != null
                && entry.projectId().equals(projectId)
                && Objects.equals(entry.branch(), branch)
                && entry.job().tracksCurrentCompilation()) {
            return Optional.of(entry.job());
        }
        return adoptFromSession(projectId, branch);
    }

    /**
     * Wrap a compilation that was started outside of {@link #acquire(ProjectId, ProjectModel)}
     * (typically via the JSF Project Tree flow, which calls {@code WebStudio.init} →
     * {@code setModuleInfo} → {@code compileProject} directly). Returns empty when the
     * WebStudio session has no current project, the current project does not match the
     * requested project/branch, or no module is selected yet — opening a project from the
     * tree without selecting a module sets {@code currentProject} but skips the
     * {@code setModuleInfo} / {@code compileProject} branch in {@code WebStudio.init},
     * meaning no compilation has actually started. Returns empty as well while a write is waiting to be
     * compiled: what the session holds then was worked out from the workbook as it stood before the write.
     */
    private Optional<CompilationJob> adoptFromSession(ProjectId projectId, @Nullable String branch) {
        var currentProject = webStudio.getCurrentProject();
        if (currentProject == null || webStudio.getCurrentModule() == null) {
            return Optional.empty();
        }
        if (!currentProject.getProjectId().equals(projectId)
                || !Objects.equals(currentProject.getBranch(), branch)) {
            return Optional.empty();
        }
        // A write has changed a module the session has not built again yet. What it compiled was worked out from
        // the workbook as it stood before that write, so adopting it would report the project as it used to be.
        if (webStudio.isAwaitingRecompile()) {
            return Optional.empty();
        }
        var model = webStudio.getModel();
        if (model == null || model.getCurrentCompilation() == null) {
            return Optional.empty();
        }
        var entry = new Entry(projectId, branch, new CompilationJobImpl(model));
        ref.set(entry);
        return Optional.of(entry.job());
    }
}
