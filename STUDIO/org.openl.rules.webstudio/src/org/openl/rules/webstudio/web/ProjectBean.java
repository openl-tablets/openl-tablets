package org.openl.rules.webstudio.web;

import static org.openl.rules.webstudio.util.NameChecker.BAD_NAME_MSG;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.xml.bind.JAXBException;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.PathMatcher;
import org.springframework.web.context.annotation.RequestScope;

import org.openl.CompiledOpenClass;
import org.openl.rules.cloner.Cloner;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.model.scaffolding.environment.EnvironmentModel;
import org.openl.rules.openapi.OpenAPIModelConverter;
import org.openl.rules.openapi.impl.GroovyScriptFile;
import org.openl.rules.openapi.impl.OpenAPIGeneratedClasses;
import org.openl.rules.openapi.impl.OpenAPIJavaClassGenerator;
import org.openl.rules.openapi.impl.OpenAPIScaffoldingConverter;
import org.openl.rules.project.ProjectDescriptorManager;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.instantiation.RulesInstantiationException;
import org.openl.rules.project.model.MethodFilter;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.OpenAPI;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.project.model.WebstudioConfiguration;
import org.openl.rules.project.model.validation.ValidationException;
import org.openl.rules.project.openapi.OpenApiGenerationException;
import org.openl.rules.project.openapi.OpenApiGenerator;
import org.openl.rules.project.resolving.InvalidFileNamePatternException;
import org.openl.rules.project.resolving.InvalidFileNameProcessorException;
import org.openl.rules.project.resolving.NoMatchFileNameException;
import org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy;
import org.openl.rules.project.resolving.PropertiesFileNameProcessorBuilder;
import org.openl.rules.project.xml.XmlRulesDeploySerializer;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.table.formatters.Formats;
import org.openl.rules.ui.Message;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.ui.util.ListItem;
import org.openl.rules.webstudio.WebStudioFormats;
import org.openl.rules.webstudio.service.OpenAPIHelper;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.rules.webstudio.web.repository.RepositoryTreeState;
import org.openl.rules.webstudio.web.repository.tree.TreeProject;
import org.openl.rules.webstudio.web.util.ProjectArtifactUtils;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.security.acl.permission.AclPermission;
import org.openl.security.acl.permission.AclRole;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.util.CollectionUtils;
import org.openl.util.FileTypeHelper;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.openl.util.StringTool;
import org.openl.util.StringUtils;
import org.openl.util.formatters.FileNameFormatter;
import org.openl.validation.ValidatedCompiledOpenClass;

@Service
@RequestScope
public class ProjectBean {
    private static final String RULES_DEPLOY_XML = "rules-deploy.xml";
    public static final String CANNOT_BE_EMPTY_MESSAGE = "Cannot be empty";
    public static final String RECONCILIATION = "reconciliation";
    private static final XmlRulesDeploySerializer RULES_DEPLOY_XML_SERIALIZER = new XmlRulesDeploySerializer();

    private final ProjectDescriptorManager projectDescriptorManager = new ProjectDescriptorManager();

    private final RepositoryTreeState repositoryTreeState;

    private final WebStudio studio = WebStudioUtils.getWebStudio();

    private final Logger log = LoggerFactory.getLogger(ProjectBean.class);

    private List<ListItem<ProjectDependencyDescriptor>> dependencies;
    private String sources;
    private String[] propertiesFileNamePatterns;

    private final Formats formats = WebStudioFormats.getInstance();
    private List<ModuleInfoDTO> modulesInfoList = new ArrayList<>();

    private UIInput propertiesFileNameProcessorInput;

    private String currentModuleName;

    private String newFileName;
    private String currentPathPattern;
    private Integer currentModuleIndex;

    private final RepositoryAclService designRepositoryAclService;

    private final OpenAPIHelper openAPIHelper = new OpenAPIHelper();

    public ProjectBean(RepositoryTreeState repositoryTreeState,
                       @Qualifier("designRepositoryAclService") RepositoryAclService designRepositoryAclService) {
        this.repositoryTreeState = repositoryTreeState;
        this.designRepositoryAclService = designRepositoryAclService;
    }

    public String getModulePath(Module module) {
        PathEntry modulePath = module == null ? null : module.getRulesRootPath();
        if (modulePath == null) {
            return null;
        }
        return modulePath.getPath();
    }

    public List<ListItem<ProjectDependencyDescriptor>> getDependencies() {
        dependencies = new ArrayList<>();

        ProjectDescriptor currentProject = studio.getCurrentProjectDescriptor();

        List<ProjectDescriptor> projects = studio.getAllProjects();
        for (ProjectDescriptor project : projects) {
            String name = project.getName();
            if (!name.equals(currentProject.getName())) {
                ProjectDependencyDescriptor dependency = new ProjectDependencyDescriptor();
                ProjectDependencyDescriptor projectDependency = studio.getProjectDependency(name);
                dependency.setName(name);
                dependency.setAutoIncluded(projectDependency != null && projectDependency.isAutoIncluded());
                dependencies.add(new ListItem<>(projectDependency != null, dependency));
            }
        }
        return dependencies;
    }

    public String getSources() {
        ProjectDescriptor currentProject = studio.getCurrentProjectDescriptor();
        List<PathEntry> sourceList = currentProject.getClasspath();
        if (sourceList != null) {
            StringBuilder sb = new StringBuilder();
            for (PathEntry source : sourceList) {
                sb.append(source.getPath()).append(StringTool.NEW_LINE);
            }
            sources = sb.toString();
        }

        return sources;
    }

    public void setSources(String sources) {
        this.sources = sources;
    }

    public String getOpenAPIPath() {
        ProjectDescriptor currentProject = studio.getCurrentProjectDescriptor();
        final OpenAPI openapi = currentProject.getOpenapi();
        if (openapi == null || openapi.getPath() == null) {
            return "";
        }
        return openapi.getPath();
    }

    public String getLastOpenAPIFileUploadDate() {
        ProjectDescriptor projectDescriptor = studio.getCurrentProjectDescriptor();
        final OpenAPI openapi = projectDescriptor.getOpenapi();
        if (openapi == null) {
            return null;
        }
        String path = openapi.getPath();
        if (path == null) {
            return null;
        }
        AProjectArtefact openAPIFile;

        try {
            openAPIFile = studio.getCurrentProject().getArtefactByPath(new ArtefactPathImpl(path));
        } catch (ProjectException e) {
            log.debug("Error on reading OpenAPI file.", e);
            return null;
        }
        FileData fileData = openAPIFile.getFileData();
        if (fileData != null && fileData.getModifiedAt() != null) {
            return formats.formatDateOrDateTime(fileData.getModifiedAt());
        }
        return null;
    }

    public String getModelModuleName() {
        ProjectDescriptor currentProject = studio.getCurrentProjectDescriptor();
        final OpenAPI openapi = currentProject.getOpenapi();
        if (openapi == null || openapi.getModelModuleName() == null) {
            return null;
        }
        return openapi.getModelModuleName();
    }

    public String getAlgorithmModuleName() {
        ProjectDescriptor currentProject = studio.getCurrentProjectDescriptor();
        final OpenAPI openapi = currentProject.getOpenapi();
        if (openapi == null || openapi.getAlgorithmModuleName() == null) {
            return null;
        }
        return openapi.getAlgorithmModuleName();
    }

    public String getPropertiesFileNamePatterns() {
        propertiesFileNamePatterns = studio.getCurrentProjectDescriptor().getPropertiesFileNamePatterns();
        return StringUtils.join(propertiesFileNamePatterns, "\n");
    }

    public void setPropertiesFileNamePatterns(String propertiesFileNamePatterns) {
        this.propertiesFileNamePatterns = StringUtils.toLines(propertiesFileNamePatterns);
    }

    // TODO Move messages to ValidationMessages.properties
    public void validateProjectName(FacesContext context, UIComponent toValidate, Object value) {
        String name = (String) value;

        WebStudioUtils.validate(StringUtils.isNotBlank(name), "Cannot be empty");

        if (!studio.getCurrentProjectDescriptor().getName().equals(name)) {
            WebStudioUtils.validate(NameChecker.checkName(name), NameChecker.BAD_PROJECT_NAME_MSG);
            WebStudioUtils.validate(!NameChecker.isReservedName(name), String.format("'%s' is a reserved word.", name));
            WebStudioUtils.validate(!studio.isProjectExists(name), "Project with such name already exists");
        }
    }

    public void validatePropertiesFileNameProcessor(FacesContext context, UIComponent toValidate, Object value) {
        String className = (String) value;

        if (!StringUtils.isBlank(className)) {
            WebStudioUtils.validate(className.matches("([\\w$]+\\.)*[\\w$]+"), "Invalid class name");

            ProjectDescriptor projectDescriptor = cloneProjectDescriptor(studio.getCurrentProjectDescriptor());
            projectDescriptor.setPropertiesFileNameProcessor(className);
            PropertiesFileNameProcessorBuilder propertiesFileNameProcessorBuilder = new PropertiesFileNameProcessorBuilder();
            try {
                propertiesFileNameProcessorBuilder.build(projectDescriptor);
            } catch (InvalidFileNameProcessorException e) {
                WebStudioUtils.throwValidationError(e.getMessage());
            } catch (InvalidFileNamePatternException ignore) {
                // Ignore
            } finally {
                propertiesFileNameProcessorBuilder.destroy();
            }
        }
    }

