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
import java.util.Objects;
import java.util.Set;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.JAXBException;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
import org.openl.rules.project.model.MethodFilter;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.WebstudioConfiguration;
import org.openl.rules.project.model.validation.ValidationException;
import org.openl.rules.project.resolving.InvalidFileNamePatternException;
import org.openl.rules.project.resolving.InvalidFileNameProcessorException;
import org.openl.rules.project.resolving.NoMatchFileNameException;
import org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy;
import org.openl.rules.project.resolving.PropertiesFileNameProcessorBuilder;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.rules.webstudio.web.util.ProjectArtifactUtils;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.projects.model.modules.BaseModuleView;
import org.openl.studio.projects.model.modules.CopyModuleRequest;
import org.openl.studio.projects.model.modules.CopyModuleResponse;
import org.openl.studio.projects.model.modules.EditModuleRequest;
import org.openl.studio.projects.model.modules.MethodFilterView;
import org.openl.studio.projects.model.modules.ModuleView;
import org.openl.studio.projects.model.modules.WildcardModuleView;
import org.openl.util.CollectionUtils;
import org.openl.util.FileUtils;
import org.openl.util.StringUtils;

/**
 * Implementation of {@link ProjectModulesService} for project module operations.
 *
 * <p>Replaces legacy module management logic from {@code ProjectBean} with a REST-friendly service layer.</p>
 */
@Validated
@Service
public class ProjectModulesServiceImpl implements ProjectModulesService {

    private static final Logger LOG = LoggerFactory.getLogger(ProjectModulesServiceImpl.class);

    private final RepositoryAclService designRepositoryAclService;
    private final AclProjectsHelper aclProjectsHelper;
    private final WorkspaceProjectService workspaceProjectService;
    private final ProjectDescriptorManager projectDescriptorManager = new ProjectDescriptorManager();

    public ProjectModulesServiceImpl(
            @Qualifier("designRepositoryAclService") RepositoryAclService designRepositoryAclService,
            AclProjectsHelper aclProjectsHelper,
            WorkspaceProjectService workspaceProjectService) {
        this.designRepositoryAclService = designRepositoryAclService;
        this.aclProjectsHelper = aclProjectsHelper;
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

        if (!wildcardCovered || StringUtils.isNotBlank(newModuleName)) {
            validateModuleName(descriptor, newModuleName);
        }
        validateModulePath(newPath, descriptor);
        if (!force) {
            validateFileNamePattern(originalDescriptor, newPath);
        }

        if (!wildcardCovered) {
            validateDescriptorPermissions(project, true);
        }
        validateCreatePermission(project, newPath);

        copySourceFile(project, sourcePath, newPath);
        project.setModified();

        if (!wildcardCovered) {
            registerModuleInDescriptor(project, originalDescriptor, newModuleName, newPath);
        }

        return new CopyModuleResponse(newModuleName, newPath, wildcardCovered);
    }

    @Override
    @NotNull
    public ModuleView addModule(@NotNull RulesProject project, @Valid EditModuleRequest request) {
        validateProjectWritePermission(project);

        var resolvedDescriptor = workspaceProjectService.getProjectDescriptor(project);
        var originalDescriptor = getOriginalProjectDescriptor(resolvedDescriptor);

        boolean shouldCreateFile = Boolean.TRUE.equals(request.createFile());

        lockProject(project);

        validateDescriptorPermissions(project, true);
        validateModuleNameForEdit(request.name(), null, request.path(), resolvedDescriptor);
        validateModulePathForEdit(request.path(), resolvedDescriptor, null, shouldCreateFile);

        if (shouldCreateFile) {
            createEmptyExcelFile(project, request.path());
        }

        ProjectDescriptor newDescriptor = Cloner.clone(originalDescriptor);
        Module module = new Module();
        module.setProject(newDescriptor);
        newDescriptor.getModules().add(module);

        applyModuleProperties(module, request);

        cleanDescriptor(newDescriptor);
        saveDescriptor(project, newDescriptor);

        return mapModuleFields(new ModuleView.Builder(), module).build();
    }

    @Override
    @NotNull
    public ModuleView editModule(@NotNull RulesProject project,
                                 @NotBlank String moduleName,
                                 @Valid EditModuleRequest request) {
        validateProjectWritePermission(project);

        var resolvedDescriptor = workspaceProjectService.getProjectDescriptor(project);
        var originalDescriptor = getOriginalProjectDescriptor(resolvedDescriptor);

        lockProject(project);

        validateDescriptorPermissions(project, true);
        validateModuleNameForEdit(request.name(), moduleName, request.path(), resolvedDescriptor);
        validateModulePathForEdit(request.path(), resolvedDescriptor, moduleName, false);

        ProjectDescriptor newDescriptor = Cloner.clone(originalDescriptor);
        Module module = CollectionUtils.findFirst(newDescriptor.getModules(),
                m -> m.getName() != null && m.getName().equals(moduleName));
        if (module == null) {
            throw new NotFoundException("module.not.found.message", moduleName);
        }

        boolean moduleWasRenamed = !moduleName.equals(request.name());
        applyModuleProperties(module, request);

        if (moduleWasRenamed) {
            updateOpenApiReferencesOnRename(newDescriptor, moduleName, request.name());
        }

        cleanDescriptor(newDescriptor);
        saveDescriptor(project, newDescriptor);

        return mapModuleFields(new ModuleView.Builder(), module).build();
    }

