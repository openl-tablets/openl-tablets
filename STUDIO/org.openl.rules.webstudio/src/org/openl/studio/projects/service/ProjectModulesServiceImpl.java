package org.openl.studio.projects.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import org.openl.rules.cloner.Cloner;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.ProjectDescriptorManager;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.validation.ValidationException;
import org.openl.rules.project.resolving.InvalidFileNamePatternException;
import org.openl.rules.project.resolving.InvalidFileNameProcessorException;
import org.openl.rules.project.resolving.NoMatchFileNameException;
import org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy;
import org.openl.rules.project.resolving.PropertiesFileNameProcessorBuilder;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.rules.webstudio.web.util.ProjectArtifactUtils;
import org.openl.security.acl.permission.AclRole;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.projects.model.modules.BaseModuleView;
import org.openl.studio.projects.model.modules.CopyModuleRequest;
import org.openl.studio.projects.model.modules.CopyModuleResponse;
import org.openl.studio.projects.model.modules.MethodFilterView;
import org.openl.studio.projects.model.modules.ModuleView;
import org.openl.studio.projects.model.modules.WildcardModuleView;
import org.openl.util.CollectionUtils;
import org.openl.util.FileUtils;
import org.openl.util.StringUtils;

/**
 * Implementation of {@link ProjectModulesService} for listing and copying project modules.
 *
 * <p>Replaces legacy copy-module logic from {@code ProjectBean} with a REST-friendly service layer.</p>
 */
@Validated
@Service
public class ProjectModulesServiceImpl implements ProjectModulesService {

    private static final Logger LOG = LoggerFactory.getLogger(ProjectModulesServiceImpl.class);

    private final RepositoryAclService designRepositoryAclService;
    private final WorkspaceProjectService workspaceProjectService;
    private final ProjectDescriptorManager projectDescriptorManager = new ProjectDescriptorManager();

    public ProjectModulesServiceImpl(
            @Qualifier("designRepositoryAclService") RepositoryAclService designRepositoryAclService,
            WorkspaceProjectService workspaceProjectService) {
        this.designRepositoryAclService = designRepositoryAclService;
        this.workspaceProjectService = workspaceProjectService;
    }

    @Override
    @NotNull
    public List<ModuleView> getModules(@NotNull RulesProject project) {
        var resolvedDescriptor = workspaceProjectService.getProjectDescriptor(project);
        var originalDescriptor = getOriginalProjectDescriptor(resolvedDescriptor);

        var result = new ArrayList<ModuleView>();

        for (Module originalModule : originalDescriptor.getModules()) {
            if (projectDescriptorManager.isModuleWithWildcard(originalModule)) {
                var builder = mapModuleFields(new WildcardModuleView.Builder(), originalModule);
                try {
                    var matchedModules = projectDescriptorManager.getAllModulesMatchingPathPattern(
                            resolvedDescriptor, originalModule, originalModule.getRulesRootPath().getPath());
                    for (Module matched : matchedModules) {
                        builder.addMatchedModule(mapBaseFields(new BaseModuleView.Builder(), matched).build());
                    }
                } catch (IOException e) {
                    LOG.error("Failed to expand wildcard module pattern '{}'",
                            originalModule.getRulesRootPath().getPath(), e);
                }
                result.add(builder.build());
            } else {
                result.add(mapModuleFields(new ModuleView.Builder(), originalModule).build());
            }
        }

        return result;
    }

    @Override
    @NotNull
    public CopyModuleResponse copyModule(@NotNull RulesProject project,
                                         @NotBlank String moduleName,
                                         @Valid CopyModuleRequest request,
                                         boolean force) {
        var descriptor = workspaceProjectService.getProjectDescriptor(project);
        workspaceProjectService.getProjectModel(project); // Ensure model is loaded for module retrieval

        Module sourceModule = CollectionUtils.findFirst(descriptor.getModules(),
                module -> module.getName() != null && module.getName().equals(moduleName));
        if (sourceModule == null) {
            throw new NotFoundException("module.not.found.message", moduleName);
        }

        String sourcePath = sourceModule.getRulesRootPath().getPath();
        String newPath = resolveNewPath(request.newPath(), request.newModuleName(), sourcePath);

        var originalDescriptor = getOriginalProjectDescriptor(descriptor);
        boolean wildcardCovered = checkWildcardCoverage(originalDescriptor, newPath);
        String newModuleName = resolveNewModuleName(request.newModuleName(), newPath, wildcardCovered);

        lockProject(project);

        try {
            if (!wildcardCovered || StringUtils.isNotBlank(newModuleName)) {
                validateModuleName(descriptor, newModuleName);
            }
            validateModulePath(newPath, descriptor);
            if (!force) {
                validateFileNamePattern(originalDescriptor, newPath);
            }

            if (!wildcardCovered) {
                validateDescriptorPermissions(project);
            }
            validateCreatePermission(project, newPath);

            copySourceFile(project, sourcePath, newPath);
            project.setModified();

            if (!wildcardCovered) {
                registerModuleInDescriptor(project, originalDescriptor, newModuleName, newPath);
            } else {
                //refreshProject(project);
            }
        } catch (ConflictException | ForbiddenException | NotFoundException | BadRequestException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Error while copying module", e);
            throw new ConflictException("module.copy.failed.message");
        }

        return new CopyModuleResponse(newModuleName, newPath, wildcardCovered);
    }