    // TODO Move messages to ValidationMessages.properties
    public void validatePropertiesFileNamePattern(FacesContext context, UIComponent toValidate, Object value) {
        String[] patterns = StringUtils.toLines((String) value);

        if (patterns != null) {
            PropertiesFileNameProcessorBuilder propertiesFileNameProcessorBuilder = new PropertiesFileNameProcessorBuilder();
            try {
                ProjectDescriptor projectDescriptor = cloneProjectDescriptor(studio.getCurrentProjectDescriptor());
                projectDescriptor.setPropertiesFileNameProcessor((String) propertiesFileNameProcessorInput.getValue());
                projectDescriptor.setPropertiesFileNamePatterns(patterns);
                propertiesFileNameProcessorBuilder.build(projectDescriptor);
            } catch (InvalidFileNamePatternException e) {
                WebStudioUtils.throwValidationError(e.getMessage());
            } catch (InvalidFileNameProcessorException ignored) {
                // Processed in other validator
            } finally {
                propertiesFileNameProcessorBuilder.destroy();
            }
        }
    }

    // TODO Move messages to ValidationMessages.properties
    public void validateModuleName(FacesContext context, UIComponent toValidate, Object value) {
        String newName = (String) value;
        String oldName = WebStudioUtils.getRequestParameter("moduleNameOld");
        String modulePath = WebStudioUtils.getRequestParameter("modulePath");

        Module toCheck = new Module();
        toCheck.setRulesRootPath(new PathEntry(modulePath));
        boolean withWildcard = isModuleWithWildcard(toCheck);
        if (!withWildcard) {
            WebStudioUtils.validate(StringUtils.isNotBlank(newName), CANNOT_BE_EMPTY_MESSAGE);
        }

        if (StringUtils.isBlank(oldName) // Add new Module
                || !oldName.equals(newName)) { // Edit current Module
            if (!withWildcard || StringUtils.isNotBlank(newName)) {
                WebStudioUtils.validate(NameChecker.checkName(newName), BAD_NAME_MSG);

                Module module = studio.getModule(studio.getCurrentProjectDescriptor(), newName);
                WebStudioUtils.validate(module == null, "Module with such name already exists");
            }
        }
    }

    public void validateModuleNameForCopy(FacesContext context, UIComponent toValidate, Object value) {
        String newName = (String) value;
        String oldName = WebStudioUtils.getRequestParameter("copyModuleForm:moduleNameOld");
        String modulePath = WebStudioUtils.getRequestParameter("copyModuleForm:modulePath");

        Module toCheck = new Module();
        toCheck.setRulesRootPath(new PathEntry(modulePath));
        boolean withWildcard = isModuleWithWildcard(toCheck);
        if (!withWildcard) {
            WebStudioUtils.validate(StringUtils.isNotBlank(newName), CANNOT_BE_EMPTY_MESSAGE);
        }

        if (StringUtils.isBlank(oldName) // Add new Module
                || !oldName.equals(newName)) { // Edit current Module
            if (!withWildcard || StringUtils.isNotBlank(newName)) {
                WebStudioUtils.validate(NameChecker.checkName(newName), BAD_NAME_MSG);

                Module module = studio.getModule(studio.getCurrentProjectDescriptor(), newName);
                WebStudioUtils.validate(module == null, "Module with such name already exists");
            }
        }
    }

    // TODO Move messages to ValidationMessages.properties
    public void validateModulePath(FacesContext context, UIComponent toValidate, Object value) {
        final String path = (String) value;
        WebStudioUtils.validate(StringUtils.isNotBlank(path), CANNOT_BE_EMPTY_MESSAGE);
        ProjectDescriptor projectDescriptor = studio.getCurrentProjectDescriptor();

        if (!(path.contains("*") || path.contains("?"))) {
            Path moduleFile = projectDescriptor.getProjectFolder().resolve(path);
            WebStudioUtils.validate(Files.exists(moduleFile), "File with the specified path is not found");
        }

        final String oldName = Optional.ofNullable(WebStudioUtils.getRequestParameter("moduleNameOld"))
                .filter(StringUtils::isNotBlank)
                .orElseGet(() -> WebStudioUtils.getRequestParameter("copyModuleForm:moduleNameOld"));
        final String index = WebStudioUtils.getRequestParameter("moduleIndex");

        final String relativePath = path.replace("\\", "/");

        final boolean isNewModule = StringUtils.isBlank(oldName) && StringUtils.isBlank(index);
        final Predicate<Module> isEditedModule = m -> !isNewModule && Objects.equals(oldName, m.getName());

        final PathMatcher pathMatcher = projectDescriptorManager.getPathMatcher();
        final Predicate<Module> wildcardPathMatch = m -> pathMatcher.match(m.getRulesRootPath().getPath(),
                relativePath);

        final Predicate<Module> strictPathMatch = m -> Objects.equals(m.getRulesRootPath().getPath(), relativePath);
        final Predicate<Module> checkDuplicatePath = strictPathMatch
                .or(wildcardPathMatch.and(this::isModuleWithWildcard));
        if (projectDescriptor.getModules().stream().filter(isEditedModule.negate()).anyMatch(checkDuplicatePath)) {
            WebStudioUtils.throwValidationError("Path is already covered with existing module.");
        }
    }

    public void validateModulePathForCopy(FacesContext context, UIComponent toValidate, Object value) {
        String path = WebStudioUtils.getRequestParameter("copyModuleForm:modulePath");
        WebStudioUtils.validate(StringUtils.isNotBlank(path), CANNOT_BE_EMPTY_MESSAGE);

        WebStudioUtils.validate(!(path.contains("*") || path.contains("?")), "Path cannot contain wildcard symbols");
        try {
            NameChecker.validatePath(path);
        } catch (IOException e) {
            WebStudioUtils.throwValidationError(String.format("Invalid path \"%s\". %s", path, e.getMessage()));
        }
        Path moduleFile = studio.getCurrentProjectDescriptor().getProjectFolder().resolve(path);
        WebStudioUtils.validate(!Files.exists(moduleFile), "File with such name already exists");
    }

    public void validateOpenAPIPath(FacesContext context, UIComponent uiComponent, Object value) {
        String pathToOpenAPIFile = (String) value;
        WebStudioUtils.validate(StringUtils.isNotBlank(pathToOpenAPIFile), "Path to openAPI file cannot be empty.");
        WebStudioUtils.validate(FileTypeHelper.isPossibleOpenAPIFile(pathToOpenAPIFile),
                "The OpenAPI file must have JSON, YAML(YML) extension.");

        boolean fileExists;
        try {
            studio.getCurrentProject().getArtefactByPath(new ArtefactPathImpl(pathToOpenAPIFile));
            fileExists = true;
        } catch (ProjectException e) {
            fileExists = false;
        }
        WebStudioUtils.validate(fileExists, "OpenAPI file with path: " + pathToOpenAPIFile + " was not found.");
    }

    public void validateAlgorithmPath(FacesContext context, UIComponent component, Object value) {
        String algoPath = WebStudioUtils.getRequestParameter("generateOpenAPIForm:algorithmModulePath");
        String modelPath = WebStudioUtils.getRequestParameter("generateOpenAPIForm:modelModulePath");
        String algorithmModuleNameParam = WebStudioUtils.getRequestParameter("generateOpenAPIForm:algorithmModuleName");
        WebStudioUtils.validate(StringUtils.isNotBlank(algoPath), "Algorithm Module Path cannot be empty.");
        WebStudioUtils.validate(!algoPath.equalsIgnoreCase(modelPath), "Module paths cannot be the same");
        validatePath(algoPath, algorithmModuleNameParam);
    }

    private void validatePath(String path, String moduleName) {
        try {
            NameChecker.validatePath(path);
        } catch (IOException e) {
            WebStudioUtils.throwValidationError(String.format("Invalid path \"%s\". %s", path, e.getMessage()));
        }
        String fileName = FileUtils.getBaseName(path);
        WebStudioUtils.validate(NameChecker.checkName(fileName),
                "File name contains illegal characters " + BAD_NAME_MSG);
        WebStudioUtils.validate(StringUtils.isNotBlank(fileName), "File name is not defined.");
        String fileNameWithExtension = FileUtils.getName(path);
        WebStudioUtils.validate(FileTypeHelper.isExcelFile(fileNameWithExtension),
                "The generated file must have an Excel format.");

        ProjectDescriptor currentProjectDescriptor = studio.getCurrentProjectDescriptor();
        Module module = studio.getModule(currentProjectDescriptor, moduleName);
        String folderPath = studio.getCurrentProject().getFolderPath();
        if (module == null || (module
                .getRulesRootPath() != null && !getArtefactPath(module.getRulesRootPath().getPath(), folderPath)
                .equals(path))) {
            Path moduleFile = currentProjectDescriptor.getProjectFolder().resolve(path);
            WebStudioUtils.validate(!Files.exists(moduleFile), "File with such name already exists.");
        }

    }