    @Override
    public void removeModule(@NotNull RulesProject project,
                             @NotBlank String moduleName,
                             boolean keepFile) {
        validateProjectWritePermission(project);

        var resolvedDescriptor = workspaceProjectService.getProjectDescriptor(project);
        var originalDescriptor = getOriginalProjectDescriptor(resolvedDescriptor);

        lockProject(project);
        validateDescriptorPermissions(project, false);

        ProjectDescriptor newDescriptor = Cloner.clone(originalDescriptor);
        List<Module> modules = newDescriptor.getModules();

        Module removed = null;
        for (int i = 0; i < modules.size(); i++) {
            Module m = modules.get(i);
            if (m.getName() != null && m.getName().equals(moduleName)) {
                removed = modules.remove(i);
                break;
            }
        }
        if (removed == null) {
            throw new NotFoundException("module.not.found.message", moduleName);
        }

        if (!keepFile) {
            List<Module> modulesForRemoving = new ArrayList<>();
            if (projectDescriptorManager.isModuleWithWildcard(removed)) {
                for (Module m : resolvedDescriptor.getModules()) {
                    if (m.getWildcardRulesRootPath() == null) {
                        continue;
                    }
                    if (m.getWildcardRulesRootPath().equals(removed.getRulesRootPath().getPath())) {
                        validateDeletePermission(project, m);
                        modulesForRemoving.add(m);
                    }
                }
            } else {
                validateDeletePermission(project, removed);
                modulesForRemoving.add(removed);
            }
            modulesForRemoving.forEach(m -> deleteModuleFile(project, m));

            if (project.hasArtefact(ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME)) {
                clearOpenApiReferencesOnRemove(newDescriptor, removed.getName());
                cleanDescriptor(newDescriptor);
                saveDescriptor(project, newDescriptor);
            } else {
                project.setModified();
            }
        } else {
            cleanDescriptor(newDescriptor);
            saveDescriptor(project, newDescriptor);
        }
    }

    /**
     * Maps base module fields (name, path) from a domain {@link Module} to the given builder.
     *
     * @param builder the target builder
     * @param module  the source domain module
     * @param <T>     the builder type
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
     * @param module  the source domain module
     * @param <T>     the builder type
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

    private void validateProjectWritePermission(RulesProject project) {
        if (!aclProjectsHelper.hasPermission(project, BasePermission.WRITE)) {
            throw new ForbiddenException();
        }
    }

    private void lockProject(RulesProject project) {
        if (!project.tryLock()) {
            throw new ConflictException("project.locked.message");
        }
    }

    /**
     * Validates permissions for modifying the project descriptor file.
     *
     * <p>Replicates legacy {@code ProjectBean.validatePermissionsForDescriptorFile(project, append)}.</p>
     *
     * @param project the rules project
     * @param append  if true, also check CREATE permission when descriptor does not exist
     */
    private void validateDescriptorPermissions(RulesProject project, boolean append) {
        if (project.hasArtefact(ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME)) {
            try {
                AProjectArtefact artefact = project
                        .getArtefact(ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME);
                if (!aclProjectsHelper.hasPermission(artefact, BasePermission.WRITE)) {
                    throw new ForbiddenException();
                }
            } catch (ProjectException ignored) {
                // Artefact access failed — skip permission check
            }
        } else if (append) {
            validateCreatePermission(project, ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME);
        }
    }

    private void validateCreatePermission(RulesProject project, String path) {
        String p = designRepositoryAclService.getPath(project);
        if (!designRepositoryAclService
                .isGranted(project.getRepository().getId(), p + "/" + path, List.of(BasePermission.CREATE))) {
            throw new ForbiddenException();
        }
    }

    private static final Set<String> EXCEL_EXTENSIONS = Set.of("xlsx", "xls");

