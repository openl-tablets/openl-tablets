package org.openl.studio.projects.service.project.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import jakarta.annotation.Nullable;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.instantiation.IDependencyLoader;
import org.openl.rules.project.model.Module;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.ui.ProjectCompilationStatus;
import org.openl.rules.ui.ProjectModel;
import org.openl.studio.common.utils.DateTimes;
import org.openl.studio.projects.model.project.status.CompilationDetails;
import org.openl.studio.projects.model.project.status.CompilationMessages;
import org.openl.studio.projects.model.project.status.CompilationModules;
import org.openl.studio.projects.model.project.status.CompilationTests;
import org.openl.studio.projects.model.project.status.CompileState;
import org.openl.studio.projects.model.project.status.ModifiedBy;
import org.openl.studio.projects.model.project.status.ProjectStatusViewModel;
import org.openl.studio.projects.service.DetailedMessageDescriptionMapper;
import org.openl.studio.projects.service.ProjectIdentifierMapper;
import org.openl.studio.projects.service.project.changes.PendingChangesResolver;
import org.openl.studio.projects.service.project.compile.CompilationJob;
import org.openl.studio.projects.service.project.compile.CompilationJobRegistry;

@Service
@RequiredArgsConstructor
public class ProjectStatusMapperImpl implements ProjectStatusMapper {

    private final ProjectIdentifierMapper projectIdentifierMapper;
    private final CompilationJobRegistry compilationJobRegistry;
    private final PendingChangesResolver pendingChangesResolver;
    private final DetailedMessageDescriptionMapper detailedMessageDescriptionMapper;

    /** How much of the status is asked for, and so how much work building it is worth. */
    private enum Detail {

        /** Everything the project screens show. */
        FULL(true, true, true, true),
        /** What a projects-list row shows: the counts, without the message list or the module names. */
        SUMMARY(false, false, true, true),
        /**
         * What a running compilation can tell about itself: how far it has come.
         *
         * <p>Resolving every message to its table reads the model under the same lock the compilation holds,
         * the changes not committed yet are read from disk, and counting the tests walks every method of what
         * is compiled so far — none of it is worth waiting for to say that three modules of twelve are built.
         * What is left out is carried by the full status that follows the compilation.
         */
        PROGRESS(false, true, false, false);

        private final boolean messageItems;
        private final boolean moduleNames;
        private final boolean pendingChanges;
        private final boolean tests;

        Detail(boolean messageItems, boolean moduleNames, boolean pendingChanges, boolean tests) {
            this.messageItems = messageItems;
            this.moduleNames = moduleNames;
            this.pendingChanges = pendingChanges;
            this.tests = tests;
        }
    }

    @Override
    public ProjectStatusViewModel map(RulesProject project) {
        return map(project, resolveModel(project), Detail.FULL);
    }

    @Override
    public ProjectStatusViewModel mapSummary(RulesProject project) {
        return map(project, resolveModel(project), Detail.SUMMARY);
    }

    @Override
    public ProjectStatusViewModel map(RulesProject project, @Nullable ProjectModel model) {
        return map(project, model, Detail.FULL);
    }

    @Override
    public ProjectStatusViewModel mapProgress(RulesProject project, @Nullable ProjectModel model) {
        return map(project, model, Detail.PROGRESS);
    }

    // Read-only check: do not initiate any compilation. The status endpoint must only
    // report whatever is already registered in the session-scoped compilation registry.
    @Nullable
    private ProjectModel resolveModel(RulesProject project) {
        var projectId = projectIdentifierMapper.map(project);
        return compilationJobRegistry.find(projectId, project.getBranch())
                .map(CompilationJob::project)
                .orElse(null);
    }

    private ProjectStatusViewModel map(RulesProject project, @Nullable ProjectModel model, Detail detail) {
        var projectId = projectIdentifierMapper.map(project);
        var builder = ProjectStatusViewModel.builder()
                .projectId(projectId);
        if (project.isSupportsBranches()) {
            builder.branch(project.getBranch());
        }
        Optional.ofNullable(project.getFileData()).ifPresent(fileData -> {
            Optional.ofNullable(fileData.getVersion()).ifPresent(builder::revision);
            builder.lastModifiedBy(mapLastModifiedBy(fileData));
        });
        if (model == null) {
            builder.compileState(CompileState.IDLE);
        } else {
            var compilationStatus = model.getCompilationStatus();
            builder.compileState(deriveCompileState(model, compilationStatus));
            builder.compilation(mapCompilationDetails(model, compilationStatus, detail));
        }
        if (detail.pendingChanges) {
            builder.pendingChanges(pendingChangesResolver.resolve(project));
        }
        return builder.build();
    }

    private ModifiedBy mapLastModifiedBy(FileData fileData) {
        var authorBuilder = ModifiedBy.builder();
        Optional.ofNullable(fileData.getAuthor())
                .map(UserInfo::getName)
                .ifPresent(authorBuilder::author);
        Optional.ofNullable(fileData.getModifiedAt())
                .map(DateTimes::atSystemZone)
                .ifPresent(authorBuilder::date);
        return authorBuilder.build();
    }