    public void validateDataPath(FacesContext context, UIComponent component, Object value) {
        String path = WebStudioUtils.getRequestParameter("generateOpenAPIForm:modelModulePath");
        String algoPath = WebStudioUtils.getRequestParameter("generateOpenAPIForm:algorithmModulePath");
        String modelModuleNameParam = WebStudioUtils.getRequestParameter("generateOpenAPIForm:modelModuleName");
        WebStudioUtils.validate(StringUtils.isNotBlank(path), "Data Module Path cannot be empty.");
        WebStudioUtils.validate(!algoPath.equalsIgnoreCase(path), "Module paths cannot be the same");
        validatePath(path, modelModuleNameParam);
    }

    public void validateAlgorithmModuleName(FacesContext context, UIComponent component, Object value) {
        if (isReconciliationMode())
            return;
        final String possibleAlgorithmModuleName = (String) value;
        final String currentModelModuleName = WebStudioUtils.getRequestParameter("importOpenAPIForm:modelModuleName");
        if (currentModelModuleName != null) {
            WebStudioUtils.validate(!currentModelModuleName.equalsIgnoreCase(possibleAlgorithmModuleName),
                    "Module names cannot be the same.");
        }
        WebStudioUtils.validate(StringUtils.isNotBlank(possibleAlgorithmModuleName),
                "Module Name for Rules must not be empty.");
        WebStudioUtils.validate(NameChecker.checkName(possibleAlgorithmModuleName),
                "Rules Module name contains illegal characters.");
    }

    public void validateModelModuleName(FacesContext context, UIComponent component, Object value) {
        if (isReconciliationMode())
            return;
        final String possibleModelModuleName = (String) value;
        final String currentAlgorithmModuleName = WebStudioUtils
                .getRequestParameter("importOpenAPIForm:algorithmModuleName");
        if (currentAlgorithmModuleName != null) {
            WebStudioUtils.validate(!currentAlgorithmModuleName.equalsIgnoreCase(possibleModelModuleName),
                    "Module names cannot be the same.");
        }
        WebStudioUtils.validate(StringUtils.isNotBlank(possibleModelModuleName),
                "Module Name for Data Types must not be empty.");
        WebStudioUtils.validate(NameChecker.checkName(possibleModelModuleName),
                "Data Module name contains illegal characters.");
    }

    private boolean isReconciliationMode() {
        String mode = WebStudioUtils.getRequestParameter("mode");
        return mode.equals(RECONCILIATION);
    }

    public void editName() {
        tryLockProject();
        RulesProject currentProject = studio.getCurrentProject();
        validatePermissionsForDescriptorFile(currentProject, true);

        ProjectDescriptor projectDescriptor = studio.getCurrentProjectDescriptor();
        projectDescriptor.setPropertiesFileNamePatterns(propertiesFileNamePatterns);
        ProjectDescriptor newProjectDescriptor = cloneProjectDescriptor(projectDescriptor);

        Repository designRepository = currentProject.getDesignRepository();
        boolean supportsMappedFolders = designRepository != null && designRepository.supports().mappedFolders();
        if (!supportsMappedFolders && studio.isRenamed(currentProject)) {
            // Restore physical project name
            newProjectDescriptor.setName(currentProject.getName());
        }

        clean(newProjectDescriptor);
        save(newProjectDescriptor);

        try {
            String repositoryId = currentProject.getRepository().getId();
            String branch = currentProject.getBranch();
            studio.init(repositoryId, branch, newProjectDescriptor.getName(), null);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new Message("Error while project renaming");
        }
    }

    private void validatePermissionsForDescriptorFile(RulesProject currentProject, boolean append) {
        if (currentProject.hasArtefact(ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME)) {
            try {
                AProjectArtefact projectArtefact = currentProject
                        .getArtefact(ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME);
                validatePermissionForEditing(projectArtefact);
            } catch (ProjectException ignored) {
            }
        } else {
            if (append) {
                validatePermissionForCreating(currentProject,
                        ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME);
            }
        }
    }

    public void editModule() {
        tryLockProject();
        RulesProject currentProject = studio.getCurrentProject();
        validatePermissionsForDescriptorFile(currentProject, true);
        ProjectDescriptor projectDescriptor = getOriginalProjectDescriptor();
        ProjectDescriptor newProjectDescriptor = cloneProjectDescriptor(projectDescriptor);

        String index = WebStudioUtils.getRequestParameter("moduleIndex");
        String oldName = WebStudioUtils.getRequestParameter("moduleNameOld");
        String name = WebStudioUtils.getRequestParameter("moduleName");
        String path = WebStudioUtils.getRequestParameter("modulePath");
        String includes = WebStudioUtils.getRequestParameter("moduleIncludes");
        String excludes = WebStudioUtils.getRequestParameter("moduleExcludes");
        String compileThisModuleOnly = WebStudioUtils.getRequestParameter("compileThisModuleOnly");

        Module module;

        boolean moduleWasRenamed = false;

        if (StringUtils.isBlank(oldName) && StringUtils.isBlank(index)) {
            // Add new Module
            module = new Module();
            module.setProject(newProjectDescriptor);
            newProjectDescriptor.getModules().add(module);
        } else {
            // Edit current Module
            if (!StringUtils.isBlank(oldName)) {
                module = studio.getModule(newProjectDescriptor, oldName);
                if (!oldName.equals(name)) {
                    moduleWasRenamed = true;
                }
            } else {
                module = newProjectDescriptor.getModules().get(Integer.parseInt(index));
            }
        }

        if (module != null) {
            module.setName(name);

            PathEntry pathEntry = module.getRulesRootPath();
            if (pathEntry == null) {
                pathEntry = new PathEntry();
                module.setRulesRootPath(pathEntry);
            }
            pathEntry.setPath(path);

            MethodFilter filter = module.getMethodFilter();
            if (filter == null) {
                filter = new MethodFilter();
                module.setMethodFilter(filter);
            }
            filter.setIncludes(null);
            filter.setExcludes(null);

            if (StringUtils.isNotBlank(includes)) {
                filter.addIncludePattern(StringUtils.toLines(includes));
            }
            if (StringUtils.isNotBlank(excludes)) {
                filter.addExcludePattern(StringUtils.toLines(excludes));
            }

            if (moduleWasRenamed) {
                OpenAPI descriptorOpenAPI = newProjectDescriptor.getOpenapi();
                if (descriptorOpenAPI != null) {
                    String algorithmsModuleName = descriptorOpenAPI.getAlgorithmModuleName();
                    String modelsModuleName = descriptorOpenAPI.getModelModuleName();
                    boolean moduleNamesExists = StringUtils.isNotBlank(algorithmsModuleName) || StringUtils
                            .isNotBlank(modelsModuleName);
                    if (moduleNamesExists) {
                        if (oldName.equals(algorithmsModuleName)) {
                            descriptorOpenAPI.setAlgorithmModuleName(name);
                        } else if (oldName.equals(modelsModuleName)) {
                            descriptorOpenAPI.setModelModuleName(name);
                        }
                    }
                }
            }
            WebstudioConfiguration webstudioConfiguration = new WebstudioConfiguration();
            if ("on".equals(compileThisModuleOnly)) {
                webstudioConfiguration.setCompileThisModuleOnly(true);
            }
            module.setWebstudioConfiguration(webstudioConfiguration);

            clean(newProjectDescriptor);
            save(newProjectDescriptor);
        }
    }

    public void copyModule() {
        tryLockProject();
        RulesProject currentProject = studio.getCurrentProject();
        ProjectDescriptor projectDescriptor = getOriginalProjectDescriptor();
        ProjectDescriptor newProjectDescriptor = cloneProjectDescriptor(projectDescriptor);

        String name = WebStudioUtils.getRequestParameter("copyModuleForm:moduleName");
        String oldPath = WebStudioUtils.getRequestParameter("copyModuleForm:modulePathOld");
        String path = WebStudioUtils.getRequestParameter("copyModuleForm:modulePath");

        PathEntry pathEntry = new PathEntry();
        pathEntry.setPath(path);

        Module newModule = new Module();
        newModule.setName(name);
        newModule.setRulesRootPath(pathEntry);
        boolean moduleMatchesSomePathPattern = isModuleMatchesSomePathPattern(newModule);
        if (!moduleMatchesSomePathPattern) {
            validatePermissionsForDescriptorFile(currentProject, false);
        }

        AProjectResource oldProjectResource;
        try {
            oldProjectResource = (AProjectResource) currentProject.getArtefact(oldPath);
        } catch (ProjectException e) {
            throw new Message("Error while module copying.");
        }
        validatePermissionForCreating(currentProject, path);
        try {
            AProjectResource newProjectResource = currentProject.addResource(path, oldProjectResource.getContent());
            if (!designRepositoryAclService.hasAcl(newProjectResource) && !designRepositoryAclService
                    .createAcl(newProjectResource, List.of(AclRole.CONTRIBUTOR.getCumulativePermission()), true)) {
                String message = String.format("Granting permissions to a new file '%s' is failed.",
                        ProjectArtifactUtils.extractResourceName(newProjectResource));
                WebStudioUtils.addErrorMessage(message);
            }
        } catch (ProjectException e) {
            throw new Message("Error while module copying.");
        }
        currentProject.setModified();

        if (!moduleMatchesSomePathPattern) {
            // Add new Module
            newModule.setProject(newProjectDescriptor);
            newProjectDescriptor.getModules().add(newModule);

            clean(newProjectDescriptor);
            save(newProjectDescriptor);
        } else {
            refreshProject(currentProject.getRepository().getId(), currentProject.getName());
        }
    }