    /**
     * Creates an empty Excel (.xlsx) file at the specified path in the project.
     *
     * @param project the rules project
     * @param path    relative file path within the project
     */
    private void createEmptyExcelFile(RulesProject project, String path) {
        String ext = FileUtils.getExtension(path);
        if (ext == null || !EXCEL_EXTENSIONS.contains(ext.toLowerCase())) {
            throw new BadRequestException("module.path.not.excel.message");
        }
        validateCreatePermission(project, path);
        try (var workbook = new XSSFWorkbook()) {
            workbook.createSheet("Sheet1");
            var out = new ByteArrayOutputStream();
            workbook.write(out);
            project.addResource(path, new ByteArrayInputStream(out.toByteArray()));
        } catch (ProjectException e) {
            throw new ConflictException("module.create.file.failed.message");
        } catch (IOException e) {
            throw new ConflictException("module.create.file.failed.message");
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
            project.addResource(newPath, sourceResource.getContent());
        } catch (ProjectException e) {
            throw new ConflictException("module.copy.failed.message");
        }
    }

    /**
     * Validates the module name for add/edit operations.
     *
     * <p>Replicates legacy {@code ProjectBean.validateModuleName()} logic:</p>
     * <ul>
     *   <li>Non-wildcard paths require a non-blank name</li>
     *   <li>Unchanged names in edit mode skip validation</li>
     *   <li>Name must pass {@link NameChecker#checkName(String)} and be unique</li>
     * </ul>
     *
     * @param newName            the new module name
     * @param oldName            the current module name ({@code null} for add mode)
     * @param path               the module path (used to detect wildcard modules)
     * @param resolvedDescriptor the resolved project descriptor for uniqueness checking
     */
    private void validateModuleNameForEdit(String newName,
                                           String oldName,
                                           String path,
                                           ProjectDescriptor resolvedDescriptor) {
        Module toCheck = new Module();
        toCheck.setRulesRootPath(new PathEntry(path));
        boolean withWildcard = projectDescriptorManager.isModuleWithWildcard(toCheck);

        if (!withWildcard) {
            if (StringUtils.isBlank(newName)) {
                throw new BadRequestException("cannot.be.empty.message");
            }
        }

        // If name unchanged in edit mode, skip further validation
        if (oldName != null && oldName.equals(newName)) {
            return;
        }

        // Adding new module, or name was changed
        if (!withWildcard || StringUtils.isNotBlank(newName)) {
            if (!NameChecker.checkName(newName)) {
                throw new BadRequestException("invalid.name.message");
            }
            Module existing = CollectionUtils.findFirst(resolvedDescriptor.getModules(),
                    m -> m.getName() != null && m.getName().equals(newName));
            if (existing != null) {
                throw new ConflictException("module.name.exists.message");
            }
        }
    }

    /**
     * Validates the module path for add/edit operations.
     *
     * <p>Replicates legacy {@code ProjectBean.validateModulePath()} logic:</p>
     * <ul>
     *   <li>Path must not be blank</li>
     *   <li>For non-wildcard paths, the file must already exist (unless {@code createFile} is true)</li>
     *   <li>Path must not conflict with existing modules (exact match or wildcard overlap)</li>
     * </ul>
     *
     * @param path               the module path to validate
     * @param resolvedDescriptor the resolved project descriptor for conflict checking
     * @param selfModuleName     the module being edited ({@code null} for add mode), excluded from conflict checks
     * @param createFile         if true, allow non-existent files (the caller will create them)
     */
    private void validateModulePathForEdit(String path,
                                           ProjectDescriptor resolvedDescriptor,
                                           String selfModuleName,
                                           boolean createFile) {
        if (StringUtils.isBlank(path)) {
            throw new BadRequestException("cannot.be.empty.message");
        }

        // For non-wildcard paths, the file must exist (unless createFile is requested)
        if (!(path.contains("*") || path.contains("?"))) {
            Path moduleFile = resolvedDescriptor.getProjectFolder().resolve(path);
            if (createFile) {
                if (Files.exists(moduleFile)) {
                    throw new ConflictException("module.file.exists.message");
                }
            } else if (!Files.exists(moduleFile)) {
                throw new NotFoundException("module.file.not.found.message");
            }
        }

        String relativePath = path.replace("\\", "/");

        for (Module m : resolvedDescriptor.getModules()) {
            // Skip self in edit mode
            if (selfModuleName != null && Objects.equals(selfModuleName, m.getName())) {
                continue;
            }

            String existingPath = m.getRulesRootPath() != null ? m.getRulesRootPath().getPath() : null;
            if (existingPath == null) {
                continue;
            }

            // Exact path match
            if (Objects.equals(existingPath, relativePath)) {
                throw new ConflictException("module.path.conflict.message");
            }
            // Wildcard overlap: existing module is wildcard AND new path matches the pattern
            if (projectDescriptorManager.isModuleWithWildcard(m) && FileUtils.pathMatches(existingPath, relativePath)) {
                throw new ConflictException("module.path.conflict.message");
            }
        }
    }