    private CompileState deriveCompileState(ProjectModel projectModel, ProjectCompilationStatus compilationStatus) {
        if (projectModel.isCompilationInProgress() || !isCompilationCompleted(projectModel)) {
            return CompileState.COMPILING;
        }
        if (compilationStatus.getErrorsCount() > 0) {
            return CompileState.ERRORS;
        }
        if (compilationStatus.getWarningsCount() > 0) {
            return CompileState.WARNINGS;
        }
        return CompileState.OK;
    }

    /**
     * Compilation is considered finished when the project-wide flag is set OR when the
     * current module is configured as "compile this module only" — single-module compiles
     * finish synchronously in {@code setModuleInfo} and never flip the project-wide flag.
     */
    private static boolean isCompilationCompleted(ProjectModel projectModel) {
        if (projectModel.isProjectCompilationCompleted()) {
            return true;
        }
        var moduleInfo = projectModel.getModuleInfo();
        return moduleInfo != null
                && moduleInfo.getWebstudioConfiguration() != null
                && moduleInfo.getWebstudioConfiguration().isCompileThisModuleOnly();
    }

    private CompilationDetails mapCompilationDetails(ProjectModel projectModel,
                                                     ProjectCompilationStatus compilationStatus,
                                                     Detail detail) {
        return CompilationDetails.builder()
                .messages(mapMessages(projectModel, compilationStatus, detail.messageItems))
                .modules(mapModules(projectModel, compilationStatus, detail.moduleNames))
                .tests(detail.tests ? mapTests(projectModel) : null)
                .build();
    }

    private CompilationTests mapTests(ProjectModel projectModel) {
        var testMethods = projectModel.getAllTestMethods();
        return CompilationTests.builder()
                .total(testMethods == null ? 0 : testMethods.length)
                .build();
    }

    private CompilationMessages mapMessages(ProjectModel projectModel,
                                            ProjectCompilationStatus compilationStatus,
                                            boolean includeItems) {
        var allMessages = compilationStatus.getAllMessage();
        var builder = CompilationMessages.builder()
                .total(allMessages.size())
                .errors(compilationStatus.getErrorsCount())
                .warnings(compilationStatus.getWarningsCount());
        // The projects list shows only the counts; resolving each message to its table and module is
        // the expensive part, so the list build (mapSummary) skips it and leaves items unset.
        if (includeItems) {
            builder.items(detailedMessageDescriptionMapper.mapSorted(allMessages, projectModel));
        }
        return builder.build();
    }

    private CompilationModules mapModules(ProjectModel projectModel, ProjectCompilationStatus compilationStatus,
                                          boolean includeNames) {
        var total = compilationStatus.getModulesCount();
        if (total == 0) {
            return CompilationModules.empty();
        }
        var builder = CompilationModules.builder()
                .total(total)
                .compiled(compilationStatus.getModulesCompiled());
        // The projects list shows only the counts; the compiled-module names require walking the loader
        // graph, so the list build (mapSummary) skips them.
        if (includeNames) {
            builder.compiledModules(resolveCompiledModuleNames(projectModel));
        }
        return builder.build();
    }

    private static List<String> resolveCompiledModuleNames(ProjectModel projectModel) {
        var moduleInfo = projectModel.getModuleInfo();
        // Single-module compile path: the opened module is the whole cycle, so it is named once it is built.
        if (moduleInfo != null
                && moduleInfo.getWebstudioConfiguration() != null
                && moduleInfo.getWebstudioConfiguration().isCompileThisModuleOnly()) {
            return projectModel.isOpenedModuleCompiled() ? List.of(moduleInfo.getName()) : List.of();
        }
        return collectCompiledModuleNames(projectModel, moduleInfo);
    }

    private static List<String> collectCompiledModuleNames(ProjectModel projectModel, @Nullable Module currentModule) {
        var dependencyManager = projectModel.getWebStudioWorkspaceDependencyManager();
        if (dependencyManager == null || currentModule == null) {
            return List.of();
        }
        var loaders = dependencyManager.findAllProjectDependencyLoaders(currentModule.getProject());
        if (loaders == null || loaders.isEmpty()) {
            return List.of();
        }
        var compiled = new ArrayList<String>();
        var projectCompilationCompleted = projectModel.isProjectCompilationCompleted();
        var openedModuleCompiled = projectModel.isOpenedModuleCompiled();
        for (IDependencyLoader loader : loaders) {
            if (!loader.isProjectLoader()) {
                if (isCompiled(loader, currentModule, projectCompilationCompleted, openedModuleCompiled)) {
                    compiled.add(loader.getModule().getName());
                }
            }
        }
        return List.copyOf(compiled);
    }

    private static boolean isCompiled(IDependencyLoader loader,
                                      Module currentModule,
                                      boolean projectCompilationCompleted,
                                      boolean openedModuleCompiled) {
        // Once the project-wide flag flips, every module loader has its compiled dependency
        // attached, so the ref-based check below is also true here — kept as an explicit
        // shortcut.
        if (projectCompilationCompleted) {
            return true;
        }
        var loaderModule = loader.getModule();
        // The opened module's compilation finishes inside setModuleInfo and its result is stored on the model
        // as openedModuleCompiledOpenClass rather than on the loader, so the model is asked about it. Until it
        // answers, that module is being compiled — which is the very thing a reader is waiting to stop.
        if (Objects.equals(loaderModule.getName(), currentModule.getName())
                && Objects.equals(loader.getProject(), currentModule.getProject())) {
            return openedModuleCompiled;
        }
        return loader.getRefToCompiledDependency() != null;
    }

}