    /**
     * Resolves the project descriptor for the given project from WebStudio.
     *
     * @param studio the current WebStudio session
     * @param project the rules project
     * @return the project descriptor
     */
    private ProjectDescriptor getProjectDescriptor(WebStudio studio, RulesProject project) {
        return studio.getProjectByName(project.getRepository().getId(), project.getName());
    }

    /**
     * Maps base module fields (name, path) from a domain {@link Module} to the given builder.
     *
     * @param builder the target builder
     * @param module the source domain module
     * @param <T> the builder type
     * @return the same builder with base fields populated
     */
    private <T extends BaseModuleView.ABuilder<T>> T mapBaseFields(T builder, Module module) {
        String path = module.getRulesRootPath() != null ? module.getRulesRootPath().getPath() : null;
        return builder.name(module.getName()).path(path);
    }

    /**
     * Maps module definition fields (name, path, methodFilter) from a domain {@link Module}
     * to the given builder.
     *
     * @param builder the target builder (either {@link ModuleView.Builder} or {@link WildcardModuleView.Builder})
     * @param module the source domain module
     * @param <T> the builder type
     * @return the same builder with fields populated
     */
    private <T extends ModuleView.ABuilder<T>> T mapModuleFields(T builder, Module module) {
        mapBaseFields(builder, module);
        if (module.getMethodFilter() != null) {
            builder.methodFilter(MethodFilterView.builder()
                    .includes(module.getMethodFilter().getIncludes())
                    .excludes(module.getMethodFilter().getExcludes())
                    .build());
        }
        return builder;
    }

    /**
     * Resolves the target file path. If newPath is provided, uses it directly.
     * Otherwise calculates from source path directory + new module name + source extension.
     */
    private String resolveNewPath(String newPath, String newModuleName, String sourcePath) {
        if (StringUtils.isNotBlank(newPath)) {
            return newPath;
        }
        if (StringUtils.isBlank(newModuleName)) {
            throw new BadRequestException("cannot.be.empty.message");
        }
        // Calculate from source path: take directory + newName + extension
        String dir = "";
        int lastSlash = sourcePath.replace("\\", "/").lastIndexOf('/');
        if (lastSlash >= 0) {
            dir = sourcePath.substring(0, lastSlash + 1);
        }
        String ext = FileUtils.getExtension(sourcePath);
        String extension = StringUtils.isNotBlank(ext) ? "." + ext : "";
        return dir + newModuleName + extension;
    }

    private boolean checkWildcardCoverage(ProjectDescriptor originalDescriptor, String newPath) {
        Module tempModule = new Module();
        tempModule.setRulesRootPath(new PathEntry(newPath));
        return projectDescriptorManager.isCoveredByWildcardModule(originalDescriptor, tempModule);
    }

    private String resolveNewModuleName(String requestedName, String newPath, boolean wildcardCovered) {
        if (StringUtils.isNotBlank(requestedName)) {
            return requestedName;
        }
        if (wildcardCovered) {
            // Derive name from path
            return FileUtils.getBaseName(newPath);
        }
        throw new BadRequestException("cannot.be.empty.message");
    }

    private void validateModuleName(ProjectDescriptor descriptor, String newName) {
        if (StringUtils.isBlank(newName)) {
            throw new BadRequestException("cannot.be.empty.message");
        }
        if (!NameChecker.checkName(newName)) {
            throw new BadRequestException("invalid.name.message");
        }
        // Check uniqueness
        Module existing = CollectionUtils.findFirst(descriptor.getModules(),
                m -> m.getName() != null && m.getName().equals(newName));
        if (existing != null) {
            throw new ConflictException("module.name.exists.message");
        }
    }

