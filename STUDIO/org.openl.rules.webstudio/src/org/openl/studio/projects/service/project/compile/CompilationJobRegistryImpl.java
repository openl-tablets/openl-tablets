package org.openl.studio.projects.service.project.compile;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.service.ProjectIdentifierMapper;

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
@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
@RequiredArgsConstructor
public class CompilationJobRegistryImpl implements CompilationJobRegistry {

    private final WebStudio webStudio;
    private final ProjectIdentifierMapper projectIdentifierMapper;

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
                    && this.job.project() == model
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

    @Override
    @NotNull
    public Optional<CompilationJob> find(@NotNull ProjectIdModel projectId, @Nullable String branch) {
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
        return Optional.of(new CompilationJobImpl(model));
    }
}