    private void refreshProject(String repoId, String name) {
        studio.getModel().clearModuleInfo();
        ProjectDescriptor oldProjectDescriptor = studio.getCurrentProjectDescriptor();
        ProjectDescriptor newProjectDescriptor = studio.resolveProject(oldProjectDescriptor);
        studio.forceUpdateProjectDescriptor(repoId, newProjectDescriptor, oldProjectDescriptor);
        TreeProject projectNode = repositoryTreeState.getProjectNodeByPhysicalName(repoId, name);
        if (projectNode != null) {
            // For example, repository wasn't refreshed yet
            projectNode.refresh();
        }
    }

    public void removeModule() {
        tryLockProject();
        RulesProject currentProject = studio.getCurrentProject();
        validatePermissionsForDescriptorFile(currentProject, false);

        ProjectDescriptor projectDescriptor = getOriginalProjectDescriptor();
        ProjectDescriptor newProjectDescriptor = cloneProjectDescriptor(projectDescriptor);

        String toRemove = WebStudioUtils.getRequestParameter("moduleToRemove");
        String leaveExcelFile = WebStudioUtils.getRequestParameter("leaveExcelFile");

        List<Module> modules = newProjectDescriptor.getModules();
        Module removed = modules.remove(Integer.parseInt(toRemove));

        if (StringUtils.isEmpty(leaveExcelFile)) {
            ProjectDescriptor currentProjectDescriptor = studio.getCurrentProjectDescriptor();
            List<Module> modulesForRemoving = new ArrayList<>();
            if (projectDescriptorManager.isModuleWithWildcard(removed)) {
                for (Module module : currentProjectDescriptor.getModules()) {
                    if (module.getWildcardRulesRootPath() == null) {
                        // Module not included in wildcard
                        continue;
                    }
                    if (module.getWildcardRulesRootPath().equals(removed.getRulesRootPath().getPath())) {
                        checkPermissionsForDeletingModule(currentProject, module);
                        modulesForRemoving.add(module);
                    }
                }
            } else {
                checkPermissionsForDeletingModule(currentProject, removed);
                modulesForRemoving.add(removed);
            }
            modulesForRemoving.forEach(e -> deleteModule(currentProject, e));
            File projectFolder = currentProjectDescriptor.getProjectFolder().toFile();
            File rulesXmlFile = new File(projectFolder,
                    ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME);
            if (rulesXmlFile.exists()) {
                String removedModuleName = removed.getName();
                clean(newProjectDescriptor);
                OpenAPI openAPI = newProjectDescriptor.getOpenapi();
                if (openAPI != null) {
                    String definedAlgoModuleName = openAPI.getAlgorithmModuleName();
                    String definedModelsName = openAPI.getModelModuleName();
                    if (definedAlgoModuleName != null && definedAlgoModuleName.equalsIgnoreCase(removedModuleName)) {
                        openAPI.setAlgorithmModuleName(null);
                    } else if (definedModelsName != null && definedModelsName.equalsIgnoreCase(removedModuleName)) {
                        openAPI.setModelModuleName(null);
                    }
                }
                save(newProjectDescriptor);
            } else {
                currentProject.setModified();
                refreshProject(currentProject.getRepository().getId(), currentProject.getName());
            }
        } else {
            clean(newProjectDescriptor);
            save(newProjectDescriptor);
        }
        studio.resetProjects();
    }

    private void checkPermissionsForDeletingModule(RulesProject currentProject, Module module) {
        try {
            AProjectArtefact projectArtefact = currentProject.getArtefact(module.getRulesRootPath().getPath());
            if (!designRepositoryAclService.isGranted(projectArtefact, true, AclPermission.DELETE)) {
                throw new Message(String.format("There is no permission for deleting '%s' file.",
                        ProjectArtifactUtils.extractResourceName(projectArtefact)));
            }
        } catch (ProjectException ignored) {
        }
    }

    private void deleteModule(RulesProject currentProject, Module module) {
        try {
            AProjectArtefact projectArtefact = currentProject.getArtefact(module.getRulesRootPath().getPath());
            projectArtefact.delete();
        } catch (ProjectException e) {
            throw new Message(String.format("Cannot delete '%s' module.", module.getName()), e);
        }
    }

    public boolean canCopyModule(Module module) {
        if (studio.getModel().isEditableProject()) {
            RulesProject currentProject = studio.getCurrentProject();
            String path = module.getRulesRootPath().getPath();
            boolean originalPathWithWildcards = path.contains("?") || path.contains("*");
            path = path.replace("\\", "/");
            int d = path.lastIndexOf("/");
            path = d >= 0 ? path.substring(0, d) : StringUtils.EMPTY;
            if (path.contains("?") || path.contains("*")) {
                return true;
            } else {
                if (!originalPathWithWildcards && !studio.getModel().isEditableProjectDescriptor()) {
                    return false;
                }
                if (path.startsWith("./")) {
                    path = path.substring(2);
                }
                path = designRepositoryAclService
                        .getPath(currentProject) + (StringUtils.isNotBlank(path) ? "/" + path : StringUtils.EMPTY);
                return designRepositoryAclService
                        .isGranted(currentProject.getRepository().getId(), path, List.of(AclPermission.CREATE));
            }
        }
        return false;
    }

    public boolean canDeleteModule(Module module) {
        if (studio.getModel().isEditableProject()) {
            RulesProject currentProject = studio.getCurrentProject();
            if (currentProject.hasArtefact(ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME)) {
                try {
                    return designRepositoryAclService.isGranted(
                            currentProject
                                    .getArtefact(ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME),
                            List.of(AclPermission.WRITE));
                } catch (ProjectException ignored) {
                    return false;
                }
            } else {
                return designRepositoryAclService.isGranted(currentProject, List.of(AclPermission.CREATE));
            }
        }
        return false;
    }

    public boolean isOnlySafeModuleRemove(Module module) {
        if (module == null) {
            return true;
        }
        RulesProject currentProject = studio.getCurrentProject();
        if (currentProject != null) {
            try {
                if (isModuleWithWildcard(module)) {
                    for (Module m : getModulesMatchingPathPattern(module)) {
                        AProjectArtefact projectArtefact = currentProject.getArtefact(m.getRulesRootPath().getPath());
                        if (!designRepositoryAclService.isGranted(projectArtefact, true, AclPermission.DELETE)) {
                            return true;
                        }
                    }
                    return false;
                } else {
                    AProjectArtefact projectArtefact = currentProject.getArtefact(module.getRulesRootPath().getPath());
                    return !designRepositoryAclService.isGranted(projectArtefact, true, AclPermission.DELETE);
                }
            } catch (ProjectException e) {
                return true;
            }
        }
        return true;
    }

    public void removeDependency(String name) {
        tryLockProject();

        RulesProject currentProject = studio.getCurrentProject();
        validatePermissionsForDescriptorFile(currentProject, false);

        ProjectDescriptor projectDescriptor = studio.getCurrentProjectDescriptor();
        ProjectDescriptor newProjectDescriptor = cloneProjectDescriptor(projectDescriptor);

        clean(newProjectDescriptor);

        List<ProjectDependencyDescriptor> resultDependencies = newProjectDescriptor.getDependencies();

        resultDependencies.removeIf(dependency -> dependency.getName().equals(name));

        newProjectDescriptor.setDependencies(!resultDependencies.isEmpty() ? resultDependencies : null);

        save(newProjectDescriptor);
    }

    public void editDependencies() {
        tryLockProject();

        RulesProject currentProject = studio.getCurrentProject();
        validatePermissionsForDescriptorFile(currentProject, true);

        ProjectDescriptor projectDescriptor = studio.getCurrentProjectDescriptor();
        ProjectDescriptor newProjectDescriptor = cloneProjectDescriptor(projectDescriptor);

        clean(newProjectDescriptor);

        List<ProjectDependencyDescriptor> resultDependencies = new ArrayList<>();

        for (ListItem<ProjectDependencyDescriptor> dependency : dependencies) {
            if (dependency.isSelected()) {
                resultDependencies.add(dependency.getItem());
            }
        }

        newProjectDescriptor.setDependencies(!resultDependencies.isEmpty() ? resultDependencies : null);

        save(newProjectDescriptor);
    }