    /**
     * Applies module properties from the request to the domain module.
     *
     * <p>Replicates the property-setting logic from legacy {@code ProjectBean.editModule()}.</p>
     *
     * @param module  the domain module to update
     * @param request the edit module request
     */
    private void applyModuleProperties(Module module, EditModuleRequest request) {
        module.setName(request.name());

        PathEntry pathEntry = module.getRulesRootPath();
        if (pathEntry == null) {
            pathEntry = new PathEntry();
            module.setRulesRootPath(pathEntry);
        }
        pathEntry.setPath(request.path());

        MethodFilter filter = module.getMethodFilter();
        if (filter == null) {
            filter = new MethodFilter();
            module.setMethodFilter(filter);
        }
        filter.setIncludes(null);
        filter.setExcludes(null);

        if (!CollectionUtils.isEmpty(request.includes())) {
            filter.addIncludePattern(request.includes().toArray(String[]::new));
        }
        if (!CollectionUtils.isEmpty(request.excludes())) {
            filter.addExcludePattern(request.excludes().toArray(String[]::new));
        }

        WebstudioConfiguration webstudioConfiguration = new WebstudioConfiguration();
        if (Boolean.TRUE.equals(request.compileThisModuleOnly())) {
            webstudioConfiguration.setCompileThisModuleOnly(true);
        }
        module.setWebstudioConfiguration(webstudioConfiguration);
    }

    /**
     * Updates OpenAPI module name references when a module is renamed.
     *
     * <p>Replicates the OpenAPI reference update logic from legacy {@code ProjectBean.editModule()}.</p>
     *
     * @param descriptor the project descriptor to update
     * @param oldName    the old module name
     * @param newName    the new module name
     */
    private void updateOpenApiReferencesOnRename(ProjectDescriptor descriptor, String oldName, String newName) {
        var openAPI = descriptor.getOpenapi();
        if (openAPI == null) {
            return;
        }
        String algoName = openAPI.getAlgorithmModuleName();
        String modelName = openAPI.getModelModuleName();
        if (StringUtils.isBlank(algoName) && StringUtils.isBlank(modelName)) {
            return;
        }
        if (oldName.equals(algoName)) {
            openAPI.setAlgorithmModuleName(newName);
        } else if (oldName.equals(modelName)) {
            openAPI.setModelModuleName(newName);
        }
    }

    /**
     * Clears OpenAPI module name references when a module is removed.
     *
     * <p>Replicates the OpenAPI cleanup logic from legacy {@code ProjectBean.removeModule()}.
     * Uses case-insensitive comparison, matching the legacy behavior.</p>
     *
     * @param descriptor  the project descriptor to update
     * @param removedName the name of the removed module
     */
    private void clearOpenApiReferencesOnRemove(ProjectDescriptor descriptor, String removedName) {
        var openAPI = descriptor.getOpenapi();
        if (openAPI == null) {
            return;
        }
        String algoName = openAPI.getAlgorithmModuleName();
        String modelName = openAPI.getModelModuleName();
        if (algoName != null && algoName.equalsIgnoreCase(removedName)) {
            openAPI.setAlgorithmModuleName(null);
        } else if (modelName != null && modelName.equalsIgnoreCase(removedName)) {
            openAPI.setModelModuleName(null);
        }
    }

    /**
     * Validates DELETE permission on the module file.
     *
     * <p>Replicates legacy {@code ProjectBean.checkPermissionsForDeletingModule()}.</p>
     *
     * @param project the rules project
     * @param module  the module whose file permission is being checked
     */
    private void validateDeletePermission(RulesProject project, Module module) {
        try {
            AProjectArtefact artefact = project.getArtefact(module.getRulesRootPath().getPath());
            if (!aclProjectsHelper.hasPermission(artefact, BasePermission.DELETE)) {
                throw new ForbiddenException("module.delete.permission.message",
                        new Object[]{ProjectArtifactUtils.extractResourceName(artefact)});
            }
        } catch (ProjectException ignored) {
            // Artefact not accessible — skip permission check
        }
    }

    /**
     * Deletes the module file from the project.
     *
     * <p>Replicates legacy {@code ProjectBean.deleteModule()}.</p>
     *
     * @param project the rules project
     * @param module  the module whose file is to be deleted
     */
    private void deleteModuleFile(RulesProject project, Module module) {
        try {
            AProjectArtefact artefact = project.getArtefact(module.getRulesRootPath().getPath());
            artefact.delete();
        } catch (ProjectException e) {
            throw new ConflictException("module.delete.failed.message", module.getName());
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
            }
        } catch (ValidationException e) {
            throw new BadRequestException("module.copy.failed.message");
        } catch (Exception e) {
            throw new ConflictException("module.copy.failed.message");
        }
    }
}
