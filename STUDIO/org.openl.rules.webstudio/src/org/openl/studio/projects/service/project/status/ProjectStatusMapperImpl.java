package org.openl.studio.projects.service.project.status;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import jakarta.annotation.Nullable;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.instantiation.IDependencyLoader;
import org.openl.rules.project.model.Module;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.ui.ProjectModel;
import org.openl.studio.projects.model.project.status.CompilationDetails;
import org.openl.studio.projects.model.project.status.CompilationMessages;
import org.openl.studio.projects.model.project.status.CompilationModules;
import org.openl.studio.projects.model.project.status.CompilationTables;
import org.openl.studio.projects.model.project.status.CompilationTests;
import org.openl.studio.projects.model.project.status.CompileState;
import org.openl.studio.projects.model.project.status.ModifiedBy;
import org.openl.studio.projects.model.project.status.ProjectStatusViewModel;
import org.openl.studio.projects.service.MessageDescriptionMapper;
import org.openl.studio.projects.service.ProjectIdentifierMapper;
import org.openl.studio.projects.service.project.changes.PendingChangesResolver;
import org.openl.studio.projects.service.project.compile.CompilationJobRegistry;

@Service
@RequiredArgsConstructor
public class ProjectStatusMapperImpl implements ProjectStatusMapper {

    private final ProjectIdentifierMapper projectIdentifierMapper;
    private final CompilationJobRegistry compilationJobRegistry;
    private final PendingChangesResolver pendingChangesResolver;
    private final MessageDescriptionMapper messageDescriptionMapper;

    @Override
    public ProjectStatusViewModel map(RulesProject project) {
        // Read-only check: do not initiate any compilation. The status endpoint must only
        // report whatever is already registered in the session-scoped compilation registry.
        var projectId = projectIdentifierMapper.map(project);
        var model = compilationJobRegistry.find(projectId, project.getBranch())
                .map(job -> job.project())
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
            var moduleMessages = model.getModuleMessages();
            builder.compileState(deriveCompileState(model, moduleMessages));
            builder.compilation(mapCompilationDetails(model, moduleMessages));
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

    private CompileState deriveCompileState(ProjectModel projectModel,
                                            Collection<OpenLMessage> messages) {
        if (projectModel.isCompilationInProgress() || !isCompilationCompleted(projectModel)) {
            return CompileState.COMPILING;
        }
        var hasWarnings = false;
        for (var message : messages) {
            var severity = message.getSeverity();
            if (severity == Severity.ERROR) {
                return CompileState.ERRORS;
            }
            if (severity == Severity.WARN) {
                hasWarnings = true;
            }
        }
        return hasWarnings ? CompileState.WARNINGS : CompileState.OK;
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
                                                     Collection<OpenLMessage> moduleMessages) {
        return CompilationDetails.builder()
                .messages(mapMessages(moduleMessages))
                .modules(mapModules(projectModel))
                .tests(mapTests(projectModel))
                .tables(mapTables(projectModel))
                .build();
    }

    private CompilationTests mapTests(ProjectModel projectModel) {
        var testMethods = projectModel.getAllTestMethods();
        return CompilationTests.builder()
                .total(testMethods == null ? 0 : testMethods.length)
                .build();
    }

    /**
     * Populated only when compilation is done — until then the table tree is incomplete and
     * the numbers would change with every poll.
     */
    private CompilationTables mapTables(ProjectModel projectModel) {
        if (!isCompilationCompleted(projectModel)) {
            return null;
        }
        return CompilationTables.builder()
                .total(projectModel.getNumberOfTables())
                .errors(projectModel.getErrorNodesNumber())
                .build();
    }

    private CompilationMessages mapMessages(Collection<OpenLMessage> moduleMessages) {
        var ordered = messageDescriptionMapper.mapSorted(moduleMessages);
        var errors = 0;
        var warnings = 0;
        for (var message : ordered) {
            if (message.severity() == Severity.ERROR) {
                errors++;
            } else if (message.severity() == Severity.WARN) {
                warnings++;
            }
        }
        return CompilationMessages.builder()
                .items(ordered)
                .total(ordered.size())
                .errors(errors)
                .warnings(warnings)
                .build();
    }

    private CompilationModules mapModules(ProjectModel projectModel) {
        var moduleInfo = projectModel.getModuleInfo();
        var dependencyManager = projectModel.getWebStudioWorkspaceDependencyManager();
        if (moduleInfo == null || dependencyManager == null) {
            return CompilationModules.empty();
        }
        if (moduleInfo.getWebstudioConfiguration() != null
                && moduleInfo.getWebstudioConfiguration().isCompileThisModuleOnly()) {
            // Single-module compile path: only this module participates in the cycle and it
            // is considered compiled (the synchronous loadDependency in setModuleInfo finished
            // before the model exposed the cycle to observers).
            return CompilationModules.builder()
                    .compiledModules(List.of(moduleInfo.getName()))
                    .total(1)
                    .compiled(1)
                    .build();
        }
        var dependencyLoaders = dependencyManager.findAllProjectDependencyLoaders(moduleInfo.getProject());
        if (dependencyLoaders == null || dependencyLoaders.isEmpty()) {
            return CompilationModules.empty();
        }
        var total = 0;
        var compiledNames = new java.util.ArrayList<String>();
        if (projectModel.isProjectCompilationCompleted()) {
            // Project compilation finished — count modules via the project loaders only.
            for (var loader : dependencyLoaders) {
                if (!loader.isProjectLoader()) {
                    continue;
                }
                for (var module : loader.getProject().getModules()) {
                    total++;
                    if (module.getName() != null) {
                        compiledNames.add(module.getName());
                    }
                }
            }
        } else {
            // In progress — count each module loader as one module; compiled when it is the
            // opened module or has a compiled dependency reference.
            for (var loader : dependencyLoaders) {
                if (loader.isProjectLoader()) {
                    continue;
                }
                total++;
                var loaderModule = loader.getModule();
                if (loaderModule == null) {
                    continue;
                }
                if (isCompiledNonCompleted(loader, loaderModule, moduleInfo)) {
                    compiledNames.add(loaderModule.getName());
                }
            }
        }
        return CompilationModules.builder()
                .compiledModules(List.copyOf(compiledNames))
                .total(total)
                .compiled(compiledNames.size())
                .build();
    }

    private static boolean isCompiledNonCompleted(IDependencyLoader loader,
                                                  Module loaderModule,
                                                  Module currentModule) {
        if (Objects.equals(loaderModule.getName(), currentModule.getName())
                && Objects.equals(loader.getProject(), currentModule.getProject())) {
            // Current opened module is always counted as compiled while the cycle is in
            // progress — its openedModuleCompiledOpenClass was attached by setModuleInfo.
            return true;
        }
        return loader.getRefToCompiledDependency() != null;
    }

}
