package org.openl.studio.projects.service.project.compile;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.ui.WorkspaceResetEvent;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.service.ProjectIdentifierMapper;

/**
 * Default {@link CompilationJobRegistry} implementation.
 *
 * <p>Holds one active job per opened project/branch so a session can compile
 * several projects in parallel (multiple tabs, async REST edits to different
 * projects). Each {@link #acquire(ProjectIdModel, ProjectModel)} call replaces
 * that project/branch's entry with a fresh job so the returned future always
 * observes the model state from now on; the previous job's future is cancelled
 * if it was still polling. Other projects' entries are left untouched.
 *
 * <p>{@link #find(ProjectIdModel, String)} additionally adopts compilations initiated
 * outside of {@link #acquire(ProjectIdModel, ProjectModel)} — JSF flows
 * (Project Tree → {@code WebStudio.init} → {@code setModuleInfo}) drive
 * {@link ProjectModel#compileProject(boolean, boolean)} directly without going through
 * the REST {@code openProject} path. Such compilations register a {@code RegisteredCompilation}
 * on the model; if the WebStudio session's current project matches the requested
 * project/branch, the registry wraps that live cycle in a fresh {@link CompilationJob}
 * so the status endpoint can report the real compile state instead of {@code IDLE}.
 *
 * @author Vladyslav Pikus
 */
@Slf4j
@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
@RequiredArgsConstructor
public class CompilationJobRegistryImpl implements CompilationJobRegistry {

    private final WebStudio webStudio;
    private final ProjectIdentifierMapper projectIdentifierMapper;

    /**
     * Identifies a job by the project and branch it tracks. Two branches of the same project are distinct
     * entries, so switching a project's branch never reuses the previous branch's job.
     */
    private record JobKey(@NotNull ProjectIdModel projectId, @Nullable String branch) {
    }

    private record Entry(@NotNull CompilationJobImpl job) {

        public boolean canReuse(ProjectModel model) {
            // The entry was looked up by its (projectId, branch) key, so those already match; only the model
            // instance (it may have been evicted and recreated) and its current compile cycle can differ.
            return this.job.project() == model && this.job.tracksCurrentCompilation();
        }
    }

    private final Map<JobKey, Entry> entries = new ConcurrentHashMap<>();

    @Override
    @NotNull
    public synchronized CompilationJob acquire(@NotNull ProjectIdModel projectId, @NotNull ProjectModel model) {
        var project = model.getProject();
        var branch = project != null ? project.getBranch() : null;
        var key = new JobKey(projectId, branch);
        var previous = entries.get(key);
        if (previous != null && previous.canReuse(model)) {
            return previous.job();
        }
        cancelIfRunning(previous);
        var entry = new Entry(new CompilationJobImpl(model));
        entries.put(key, entry);
        return entry.job();
    }

    /**
     * Drop all cached compilation jobs so the status endpoint no longer reports a stale
     * compile state after the workspace is reset. Cancels any tracked future still running.
     * The next {@link #acquire(ProjectIdModel, ProjectModel)} registers a fresh job.
     */
    @Override
    public synchronized void clear() {
        entries.values().forEach(this::cancelIfRunning);
        entries.clear();
    }

    @Override
    public synchronized void clear(@NotNull ProjectIdModel projectId, @Nullable String branch) {
        var previous = entries.remove(new JobKey(projectId, branch));
        cancelIfRunning(previous);
    }

    private void cancelIfRunning(@Nullable Entry entry) {
        if (entry != null && !entry.job().isFinished()) {
            entry.job().future().cancel(false);
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
    public synchronized Optional<CompilationJob> find(@NotNull ProjectIdModel projectId, @Nullable String branch) {
        var entry = entries.get(new JobKey(projectId, branch));
        if (entry != null && entry.job().tracksCurrentCompilation()) {
            return Optional.of(entry.job());
        }
        return adoptFromSession(projectId, branch);
    }

    /**
     * Wrap a compilation that was started outside of {@link #acquire(ProjectIdModel, ProjectModel)}
     * (typically via the JSF Project Tree flow, which calls {@code WebStudio.init} →
     * {@code setModuleInfo} → {@code compileProject} directly). Returns empty when the
     * WebStudio session has no current project, the current project does not match the
     * requested project/branch, or no module is selected yet — opening a project from the
     * tree without selecting a module sets {@code currentProject} but skips the
     * {@code setModuleInfo} / {@code compileProject} branch in {@code WebStudio.init},
     * meaning no compilation has actually started.
     */
    private Optional<CompilationJob> adoptFromSession(ProjectIdModel projectId, @Nullable String branch) {
        var currentProject = webStudio.getCurrentProject();
        if (currentProject == null || webStudio.getCurrentModule() == null) {
            return Optional.empty();
        }
        if (!projectIdentifierMapper.map(currentProject).equals(projectId)
                || !Objects.equals(currentProject.getBranch(), branch)) {
            return Optional.empty();
        }
        var model = webStudio.getModel();
        if (model == null || model.getCurrentCompilation() == null) {
            return Optional.empty();
        }
        var entry = new Entry(new CompilationJobImpl(model));
        entries.put(new JobKey(projectId, branch), entry);
        return Optional.of(entry.job());
    }
}