    public void editSources() {
        tryLockProject();

        RulesProject currentProject = studio.getCurrentProject();
        validatePermissionsForDescriptorFile(currentProject, true);

        List<PathEntry> sourceList = new ArrayList<>();
        String[] sourceArray = StringUtils.toLines(sources);

        if (CollectionUtils.isNotEmpty(sourceArray)) {
            for (String source : sourceArray) {
                PathEntry sourcePath = new PathEntry(source);
                sourceList.add(sourcePath);
            }
        }

        ProjectDescriptor projectDescriptor = studio.getCurrentProjectDescriptor();
        ProjectDescriptor newProjectDescriptor = cloneProjectDescriptor(projectDescriptor);

        clean(newProjectDescriptor);

        newProjectDescriptor.setClasspath(!sourceList.isEmpty() ? sourceList : null);

        save(newProjectDescriptor);
    }

    public void reconcileOpenAPI() {
        tryLockProject();

        RulesProject currentProject = studio.getCurrentProject();
        validatePermissionsForDescriptorFile(currentProject, true);

        ProjectDescriptor projectDescriptor = studio.getCurrentProjectDescriptor();
        ProjectDescriptor newProjectDescriptor = cloneProjectDescriptor(projectDescriptor);
        clean(newProjectDescriptor);

        String openAPIPathParam = FileNameFormatter
                .normalizePath(WebStudioUtils.getRequestParameter("importOpenAPIForm:openAPIPath"));

        OpenAPI openAPI = projectDescriptor.getOpenapi();
        if (openAPI != null) {
            openAPI.setPath(openAPIPathParam);
            openAPI.setMode(OpenAPI.Mode.RECONCILIATION);
        } else {
            openAPI = new OpenAPI(openAPIPathParam, OpenAPI.Mode.RECONCILIATION, null, null);
        }
        newProjectDescriptor.setOpenapi(openAPI);
        save(newProjectDescriptor);
    }

    public void getModulesInfo() {
        ProjectDescriptor currentProjectDescriptor = studio.getCurrentProjectDescriptor();
        String algorithmModuleNameParam = WebStudioUtils.getRequestParameter("importOpenAPIForm:algorithmModuleName");
        String modelsModuleNameParam = WebStudioUtils.getRequestParameter("importOpenAPIForm:modelModuleName");
        Module algoModule = studio.getModule(currentProjectDescriptor, algorithmModuleNameParam);
        Module modelsModule = studio.getModule(currentProjectDescriptor, modelsModuleNameParam);
        ModuleInfoDTO rulesDTO = getModuleInfoDTO(algoModule, "Algorithms");
        ModuleInfoDTO modelsDTO = getModuleInfoDTO(modelsModule, "Models");
        modulesInfoList = Arrays.asList(rulesDTO, modelsDTO);
    }

    private ModuleInfoDTO getModuleInfoDTO(Module module, String type) {
        ModuleInfoDTO result = null;
        if (module != null) {
            PathEntry rulesRootPath = module.getRulesRootPath();
            result = new ModuleInfoDTO(module.getName(),
                    rulesRootPath != null ? getArtefactPath(rulesRootPath.getPath(),
                            studio.getCurrentProject().getFolderPath()) : null,
                    type);
        }
        return result;
    }

