package org.openl.studio.projects.service.project.status;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
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

    @Override
    public ProjectStatusViewModel map(RulesProject project) {
        // Read-only check: do not initiate any compilation. The status endpoint must only
        // report whatever is already registered in the session-scoped compilation registry.
        var projectId = projectIdentifierMapper.map(project);
        var model = compilationJobRegistry.find(projectId, project.getBranch())
                .map(CompilationJob::project)
                .orElse(null);
        return map(project, model);
    }

    @Override
    public ProjectStatusViewModel map(RulesProject project, @Nullable ProjectModel model) {
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
            builder.compilation(mapCompilationDetails(model, compilationStatus));
        }
        builder.pendingChanges(pendingChangesResolver.resolve(project));
        return builder.build();
    }

    private ModifiedBy mapLastModifiedBy(FileData fileData) {
        var authorBuilder = ModifiedBy.builder();
        Optional.ofNullable(fileData.getAuthor())
                .map(UserInfo::getName)
                .ifPresent(authorBuilder::author);
        Optional.ofNullable(fileData.getModifiedAt())
                .map(Date::toInstant)
                .map(instant -> ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()))
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
                                                     ProjectCompilationStatus compilationStatus) {
        return CompilationDetails.builder()
                .messages(mapMessages(projectModel, compilationStatus))
                .modules(mapModules(projectModel, compilationStatus))
                .tests(mapTests(projectModel))
                .build();
    }

    private CompilationTests mapTests(ProjectModel projectModel) {
        var testMethods = projectModel.getAllTestMethods();
        return CompilationTests.builder()
                .total(testMethods == null ? 0 : testMethods.length)
                .build();
    }

    private CompilationMessages mapMessages(ProjectModel projectModel, ProjectCompilationStatus compilationStatus) {
        var ordered = detailedMessageDescriptionMapper.mapSorted(compilationStatus.getAllMessage(), projectModel);
        return CompilationMessages.builder()
                .items(ordered)
                .total(ordered.size())
                .errors(compilationStatus.getErrorsCount())
                .warnings(compilationStatus.getWarningsCount())
                .build();
    }

    private CompilationModules mapModules(ProjectModel projectModel, ProjectCompilationStatus compilationStatus) {
        var total = compilationStatus.getModulesCount();
        if (total == 0) {
            return CompilationModules.empty();
        }
        var moduleInfo = projectModel.getModuleInfo();
        // Single-module compile path: only the opened module is in the cycle and it's done
        // synchronously inside setModuleInfo, so no need to walk the loader graph.
        if (moduleInfo != null
                && moduleInfo.getWebstudioConfiguration() != null
                && moduleInfo.getWebstudioConfiguration().isCompileThisModuleOnly()) {
            return CompilationModules.builder()
                    .compiledModules(List.of(moduleInfo.getName()))
                    .total(total)
                    .compiled(compilationStatus.getModulesCompiled())
                    .build();
        }
        return CompilationModules.builder()
                .compiledModules(collectCompiledModuleNames(projectModel, moduleInfo))
                .total(total)
                .compiled(compilationStatus.getModulesCompiled())
                .build();
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
        for (IDependencyLoader loader : loaders) {
            if (loader.isProjectLoader()) {
                continue;
            }
            var loaderModule = loader.getModule();
            if (loaderModule == null || loaderModule.getName() == null) {
                continue;
            }
            if (isCompiled(loader, loaderModule, currentModule, projectCompilationCompleted)) {
                compiled.add(loaderModule.getName());
            }
        }
        return List.copyOf(compiled);
    }

    private static boolean isCompiled(IDependencyLoader loader,
                                      Module loaderModule,
                                      Module currentModule,
                                      boolean projectCompilationCompleted) {
        // Once the project-wide flag flips, every module loader has its compiled dependency
        // attached, so the ref-based check below is also true here — kept as an explicit
        // shortcut.
        if (projectCompilationCompleted) {
            return true;
        }
        // The opened module's compilation finishes synchronously in setModuleInfo and its
        // result is stored on the model as openedModuleCompiledOpenClass rather than on
        // the loader, so it is counted via identity match.
        if (Objects.equals(loaderModule.getName(), currentModule.getName())
                && Objects.equals(loader.getProject(), currentModule.getProject())) {
            return true;
        }
        return loader.getRefToCompiledDependency() != null;
    }

}