    private void validateModulePath(String path, ProjectDescriptor descriptor) {
        if (StringUtils.isBlank(path)) {
            throw new BadRequestException("cannot.be.empty.message");
        }
        if (path.contains("*") || path.contains("?")) {
            throw new BadRequestException("module.path.wildcard.message");
        }
        try {
            NameChecker.validatePath(path);
        } catch (IOException e) {
            throw new BadRequestException("module.path.invalid.message", new Object[]{path, e.getMessage()});
        }
        Path moduleFile = descriptor.getProjectFolder().resolve(path);
        if (Files.exists(moduleFile)) {
            throw new ConflictException("module.file.exists.message");
        }
    }

    /**
     * Validates that the new file path matches the project's properties file name patterns.
     * Silently skips if the patterns cannot be parsed.
     */
    private void validateFileNamePattern(ProjectDescriptor descriptor, String newPath) {
        String[] patterns = descriptor.getPropertiesFileNamePatterns();
        if (patterns == null) {
            return;
        }
        try {
            new PropertiesFileNameProcessorBuilder().build(descriptor).process(newPath);
        } catch (NoMatchFileNameException e) {
            throw new BadRequestException("module.path.pattern.mismatch.message");
        } catch (InvalidFileNameProcessorException | InvalidFileNamePatternException ignored) {
            // Cannot check for name correctness — skip validation (same as legacy)
        }
    }

    private void lockProject(RulesProject project) {
        if (!project.tryLock()) {
            throw new ConflictException("project.locked.message");
        }
    }

    private void validateDescriptorPermissions(RulesProject project) {
        if (project.hasArtefact(ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME)) {
            try {
                AProjectArtefact artefact = project
                        .getArtefact(ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME);
                if (!designRepositoryAclService.isGranted(artefact, List.of(BasePermission.WRITE))) {
                    throw new ForbiddenException();
                }
            } catch (ProjectException ignored) {
                // Artefact access failed — skip permission check
            }
        }
    }

    private void validateCreatePermission(RulesProject project, String path) {
        String p = designRepositoryAclService.getPath(project);
        if (!designRepositoryAclService
                .isGranted(project.getRepository().getId(), p + "/" + path, List.of(BasePermission.CREATE))) {
            throw new ForbiddenException();
        }
    }

    /**
     * Copies the source file and sets ACL on the new resource.
     */
    private void copySourceFile(RulesProject project, String sourcePath, String newPath) {
        AProjectResource sourceResource;
        try {
            sourceResource = (AProjectResource) project.getArtefact(sourcePath);
        } catch (ProjectException e) {
            throw new NotFoundException("module.file.not.found.message");
        }
        try {
            AProjectResource newResource = project.addResource(newPath, sourceResource.getContent());
            if (!designRepositoryAclService.hasAcl(newResource) && !designRepositoryAclService
                    .createAcl(newResource, List.of(AclRole.CONTRIBUTOR.getCumulativePermission()), true)) {
                LOG.warn("Granting permissions to a new file '{}' is failed.",
                        ProjectArtifactUtils.extractResourceName(newResource));
            }
        } catch (ProjectException e) {
            throw new ConflictException("module.copy.failed.message");
        }
    }

    /**
     * Adds the new module to the project descriptor and saves it.
     */
    private void registerModuleInDescriptor(RulesProject project,
                                            ProjectDescriptor originalDescriptor,
                                            String newModuleName,
                                            String newPath) {
        ProjectDescriptor newDescriptor = Cloner.clone(originalDescriptor);
        Module newModule = new Module();
        newModule.setName(newModuleName);
        newModule.setRulesRootPath(new PathEntry(newPath));
        newModule.setProject(newDescriptor);
        newDescriptor.getModules().add(newModule);

        cleanDescriptor(newDescriptor);
        saveDescriptor(project, newDescriptor);
        //refreshProject(project);
    }

    /**
     * Refreshes the project in WebStudio after a module change.
     *
     * @param project the rules project used to resolve the descriptor
     */
    private void refreshProject(RulesProject project) {
        workspaceProjectService.getProjectModel(project); // Ensure model is loaded before refresh
        var studio = workspaceProjectService.getWebStudio();
        studio.getModel().clearModuleInfo();
        var repoId = project.getRepository().getId();
        ProjectDescriptor oldDescriptor = getProjectDescriptor(studio, project);
        ProjectDescriptor newDescriptor = studio.resolveProject(oldDescriptor);
        studio.forceUpdateProjectDescriptor(repoId, newDescriptor, oldDescriptor);
    }