    public void createOrUpdateOpenAPISchema() {
        ProjectDescriptor currentDescriptor = studio.getCurrentProjectDescriptor();
        List<Module> modules = currentDescriptor.getModules();
        if (modules.isEmpty()) {
            throw new Message("Project has no modules.");
        }
        RulesProject currentProject = studio.getCurrentProject();
        validatePermissionsForDescriptorFile(currentProject, true);

        studio.init(currentProject.getDesignRepository().getId(),
                currentProject.getBranch(),
                currentProject.getName(),
                modules.iterator().next().getName());
        org.openl.rules.ui.ProjectModel projectModel = studio.getModel();
        while (!isCompilationCompleted(projectModel)) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new Message("Project compilation has been interrupted.", e);
            }
        }
        final String existedOpenAPIFilePath = getExistedOpenAPIFilePath();
        final boolean update = existedOpenAPIFilePath != null;
        CompiledOpenClass compiledOpenClass = projectModel.getCompiledOpenClass();
        final boolean hasCompilationErrors;
        if (compiledOpenClass instanceof ValidatedCompiledOpenClass) {
            ValidatedCompiledOpenClass validated = (ValidatedCompiledOpenClass) compiledOpenClass;
            hasCompilationErrors = validated.hasErrors() && !validated.hasOnlyValidationErrors();
        } else {
            hasCompilationErrors = compiledOpenClass.hasErrors();
        }
        if (hasCompilationErrors) {
            throw new Message(
                    String.format("Cannot %s OpenAPI file. Project has compilation error.", update ? "update" : "create"));
        }

        tryLockProject();
        try {
            OpenApiGenerator generator = OpenApiGenerator
                    .builder(projectModel.getModuleInfo().getProject(), projectModel.getRulesInstantiationStrategy(projectModel.getModuleInfo().getProject()))
                    .generator();
            final OpenAPI.Type openAPIType = Optional.of(OpenAPI.Type.JSON)
                    .map(t -> update ? OpenAPI.Type.chooseType(FileUtils.getExtension(existedOpenAPIFilePath)) : t)
                    .orElse(OpenAPI.Type.JSON);
            try (InputStream in = serializeOpenApi(generator, openAPIType)) {
                if (update) {
                    AProjectResource resource = ((AProjectResource) currentProject.getProject()
                            .getArtefact(existedOpenAPIFilePath));
                    validatePermissionForEditing(resource);
                    resource.setContent(in);
                } else {
                    validatePermissionForCreating(currentProject, openAPIType.getDefaultFileName());
                    currentProject.addResource(openAPIType.getDefaultFileName(), in);
                    AProjectArtefact projectArtefact = currentProject.getArtefact(openAPIType.getDefaultFileName());
                    if (!designRepositoryAclService.hasAcl(projectArtefact) && !designRepositoryAclService
                            .createAcl(projectArtefact, List.of(AclRole.CONTRIBUTOR.getCumulativePermission()), true)) {
                        String message = String.format("Granting permissions to a new file '%s' is failed.",
                                ProjectArtifactUtils.extractResourceName(projectArtefact));
                        WebStudioUtils.addErrorMessage(message);
                    }
                }
            }
            ProjectDescriptor newProjectDescriptor = cloneProjectDescriptor(currentDescriptor);
            clean(newProjectDescriptor);
            if (update && newProjectDescriptor.getOpenapi() != null) {
                OpenAPI openAPI = newProjectDescriptor.getOpenapi();
                openAPI.setPath(existedOpenAPIFilePath);
                openAPI.setMode(OpenAPI.Mode.RECONCILIATION);
            } else {
                OpenAPI openAPI = new OpenAPI();
                openAPI.setPath(openAPIType.getDefaultFileName());
                openAPI.setMode(OpenAPI.Mode.RECONCILIATION);
                newProjectDescriptor.setOpenapi(openAPI);
            }
            save(newProjectDescriptor);
        } catch (OpenApiGenerationException e) {
            throw new Message(e.getMessage(), e);
        } catch (Message e) {
            throw e;
        } catch (Exception e) {
            throw new Message(
                    String.format("Failed to %s OpenAPI file. Check compilation.", update ? "update" : "create"),
                    e);
        } finally {
            if (!currentProject.isModified()) {
                currentProject.unlock();
            }
        }
    }

    private static boolean isCompilationCompleted(org.openl.rules.ui.ProjectModel projectModel) {
        return projectModel.isProjectCompilationCompleted() || Optional.ofNullable(projectModel.getModuleInfo())
                .map(Module::getWebstudioConfiguration)
                .map(WebstudioConfiguration::isCompileThisModuleOnly)
                .orElse(Boolean.FALSE);
    }

    private static InputStream serializeOpenApi(OpenApiGenerator generator,
                                                OpenAPI.Type openAPIType) throws JsonProcessingException, RulesInstantiationException, OpenApiGenerationException {
        String generatedOpenAPISchema;
        switch (openAPIType) {
            case JSON:
                generatedOpenAPISchema = Json.pretty().writeValueAsString(generator.generate());
                break;
            case YAML:
            case YML:
                generatedOpenAPISchema = Yaml.pretty().writeValueAsString(generator.generate());
                break;
            default:
                throw new IllegalStateException(); // Must newer happened.
        }
        return IOUtils.toInputStream(generatedOpenAPISchema);
    }

    public String getExistedOpenAPIFilePath() {
        ProjectDescriptor currentDescriptor = studio.getCurrentProjectDescriptor();
        RulesProject currentProject = studio.getCurrentProject();
        Optional<String> openApiPath = Optional.ofNullable(currentDescriptor.getOpenapi())
                .map(OpenAPI::getPath)
                .filter(StringUtils::isNotBlank);
        if (openApiPath.isPresent() && currentProject.hasArtefact(openApiPath.get())) {
            return openApiPath.get();
        }
        return Stream.of(OpenAPI.Type.values())
                .map(OpenAPI.Type::getDefaultFileName)
                .filter(currentProject::hasArtefact)
                .findFirst()
                .orElse(null);
    }

    public void regenerateOpenAPI() {
        studio.initProjectHistory();
        tryLockProject();

        ProjectDescriptor currentProjectDescriptor = studio.getCurrentProjectDescriptor();
        List<Module> modules = currentProjectDescriptor.getModules();

        String openAPIPathParam = FileNameFormatter
                .normalizePath(WebStudioUtils.getRequestParameter("generateOpenAPIForm:openAPIPath"));
        String algorithmModuleNameParam = WebStudioUtils.getRequestParameter("generateOpenAPIForm:algorithmModuleName");
        String modelModuleNameParam = WebStudioUtils.getRequestParameter("generateOpenAPIForm:modelModuleName");
        String algorithmModulePathParam = WebStudioUtils.getRequestParameter("generateOpenAPIForm:algorithmModulePath");
        String modelModulePathParam = WebStudioUtils.getRequestParameter("generateOpenAPIForm:modelModulePath");
        if (!NameChecker.checkName(algorithmModuleNameParam) || !NameChecker.checkName(modelModuleNameParam)) {
            throw new Message("Module names are not correct.");
        }
        if (modelModulePathParam.equalsIgnoreCase(algorithmModulePathParam)) {
            throw new Message("Module paths cannot be the same.");
        }
        try {
            studio.freezeProject(currentProjectDescriptor.getName());
            Module existingAlgorithmModule = studio.getModule(currentProjectDescriptor, algorithmModuleNameParam);
            boolean isNewAlgorithmModule = existingAlgorithmModule == null;
            Module existingModelModule = studio.getModule(currentProjectDescriptor, modelModuleNameParam);
            boolean isNewDataModule = existingModelModule == null;

            validatePaths(algorithmModulePathParam, modelModulePathParam, isNewAlgorithmModule, isNewDataModule);

            boolean openAPIInfoChanged;
            boolean classPathChanged = false;

            final OpenAPI existingOpenAPI = currentProjectDescriptor.getOpenapi();
            OpenAPI openAPI = new OpenAPI();
            openAPI.setPath(openAPIPathParam);
            openAPI.setMode(OpenAPI.Mode.GENERATION);

            List<PathEntry> currentClassPath = currentProjectDescriptor.getClasspath();
            boolean openAPIClassesInClassPath = CollectionUtils.isNotEmpty(currentClassPath) && currentClassPath
                    .stream()
                    .anyMatch(pathEntry -> pathEntry.getPath().equals(OpenAPIHelper.DEF_JAVA_CLASS_PATH));

            if (existingOpenAPI == null || existingOpenAPI.getPath() == null || existingOpenAPI.getMode() == null) {
                openAPIInfoChanged = true;
            } else {
                boolean pathWasChanged = !existingOpenAPI.getPath().equals(openAPIPathParam);
                boolean modeWasChanged = !existingOpenAPI.getMode().equals(OpenAPI.Mode.GENERATION);
                openAPIInfoChanged = pathWasChanged || modeWasChanged;
            }

            RulesProject currentProject = studio.getCurrentProject();

            AProjectArtefact openAPIFile = getOpenAPIFile(openAPIPathParam, currentProject);

            if (!isNewAlgorithmModule) {
                deleteExistingExcelFile(existingAlgorithmModule,
                        currentProject,
                        "It's impossible to delete existing generated Rules file.");
                openAPI.setAlgorithmModuleName(algorithmModuleNameParam);
            } else {
                Module rulesModule = new Module();
                rulesModule.setRulesRootPath(new PathEntry(algorithmModulePathParam));
                rulesModule.setName(algorithmModuleNameParam);
                modules.add(rulesModule);
                openAPI.setAlgorithmModuleName(algorithmModuleNameParam);
                openAPIInfoChanged = true;
            }

            if (!isNewDataModule) {
                deleteExistingExcelFile(existingModelModule,
                        currentProject,
                        "It's impossible to delete existing generated Data Types file.");
                openAPI.setModelModuleName(modelModuleNameParam);
            } else {
                Module modelsModule = new Module();
                modelsModule.setRulesRootPath(new PathEntry(modelModulePathParam));
                modelsModule.setName(modelModuleNameParam);
                modules.add(modelsModule);
                openAPI.setModelModuleName(modelModuleNameParam);
                openAPIInfoChanged = true;
            }

            String workspacePath = studio.getWorkspacePath();
            String internalOpenAPIPath = openAPIFile.getArtefactPath().getStringValue();
            OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();

            ProjectModel projectModel = getProjectModel(FileNameFormatter.normalizePath(workspacePath),
                    internalOpenAPIPath,
                    converter);

            modules.stream().filter(m -> m.getName().equals(algorithmModuleNameParam)).findFirst().ifPresent(m -> {
                MethodFilter filter = new MethodFilter();
                filter.setIncludes(projectModel.getIncludeMethodFilter());
                m.setMethodFilter(filter);
            });

            addDataTypesFile(modelModulePathParam, currentProject, projectModel);

            addAlgorithmsFile(modelModuleNameParam, algorithmModulePathParam, currentProject, projectModel);

            OpenAPIGeneratedClasses generated = new OpenAPIJavaClassGenerator(projectModel).generate();
            boolean annotationTemplateClassesAreGenerated = generated.hasAnnotationTemplateClass();
            deletePreviouslyGeneratedOpenAPIClasses(currentProject);
            addGeneratedGroovyScripts(currentProject, generated, annotationTemplateClassesAreGenerated);

            if (annotationTemplateClassesAreGenerated) {
                if (!openAPIClassesInClassPath) {
                    classPathChanged = true;
                }
            } else {
                if (openAPIClassesInClassPath) {
                    classPathChanged = true;
                }
            }

            editOrCreateRulesDeploy(currentProject,
                    projectModel,
                    generated,
                    currentProject.hasArtefact(RULES_DEPLOY_XML));
            studio.storeProjectHistory();

            refreshProject(currentProject.getRepository().getId(), currentProject.getName());

            if (openAPIInfoChanged || classPathChanged) {
                editDescriptorIfNeeded(currentProjectDescriptor,
                        openAPIInfoChanged,
                        classPathChanged,
                        openAPI,
                        annotationTemplateClassesAreGenerated);
            }
        } finally {
            studio.releaseProject(currentProjectDescriptor.getName());
        }
    }

    private void validatePermissionForEditing(AProjectArtefact artefact) {
        if (!designRepositoryAclService.isGranted(artefact, List.of(AclPermission.WRITE))) {
            throw new Message(String.format("There is no permission for modifying '%s' file.",
                    ProjectArtifactUtils.extractResourceName(artefact)));
        }
    }

    private void validatePermissionForCreating(RulesProject currentProject, String path) {
        String p = designRepositoryAclService.getPath(currentProject);
        if (!designRepositoryAclService
                .isGranted(currentProject.getRepository().getId(), p + "/" + path, List.of(AclPermission.CREATE))) {
            throw new Message(String.format("There is no permission for creating '%s/%s' file.",
                    ProjectArtifactUtils.extractResourceName(currentProject),
                    path));
        }
    }

    private void addGeneratedGroovyScripts(RulesProject currentProject,
                                           OpenAPIGeneratedClasses generated,
                                           boolean annotationTemplateClassesAreGenerated) {
        if (annotationTemplateClassesAreGenerated) {
            try {
                GroovyScriptFile groovyFile = generated.getAnnotationTemplateGroovyFile();
                String groovyPath = openAPIHelper.makePathToTheGeneratedFile(groovyFile.getPath());
                validatePermissionForCreating(currentProject, groovyPath);
                currentProject.addResource(groovyPath, IOUtils.toInputStream(groovyFile.getScriptText()));
            } catch (ProjectException e) {
                throw new Message("Failed to add generated annotation template class.");
            }
        }
        try {
            for (GroovyScriptFile groovyCommonFile : generated.getGroovyCommonClasses()) {
                String javaInterfacePath = openAPIHelper.makePathToTheGeneratedFile(groovyCommonFile.getPath());
                validatePermissionForCreating(currentProject, javaInterfacePath);
                currentProject.addResource(javaInterfacePath, IOUtils.toInputStream(groovyCommonFile.getScriptText()));
            }
        } catch (ProjectException e) {
            throw new Message("Failed to add generated common classes.");
        }
    }

    private void deletePreviouslyGeneratedOpenAPIClasses(RulesProject currentProject) {
        try {
            currentProject.deleteArtefactsInFolder(
                    OpenAPIHelper.DEF_JAVA_CLASS_PATH + "/" + OpenAPIJavaClassGenerator.DEFAULT_OPEN_API_PATH.replace(".",
                            "/"));
        } catch (ProjectException e) {
            throw new Message(
                    String.format("Failed to remove previously generated file for project %s.", currentProject.getName()));
        }
    }

    private void editDescriptorIfNeeded(ProjectDescriptor currentProjectDescriptor,
                                        boolean openAPIInfoChanged,
                                        boolean classPathChanged,
                                        OpenAPI openAPI,
                                        boolean annotationTemplateClassesAreGenerated) {
        RulesProject currentProject = studio.getCurrentProject();
        validatePermissionsForDescriptorFile(currentProject, true);

        ProjectDescriptor newProjectDescriptor = cloneProjectDescriptor(currentProjectDescriptor);
        clean(newProjectDescriptor);
        if (openAPIInfoChanged) {
            newProjectDescriptor.setOpenapi(openAPI);
        }
        if (classPathChanged) {
            List<PathEntry> classpath = newProjectDescriptor.getClasspath();
            if (annotationTemplateClassesAreGenerated) {
                PathEntry openAPIClasses = new PathEntry(OpenAPIHelper.DEF_JAVA_CLASS_PATH);
                if (classpath == null) {
                    List<PathEntry> generatedClassPath = new ArrayList<>();
                    generatedClassPath.add(openAPIClasses);
                    newProjectDescriptor.setClasspath(generatedClassPath);
                } else {
                    classpath.add(openAPIClasses);
                }
            } else {
                if (classpath != null) {
                    classpath.removeIf(pathEntry -> pathEntry.getPath().equals(OpenAPIHelper.DEF_JAVA_CLASS_PATH));
                }
            }
        }
        save(newProjectDescriptor);
    }

    private void addAlgorithmsFile(String modelModuleNameParam,
                                   String algorithmModulePathParam,
                                   RulesProject currentProject,
                                   ProjectModel projectModel) {
        try (InputStream spreadsheets = openAPIHelper.generateAlgorithmsModule(projectModel
                .getSpreadsheetResultModels(), projectModel.getDataModels(), getEnvironmentModel(modelModuleNameParam))) {
            validatePermissionForCreating(currentProject, algorithmModulePathParam);
            currentProject.addResource(algorithmModulePathParam, spreadsheets);
        } catch (IOException | ProjectException e) {
            log.error(e.getMessage(), e);
            throw new Message("Failed to add rules file.");
        }
    }

    private void addDataTypesFile(String modelModulePathParam, RulesProject currentProject, ProjectModel projectModel) {
        try (InputStream dataTypes = openAPIHelper.generateDataTypesFile(projectModel.getDatatypeModels())) {
            validatePermissionForCreating(currentProject, modelModulePathParam);
            currentProject.addResource(modelModulePathParam, dataTypes);
        } catch (IOException | ProjectException e) {
            log.error(e.getMessage(), e);
            throw new Message("Failed to add data types file.");
        }
    }

    private void validatePaths(String algorithmModulePathParam,
                               String modelModulePathParam,
                               boolean isNewAlgorithmModule,
                               boolean isNewDataModule) {
        if (isNewAlgorithmModule) {
            checkPath(algorithmModulePathParam);
        }
        if (isNewDataModule) {
            checkPath(modelModulePathParam);
        }
    }

    private AProjectArtefact getOpenAPIFile(String openAPIPath, RulesProject currentProject) {
        AProjectArtefact openAPIFile;
        try {
            openAPIFile = currentProject.getArtefactByPath(new ArtefactPathImpl(openAPIPath));
        } catch (ProjectException e) {
            throw new Message("There is no openAPI file wasn't found with path " + openAPIPath + ".");
        }
        return openAPIFile;
    }

    private void deleteExistingExcelFile(Module existingModule, RulesProject currentProject, String errorMessage) {
        AProjectArtefact artefact = null;
        try {
            if (existingModule.getRulesRootPath() != null) {
                artefact = currentProject
                        .getArtefactByPath(new ArtefactPathImpl(getArtefactPath(existingModule.getRulesRootPath().getPath(),
                                studio.getCurrentProject().getFolderPath())));
            }
        } catch (ProjectException e) {
            log.warn("Existing file wasn't found in module {}", existingModule.getName(), e);
        }
        if (artefact != null) {
            if (!designRepositoryAclService.isGranted(artefact, true, AclPermission.DELETE)) {
                throw new Message(String.format("There is no permission for deleting '%s' file.",
                        ProjectArtifactUtils.extractResourceName(artefact)));
            }
            try {
                currentProject.deleteArtefact(artefact.getInternalPath());
            } catch (ProjectException e) {
                throw new Message(errorMessage);
            }
        }
    }

    private void checkPath(String path) {
        Path existingArtefact = studio.getCurrentProjectDescriptor().getProjectFolder().resolve(path);
        if (Files.exists(existingArtefact)) {
            throw new Message("Artefact with the path " + path + " already exists.");
        }
    }

    private void editOrCreateRulesDeploy(RulesProject currentProject,
                                         ProjectModel projectModel,
                                         OpenAPIGeneratedClasses generatedClasses,
                                         boolean rulesDeployExists) {
        try {
            if (rulesDeployExists) {
                AProjectResource artifact = (AProjectResource) currentProject.getArtefact(RULES_DEPLOY_XML);
                validatePermissionForEditing(artifact);
                try (InputStream rulesDeployContent = artifact.getContent()) {
                    RulesDeploy rulesDeploy = RULES_DEPLOY_XML_SERIALIZER.deserialize(rulesDeployContent);
                    artifact.setContent(openAPIHelper
                            .editOrCreateRulesDeploy(RULES_DEPLOY_XML_SERIALIZER, projectModel, generatedClasses, rulesDeploy));
                }
            } else {
                try (ByteArrayInputStream rulesDeployInputStream = openAPIHelper
                        .editOrCreateRulesDeploy(RULES_DEPLOY_XML_SERIALIZER, projectModel, generatedClasses, null)) {
                    validatePermissionForCreating(currentProject, RULES_DEPLOY_XML);
                    currentProject.addResource(RULES_DEPLOY_XML, rulesDeployInputStream);
                }
            }
        } catch (ProjectException | IOException | JAXBException e) {
            throw new Message("Failed to add 'rules-deploy.xml' file to the project.");
        }
    }

    private ProjectModel getProjectModel(String workspacePath,
                                         String internalOpenAPIPath,
                                         OpenAPIModelConverter converter) {
        ProjectModel projectModel;
        try {
            projectModel = converter.extractProjectModel(workspacePath + "/" + internalOpenAPIPath);
        } catch (Exception e) {
            throw new Message("OpenAPI file is corrupted.");
        }
        return projectModel;
    }

    private String getArtefactPath(String filePath, String basePath) {
        if (filePath.startsWith(basePath)) {
            filePath = filePath.substring(filePath.lastIndexOf(basePath) + basePath.length() + 1);
        }
        return FileNameFormatter.normalizePath(filePath);
    }

    private EnvironmentModel getEnvironmentModel(String moduleName) {
        EnvironmentModel environmentModel = new EnvironmentModel();
        environmentModel.setDependencies(Collections.singletonList(moduleName));
        return environmentModel;
    }

    private void tryLockProject() {
        RulesProject currentProject = studio.getCurrentProject();
        if (!currentProject.tryLock()) {
            throw new Message("Project is locked by the other user.");
        }
    }

    private void save(ProjectDescriptor projectDescriptor) {
        RulesProject project = studio.getCurrentProject();
        InputStream rulesDeployContent = null;
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            projectDescriptorManager.writeDescriptor(projectDescriptor, byteArrayOutputStream);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

            if (project.hasArtefact(ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME)) {
                AProjectResource artifact = (AProjectResource) project
                        .getArtefact(ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME);
                validatePermissionForEditing(artifact);
                artifact.setContent(inputStream);
            } else {
                validatePermissionForCreating(project,
                        ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME);
                project.addResource(ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME, inputStream);
                AProjectArtefact projectArtefact = project
                        .getArtefact(ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME);
                if (!designRepositoryAclService.hasAcl(projectArtefact) && !designRepositoryAclService
                        .createAcl(projectArtefact, List.of(AclRole.CONTRIBUTOR.getCumulativePermission()), true)) {
                    String message = String.format("Granting permissions to a new file '%s' is failed.",
                            ProjectArtifactUtils.extractResourceName(projectArtefact));
                    WebStudioUtils.addErrorMessage(message);
                }
            }
            if (project.hasArtefact(RULES_DEPLOY_XML)) {
                AProjectResource artifact = (AProjectResource) project.getArtefact(RULES_DEPLOY_XML);
                validatePermissionForEditing(artifact);
                rulesDeployContent = artifact.getContent();
                RulesDeploy rulesDeploy = RULES_DEPLOY_XML_SERIALIZER.deserialize(rulesDeployContent);
                artifact.setContent(new ByteArrayInputStream(
                        RULES_DEPLOY_XML_SERIALIZER.serialize(rulesDeploy).getBytes(StandardCharsets.UTF_8)));
            }

            refreshProject(project.getRepository().getId(), project.getName());
        } catch (ValidationException e) {
            throw new Message(e.getMessage());
        } catch (Message e) {
            throw e;
        } catch (Exception e) {
            throw new Message("Error while saving the project.");
        } finally {
            IOUtils.closeQuietly(rulesDeployContent);
        }
        WebStudioUtils.getWebStudio().resetProjects();
    }

    private void clean(ProjectDescriptor descriptor) {
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

        OpenAPI openapi = descriptor.getOpenapi();
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
                    rulesRootPath.setPath(getModulePath(module));
                } else {
                    module.setRulesRootPath(null);
                }
            }

            MethodFilter methodFilter = module.getMethodFilter();
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

    private ProjectDescriptor cloneProjectDescriptor(ProjectDescriptor projectDescriptor) {
        return Cloner.clone(projectDescriptor);
    }

    public UIInput getPropertiesFileNameProcessorInput() {
        return propertiesFileNameProcessorInput;
    }

    public void setPropertiesFileNameProcessorInput(UIInput propertiesFileNameProcessorInput) {
        this.propertiesFileNameProcessorInput = propertiesFileNameProcessorInput;
    }

    public String getCurrentModuleName() {
        return currentModuleName;
    }

    public void setCurrentModuleName(String currentModuleName) {
        this.currentModuleName = currentModuleName;
    }

    public List<Module> getModulesWithWildcard() {
        return getOriginalProjectDescriptor().getModules();
    }

    public boolean isModuleWithWildcard(Module module) {
        return projectDescriptorManager.isModuleWithWildcard(module);
    }

    public boolean isModuleMatchesSomePathPattern(Module module) {
        return projectDescriptorManager.isCoveredByWildcardModule(getOriginalProjectDescriptor(), module);
    }

    private Module getModuleWithWildcard(Module module) {
        List<Module> modules = getOriginalProjectDescriptor().getModules();

        for (Module originalModule : modules) {
            if (isModuleWithWildcard(originalModule)) {
                List<Module> modulesMatchingPathPattern = getModulesMatchingPathPattern(originalModule);
                for (Module m : modulesMatchingPathPattern) {
                    if (module.getName().equals(m.getName())) {
                        return originalModule;
                    }
                }
            }
        }

        return null;
    }

    public String getModulePathPattern() {
        if (currentModuleName == null) {
            return null;
        }

        ProjectDescriptor newProjectDescriptor = cloneProjectDescriptor(getOriginalProjectDescriptor());
        Module module;
        if (currentModuleIndex != null) {
            module = newProjectDescriptor.getModules().get(currentModuleIndex);
        } else {
            module = studio.getModule(newProjectDescriptor, currentModuleName);
        }
        if (module == null) {
            module = getModuleWithWildcard(studio.getModule(studio.getCurrentProjectDescriptor(), currentModuleName));
        }
        return getModulePath(module);
    }

    public boolean isCurrentModuleMatchesSomePathPattern() {
        if (currentModuleName == null) {
            return false;
        }
        ProjectDescriptor projectDescriptor = studio.getCurrentProjectDescriptor();
        Module module = studio.getModule(projectDescriptor, currentModuleName);
        return module != null && isModuleMatchesSomePathPattern(module);
    }

    public boolean isCurrentModuleHasPath(String path) {
        if (currentModuleName == null || path == null) {
            return false;
        }
        ProjectDescriptor projectDescriptor = studio.getCurrentProjectDescriptor();
        Module module = studio.getModule(projectDescriptor, currentModuleName);
        return module != null && path.equals(getModulePath(module));
    }

    public List<String> getModulePathsForPathPattern() {
        if (currentModuleName == null) {
            return Collections.emptyList();
        }

        ProjectDescriptor projectDescriptor = studio.getCurrentProjectDescriptor();
        ProjectDescriptor newProjectDescriptor = cloneProjectDescriptor(getOriginalProjectDescriptor());
        Module module = studio.getModule(projectDescriptor, currentModuleName);
        Module otherModule = null;
        if (module != null) {
            otherModule = getModuleWithWildcard(module);
        }
        if (otherModule != null) {
            module = otherModule;
        } else {
            if (currentModuleIndex != null) {
                module = newProjectDescriptor.getModules().get(currentModuleIndex);
            } else {
                module = studio.getModule(newProjectDescriptor, currentModuleName);
            }

            if (!projectDescriptorManager.isModuleWithWildcard(module)) {
                // Single module
                return Collections.singletonList(getModulePath(module));
            }
        }

        // Multiple modules
        List<Module> modules = getModulesMatchingPathPattern(module);

        return CollectionUtils.map(modules, this::getModulePath);
    }

    public void setNewFileName(String newFileName) {
        this.newFileName = newFileName;
    }

    public void setCurrentPathPattern(String currentPathPattern) {
        this.currentPathPattern = currentPathPattern;
    }

    public Boolean getFileNameMatched() {
        if (newFileName == null) {
            return null;
        }
        ProjectDescriptor projectDescriptor = getOriginalProjectDescriptor();
        PropertiesFileNameProcessorBuilder builder = new PropertiesFileNameProcessorBuilder();

        Boolean fileNameMatched = null;
        try {
            String[] patterns = projectDescriptor.getPropertiesFileNamePatterns();
            if (patterns != null) {
                builder.build(projectDescriptor).process(newFileName);
                fileNameMatched = true;
            }
        } catch (InvalidFileNameProcessorException | InvalidFileNamePatternException ignored) {
            // Cannot check for name correctness
        } catch (NoMatchFileNameException e) {
            fileNameMatched = false;
        }
        return fileNameMatched;
    }

    public String getChangedWildcardMatch() {
        if (currentPathPattern == null) {
            return null;
        }

        ProjectDescriptor projectDescriptor = getOriginalProjectDescriptor();

        PathMatcher pathMatcher = projectDescriptorManager.getPathMatcher();
        for (Module m : projectDescriptor.getModules()) {
            if (projectDescriptorManager.isModuleWithWildcard(m)) {
                String path = m.getRulesRootPath().getPath();
                if (pathMatcher.match(path, newFileName)) {
                    if (!currentPathPattern.equals(path)) {
                        // Module file name captured by another path pattern (not current one).
                        // User should ensure that he didn't do that unintentionally.
                        return path;
                    }
                    break;
                }
            }
        }

        return null;
    }

    public List<Module> getModulesMatchingPathPattern(Module module) {
        if (module == null || !projectDescriptorManager.isModuleWithWildcard(module)) {
            return Collections.emptyList();
        }
        try {
            return projectDescriptorManager.getAllModulesMatchingPathPattern(studio.getCurrentProjectDescriptor(),
                    module,
                    module.getRulesRootPath().getPath());
        } catch (IOException e) {
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
            }
            return Collections.emptyList();
        }
    }

    public boolean isEmptyMethodFilter(Module module) {
        MethodFilter methodFilter = module.getMethodFilter();
        if (methodFilter == null) {
            return true;
        }

        if (methodFilter.getIncludes() != null) {
            ArrayList<String> includes = new ArrayList<>(methodFilter.getIncludes());
            includes.removeAll(Arrays.asList("", null));
            if (!includes.isEmpty()) {
                return false;
            }
        }
        if (methodFilter.getExcludes() != null) {
            ArrayList<String> excludes = new ArrayList<>(methodFilter.getExcludes());
            excludes.removeAll(Arrays.asList("", null));
            return excludes.isEmpty();
        }

        return true;
    }

    public String getCurrentPropertiesFileNameProcessor() {
        return studio.getCurrentProjectDescriptor().getPropertiesFileNameProcessor();
    }

    public boolean isPropertiesFileNamePatternSupported() {
        return true;
    }

    public boolean isProjectDependenciesSupported() {
        return true;
    }

    private ProjectDescriptor getOriginalProjectDescriptor() {
        ProjectDescriptor descriptor = studio.getCurrentProjectDescriptor();
        try {
            File file = descriptor.getProjectFolder()
                    .resolve(ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME)
                    .toFile();
            return projectDescriptorManager.readOriginalDescriptor(file);
        } catch (FileNotFoundException ignored) {
            return descriptor;
        } catch (IOException | ValidationException | JAXBException e) {
            log.error(e.getMessage(), e);
            return descriptor;
        }
    }

    public void setCurrentModuleIndex(Integer currentModuleIndex) {
        this.currentModuleIndex = currentModuleIndex;
    }

    public Integer getCurrentModuleIndex() {
        return currentModuleIndex;
    }

    public List<ModuleInfoDTO> getModulesInfoList() {
        return modulesInfoList;
    }

    public void setModulesInfoList(List<ModuleInfoDTO> modulesInfoList) {
        this.modulesInfoList = modulesInfoList;
    }

}
