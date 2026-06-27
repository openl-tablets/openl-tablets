package org.openl.rules.ui;

import java.util.Objects;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.testmethod.TestSuiteExecutor;
import org.openl.util.RuntimeExceptionWrapper;

/**
 * The set of projects a session currently has open, each with its own {@link ProjectModel} but all sharing one
 * {@link WorkspaceCompilationContext} (a single dependency manager + syntax-node index) so they reuse each
 * other's compiled dependencies.
 *
 * <p>Owns the bounded {@link ProjectModelRegistry} and the "current selection" key, and keeps the invariant
 * "the current model is the pinned model" in one place ({@link #select}/{@link #selectNone}). Extracted from
 * {@link WebStudio} so the session's project-model state machine has a single, cohesive, testable home; the
 * session's JSF "current selection" labels (current project/module) stay on {@link WebStudio}, which delegates
 * the model operations here.
 */
class OpenedProjectsSession {

    private final WorkspaceCompilationContext compilationContext;
    private final ProjectModelRegistry models;
    private volatile ProjectModelKey currentModelKey = ProjectModelKey.NONE;

    OpenedProjectsSession(WebStudio studio, TestSuiteExecutor testSuiteExecutor, int maxModels) {
        this.compilationContext = new WorkspaceCompilationContext(studio);
        this.models = new ProjectModelRegistry(
                key -> new ProjectModel(studio, testSuiteExecutor, compilationContext),
                maxModels,
                projectModel -> projectModel.getCurrentCompilation().future().isDone());
    }

    /**
     * For tests only: inject the compilation context and model store directly.
     */
    OpenedProjectsSession(WorkspaceCompilationContext compilationContext, ProjectModelRegistry models) {
        this.compilationContext = compilationContext;
        this.models = models;
    }

    /**
     * The model of the current selection, for the JSF UI and other legacy callers. Never {@code null}: with no
     * selection it returns an empty model.
     */
    ProjectModel currentModel() {
        return models.getOrCreate(currentModelKey);
    }

    /**
     * The model for a specific opened project, independent of the current selection. Created on first access and
     * reused afterwards.
     */
    ProjectModel model(ProjectDescriptor descriptor, String repositoryId, String branch) {
        return models.getOrCreate(keyFor(descriptor, repositoryId, branch));
    }

    /**
     * The already-created model for a project, or {@code null} when it has not been opened. Matches by project
     * identity so it works for local-only projects too.
     */
    ProjectModel modelIfPresent(RulesProject project) {
        return models.findFirst(projectModel -> belongsTo(projectModel, project));
    }

    /**
     * Open (and compile if needed) a module in the project's own model, without changing the current selection,
     * so concurrent opens/edits of different projects do not clobber each other.
     */
    ProjectModel openModule(RulesProject project, ProjectDescriptor descriptor, Module module) {
        String repositoryId = project.getRepository().getId();
        ProjectModel projectModel = model(descriptor, repositoryId, project.getBranch());
        projectModel.bindRepositoryId(repositoryId);
        if (projectModel.getModuleInfo() != module) {
            try {
                projectModel.setModuleInfo(module);
            } catch (Exception e) {
                throw RuntimeExceptionWrapper.wrap(e);
            }
        }
        return projectModel;
    }

    /**
     * Make the given project/branch the current selection and pin its model so it is never evicted while in use.
     */
    void select(ProjectDescriptor descriptor, String repositoryId, String branch) {
        currentModelKey = keyFor(descriptor, repositoryId, branch);
        models.setPinned(currentModelKey);
    }

    void selectNone() {
        currentModelKey = ProjectModelKey.NONE;
        models.setPinned(ProjectModelKey.NONE);
    }

    /**
     * Clear the current selection and destroy its model. Used when the current project is no longer opened
     * (closed or removed externally), so its dependency manager and jars are released.
     */
    void dropCurrentSelection() {
        ProjectModelKey staleKey = currentModelKey;
        selectNone();
        models.remove(staleKey);
    }

    /**
     * Invalidate a project's compiled modules in the shared manager (so a later reopen recompiles from disk) and
     * drop its model. Used when a project is closed or switched to another branch/revision.
     */
    void evict(RulesProject project) {
        ProjectModel model = modelIfPresent(project);
        if (model != null && model.getModuleInfo() != null) {
            compilationContext.invalidate(model.getModuleInfo().getProject());
        }
        models.removeMatching(projectModel -> belongsTo(projectModel, project));
    }

    /**
     * Eagerly recompile a project's model in the background, if it is open. Picks up an on-disk change to the
     * project or to one it depends on while other opened projects keep their compiled state.
     */
    void recompile(RulesProject project) {
        ProjectModel model = modelIfPresent(project);
        if (model != null) {
            model.reloadAsync();
        }
    }

    /**
     * Drop models whose project is no longer opened (e.g. closed or removed externally), releasing their
     * resources. The pinned current model is kept.
     */
    void removeStaleModels() {
        models.removeMatching(projectModel -> {
            if (projectModel.getModuleInfo() == null) {
                return false;
            }
            RulesProject owner = projectModel.getProject();
            return owner == null || !owner.isOpened();
        });
    }

    void resetSourceModified() {
        models.forEach(ProjectModel::resetSourceModified);
    }

    /**
     * Drop every opened project's model and shut down the shared dependency manager. Whole-session reset/destroy
     * only — targeted edits use {@link #evict}/{@link #recompile} so other projects keep their compiled state.
     */
    void clear() {
        selectNone();
        models.clear();
        compilationContext.shutdown();
    }

    boolean sameProject(RulesProject a, RulesProject b) {
        return a.getRepository().getId().equals(b.getRepository().getId())
                && a.getBusinessName().equals(b.getBusinessName())
                && Objects.equals(a.getBranch(), b.getBranch());
    }

    private boolean belongsTo(ProjectModel projectModel, RulesProject project) {
        if (projectModel.getModuleInfo() == null) {
            return false;
        }
        RulesProject owner = projectModel.getProject();
        return owner != null && sameProject(owner, project);
    }

    private ProjectModelKey keyFor(ProjectDescriptor descriptor, String repositoryId, String branch) {
        return new ProjectModelKey(repositoryId, descriptor.getProjectFolder().getFileName().toString(), branch);
    }
}