    /**
     * Reads the original (unresolved) project descriptor from the rules.xml file.
     *
     * @param descriptor the resolved project descriptor (used for project folder location)
     * @return the original unresolved descriptor, or the given descriptor if reading fails
     */
    private ProjectDescriptor getOriginalProjectDescriptor(ProjectDescriptor descriptor) {
        try {
            File file = descriptor.getProjectFolder()
                    .resolve(ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME)
                    .toFile();
            return projectDescriptorManager.readOriginalDescriptor(file);
        } catch (FileNotFoundException ignored) {
            return descriptor;
        } catch (IOException | ValidationException | JAXBException e) {
            LOG.error(e.getMessage(), e);
            return descriptor;
        }
    }

    /**
     * Nullifies empty collections in the descriptor before serialization.
     * Replicates {@code ProjectBean.clean()} behavior.
     */
    private void cleanDescriptor(ProjectDescriptor descriptor) {
        if (CollectionUtils.isEmpty(descriptor.getClasspath())) {
            descriptor.setClasspath(null);
        }
        if (CollectionUtils.isEmpty(descriptor.getPropertiesFileNamePatterns())) {
            descriptor.setPropertiesFileNamePatterns(null);
        }
        if (StringUtils.isBlank(descriptor.getPropertiesFileNameProcessor())) {
            descriptor.setPropertiesFileNameProcessor(null);
        }
        if (CollectionUtils.isEmpty(descriptor.getDependencies())) {
            descriptor.setDependencies(null);
        }

        var openapi = descriptor.getOpenapi();
        if (openapi != null) {
            if (StringUtils.isBlank(openapi.getPath())) {
                openapi.setPath(null);
            }
            if (StringUtils.isBlank(openapi.getAlgorithmModuleName())) {
                openapi.setAlgorithmModuleName(null);
            }
            if (StringUtils.isBlank(openapi.getModelModuleName())) {
                openapi.setModelModuleName(null);
            }
        }

        List<Module> modules = descriptor.getModules();
        if (CollectionUtils.isEmpty(modules)) {
            descriptor.setModules(null);
            return;
        }

        for (Module module : modules) {
            PathEntry rulesRootPath = module.getRulesRootPath();
            if (rulesRootPath != null) {
                if (StringUtils.isNotBlank(rulesRootPath.getPath())) {
                    rulesRootPath.setPath(rulesRootPath.getPath().replace("\\", "/"));
                } else {
                    module.setRulesRootPath(null);
                }
            }

            var methodFilter = module.getMethodFilter();
            if (methodFilter != null) {
                if (CollectionUtils.isEmpty(methodFilter.getIncludes()) && CollectionUtils
                        .isEmpty(methodFilter.getExcludes())) {
                    module.setMethodFilter(null);
                } else if (CollectionUtils.isEmpty(methodFilter.getIncludes())) {
                    methodFilter.setIncludes(null);
                } else if (CollectionUtils.isEmpty(methodFilter.getExcludes())) {
                    methodFilter.setExcludes(null);
                }
            }
        }

        descriptor.setProjectFolder(null);
    }

    /**
     * Serializes the project descriptor and writes it to the project.
     */
    private void saveDescriptor(RulesProject project, ProjectDescriptor descriptor) {
        try {
            var outputStream = new ByteArrayOutputStream();
            projectDescriptorManager.writeDescriptor(descriptor, outputStream);
            var inputStream = new ByteArrayInputStream(outputStream.toByteArray());

            if (project.hasArtefact(ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME)) {
                AProjectResource artifact = (AProjectResource) project
                        .getArtefact(ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME);
                artifact.setContent(inputStream);
            } else {
                project.addResource(ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME, inputStream);
                AProjectArtefact projectArtefact = project
                        .getArtefact(ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME);
                if (!designRepositoryAclService.hasAcl(projectArtefact) && !designRepositoryAclService
                        .createAcl(projectArtefact, List.of(AclRole.CONTRIBUTOR.getCumulativePermission()), true)) {
                    LOG.warn("Granting permissions to a new file '{}' is failed.",
                            ProjectArtifactUtils.extractResourceName(projectArtefact));
                }
            }
        } catch (ValidationException e) {
            throw new BadRequestException("module.copy.failed.message");
        } catch (Exception e) {
            throw new ConflictException("module.copy.failed.message");
        }
    }
}
