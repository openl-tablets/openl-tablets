package org.openl.rules.webstudio.web;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;

import org.openl.rules.common.ProjectException;
import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.excel.builder.ExcelFileBuilder;
import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.model.scaffolding.SpreadsheetModel;
import org.openl.rules.model.scaffolding.data.DataModel;
import org.openl.rules.model.scaffolding.environment.EnvironmentModel;
import org.openl.rules.openapi.OpenAPIModelConverter;
import org.openl.rules.openapi.impl.OpenAPIScaffoldingConverter;
import org.openl.rules.project.IProjectDescriptorSerializer;
import org.openl.rules.project.IRulesDeploySerializer;
import org.openl.rules.project.ProjectDescriptorManager;
import org.openl.rules.project.SafeCloner;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.project.model.MethodFilter;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.OpenAPI;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.project.model.validation.ValidationException;
import org.openl.rules.project.resolving.InvalidFileNamePatternException;
import org.openl.rules.project.resolving.InvalidFileNameProcessorException;
import org.openl.rules.project.resolving.NoMatchFileNameException;
import org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy;
import org.openl.rules.project.resolving.PropertiesFileNameProcessorBuilder;
import org.openl.rules.project.xml.ProjectDescriptorSerializerFactory;
import org.openl.rules.project.xml.RulesDeploySerializerFactory;
import org.openl.rules.project.xml.SupportedVersion;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.table.formatters.Formats;
import org.openl.rules.ui.Message;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.ui.util.ListItem;
import org.openl.rules.webstudio.WebStudioFormats;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.rules.webstudio.web.repository.RepositoryTreeState;
import org.openl.rules.webstudio.web.repository.tree.TreeProject;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.CollectionUtils;
import org.openl.util.FileTypeHelper;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.openl.util.StringTool;
import org.openl.util.StringUtils;
import org.openl.util.formatters.FileNameFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.PathMatcher;
import org.springframework.web.context.annotation.RequestScope;

@Service
@RequestScope
public class ProjectBean {
    private static final String RULES_DEPLOY_XML = "rules-deploy.xml";
    public static final String CANNOT_BE_EMPTY_MESSAGE = "Cannot be empty";
    public static final String RECONCILIATION = "reconciliation";

    private final ProjectDescriptorManager projectDescriptorManager = new ProjectDescriptorManager();

    private final RepositoryTreeState repositoryTreeState;

    private final ProjectDescriptorSerializerFactory projectDescriptorSerializerFactory;
    private final RulesDeploySerializerFactory rulesDeploySerializerFactory;

    private final WebStudio studio = WebStudioUtils.getWebStudio();

    private final Logger log = LoggerFactory.getLogger(ProjectBean.class);

    private List<ListItem<ProjectDependencyDescriptor>> dependencies;
    private String sources;
    private String[] propertiesFileNamePatterns;

    private final Formats formats = WebStudioFormats.getInstance();
    private List<ModuleInfoDTO> modulesInfoList = new ArrayList<>();

    private UIInput propertiesFileNameProcessorInput;

    private String currentModuleName;

    private SupportedVersion supportedVersion;
    private String newFileName;
    private String currentPathPattern;
    private Integer currentModuleIndex;
    private IRulesDeploySerializer rulesDeploySerializer;

    public ProjectBean(RepositoryTreeState repositoryTreeState,
            ProjectDescriptorSerializerFactory projectDescriptorSerializerFactory,
            RulesDeploySerializerFactory rulesDeploySerializerFactory) {
        this.repositoryTreeState = repositoryTreeState;
        this.projectDescriptorSerializerFactory = projectDescriptorSerializerFactory;
        this.rulesDeploySerializerFactory = rulesDeploySerializerFactory;
    }

    public String getModulePath(Module module) {
        PathEntry modulePath = module == null ? null : module.getRulesRootPath();

        if (modulePath == null) {
            return null;
        }

        String moduleFullPath = modulePath.getPath();
        ProjectDescriptor project = module.getProject();
        if (project == null || project.getProjectFolder() == null) {
            return moduleFullPath;
        }
        String projectFullPath = project.getProjectFolder().getAbsolutePath();

        if (moduleFullPath.contains(projectFullPath)) {
            return moduleFullPath.replace(projectFullPath, "").substring(1);
        }
        return moduleFullPath;
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
                dependency.setAutoIncluded(projectDependency == null || projectDependency.isAutoIncluded());
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

        WebStudioUtils.validate(StringUtils.isNotBlank(name), "Can not be empty");

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
                WebStudioUtils.validate(NameChecker.checkName(newName), NameChecker.BAD_NAME_MSG);

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
                WebStudioUtils.validate(NameChecker.checkName(newName), NameChecker.BAD_NAME_MSG);

                Module module = studio.getModule(studio.getCurrentProjectDescriptor(), newName);
                WebStudioUtils.validate(module == null, "Module with such name already exists");
            }
        }
    }

    // TODO Move messages to ValidationMessages.properties
    public void validateModulePath(FacesContext context, UIComponent toValidate, Object value) {
        String path = (String) value;
        WebStudioUtils.validate(StringUtils.isNotBlank(path), CANNOT_BE_EMPTY_MESSAGE);

        if (!(path.contains("*") || path.contains("?"))) {
            File moduleFile = new File(studio.getCurrentProjectDescriptor().getProjectFolder(), path);
            WebStudioUtils.validate(moduleFile.exists(), "File with such path does not exist");
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
        File moduleFile = new File(studio.getCurrentProjectDescriptor().getProjectFolder(), path);
        WebStudioUtils.validate(!moduleFile.exists(), "File with such name already exists");
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
        String path = WebStudioUtils.getRequestParameter("generateOpenAPIForm:algorithmModulePath");
        String algorithmModuleNameParam = WebStudioUtils.getRequestParameter("generateOpenAPIForm:algorithmModuleName");
        WebStudioUtils.validate(StringUtils.isNotBlank(path), "Algorithm Module Path cannot be empty.");
        validatePath(path, algorithmModuleNameParam);
    }

    private void validatePath(String path, String moduleName) {
        try {
            NameChecker.validatePath(path);
        } catch (IOException e) {
            WebStudioUtils.throwValidationError(String.format("Invalid path \"%s\". %s", path, e.getMessage()));
        }
        String fileName = FileUtils.getBaseName(path);
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
            File moduleFile = new File(currentProjectDescriptor.getProjectFolder(), path);
            WebStudioUtils.validate(!moduleFile.exists(), "File with such name already exists.");
        }

    }

    public void validateDataPath(FacesContext context, UIComponent component, Object value) {
        String path = WebStudioUtils.getRequestParameter("generateOpenAPIForm:modelModulePath");
        String modelModuleNameParam = WebStudioUtils.getRequestParameter("generateOpenAPIForm:modelModuleName");
        WebStudioUtils.validate(StringUtils.isNotBlank(path), "Data Module Path cannot be empty.");
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

        ProjectDescriptor projectDescriptor = studio.getCurrentProjectDescriptor();
        projectDescriptor.setPropertiesFileNamePatterns(propertiesFileNamePatterns);

        ProjectDescriptor newProjectDescriptor = cloneProjectDescriptor(projectDescriptor);

        RulesProject currentProject = studio.getCurrentProject();
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

    public void editModule() {
        tryLockProject();

        ProjectDescriptor projectDescriptor = getOriginalProjectDescriptor();
        ProjectDescriptor newProjectDescriptor = cloneProjectDescriptor(projectDescriptor);

        String index = WebStudioUtils.getRequestParameter("moduleIndex");
        String oldName = WebStudioUtils.getRequestParameter("moduleNameOld");
        String name = WebStudioUtils.getRequestParameter("moduleName");
        String path = WebStudioUtils.getRequestParameter("modulePath");
        String includes = WebStudioUtils.getRequestParameter("moduleIncludes");
        String excludes = WebStudioUtils.getRequestParameter("moduleExcludes");

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
                filter.addIncludePattern(includes.split(StringTool.NEW_LINE));
            }
            if (StringUtils.isNotBlank(excludes)) {
                filter.addExcludePattern(excludes.split(StringTool.NEW_LINE));
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

            clean(newProjectDescriptor);
            save(newProjectDescriptor);
        }
    }

    public void copyModule() {
        tryLockProject();

        ProjectDescriptor projectDescriptor = getOriginalProjectDescriptor();
        ProjectDescriptor newProjectDescriptor = cloneProjectDescriptor(projectDescriptor);

        String name = WebStudioUtils.getRequestParameter("copyModuleForm:moduleName");
        String oldPath = WebStudioUtils.getRequestParameter("copyModuleForm:modulePathOld");
        String path = WebStudioUtils.getRequestParameter("copyModuleForm:modulePath");

        File projectFolder = studio.getCurrentProjectDescriptor().getProjectFolder();
        File inputFile = new File(projectFolder, oldPath);
        File outputFile = new File(projectFolder, path);
        try {
            FileUtils.copy(inputFile, outputFile);
        } catch (IOException e) {
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
            }
            throw new Message("Error while project copying");
        }

        RulesProject currentProject = studio.getCurrentProject();
        currentProject.setModified();

        PathEntry pathEntry = new PathEntry();
        pathEntry.setPath(path);

        Module module = new Module();
        module.setName(name);
        module.setRulesRootPath(pathEntry);

        if (!isModuleMatchesSomePathPattern(module)) {
            // Add new Module
            module.setProject(newProjectDescriptor);
            newProjectDescriptor.getModules().add(module);

            clean(newProjectDescriptor);
            save(newProjectDescriptor);
        } else {
            refreshProject(currentProject.getRepository().getId(), currentProject.getName());
        }
    }

    private void refreshProject(String repoId, String name) {
        studio.getModel().clearModuleInfo();
        studio.resolveProject(studio.getCurrentProjectDescriptor());
        TreeProject projectNode = repositoryTreeState.getProjectNodeByPhysicalName(repoId, name);
        if (projectNode != null) {
            // For example, repository wasn't refreshed yet
            projectNode.refresh();
        }
    }

    public void removeModule() {
        tryLockProject();

        ProjectDescriptor projectDescriptor = getOriginalProjectDescriptor();
        ProjectDescriptor newProjectDescriptor = cloneProjectDescriptor(projectDescriptor);

        String toRemove = WebStudioUtils.getRequestParameter("moduleToRemove");
        String leaveExcelFile = WebStudioUtils.getRequestParameter("leaveExcelFile");

        List<Module> modules = newProjectDescriptor.getModules();
        Module removed = modules.remove(Integer.parseInt(toRemove));
        String removedModuleName = removed.getName();

        if (StringUtils.isEmpty(leaveExcelFile)) {
            ProjectDescriptor currentProjectDescriptor = studio.getCurrentProjectDescriptor();
            File projectFolder = currentProjectDescriptor.getProjectFolder();

            if (projectDescriptorManager.isModuleWithWildcard(removed)) {
                for (Module module : currentProjectDescriptor.getModules()) {
                    if (module.getWildcardRulesRootPath() == null) {
                        // Module not included in wildcard
                        continue;
                    }
                    if (module.getWildcardRulesRootPath().equals(removed.getRulesRootPath().getPath())) {
                        File file = new File(module.getRulesRootPath().getPath());
                        if (!file.delete() && file.exists()) {
                            throw new Message("Cannot delete the file " + file.getName());
                        }
                    }
                }
            } else {
                File file = new File(removed.getRulesRootPath().getPath());
                if (!file.isAbsolute()) {
                    file = new File(projectFolder, removed.getRulesRootPath().getPath());
                }
                if (!file.delete() && file.exists()) {
                    throw new Message("Cannot delete the file " + file.getName());
                }
            }
            File rulesXmlFile = new File(projectFolder,
                ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME);
            if (rulesXmlFile.exists()) {
                clean(newProjectDescriptor);
                OpenAPI openAPI = newProjectDescriptor.getOpenapi();
                if (openAPI != null) {
                    String definedAlgoModuleName = openAPI.getAlgorithmModuleName();
                    String definedModelsName = openAPI.getModelModuleName();
                    if (definedAlgoModuleName.equalsIgnoreCase(removedModuleName)) {
                        openAPI.setAlgorithmModuleName(null);
                    } else if (definedModelsName.equalsIgnoreCase(removedModuleName)) {
                        openAPI.setModelModuleName(null);
                    }
                }
                save(newProjectDescriptor);
            } else {
                RulesProject currentProject = studio.getCurrentProject();
                currentProject.setModified();
                refreshProject(currentProject.getRepository().getId(), currentProject.getName());
            }
        } else {
            clean(newProjectDescriptor);
            save(newProjectDescriptor);
        }
        studio.resetProjects();
    }

    public void removeDependency(String name) {
        tryLockProject();

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

        List<PathEntry> sourceList = new ArrayList<>();
        String[] sourceArray = sources.split(StringTool.NEW_LINE);

        if (CollectionUtils.isNotEmpty(sourceArray)) {
            for (String source : sourceArray) {
                if (StringUtils.isNotBlank(source)) {
                    PathEntry sourcePath = new PathEntry(source);
                    sourceList.add(sourcePath);
                }
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

    public void regenerateOpenAPI() {
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
        Module existingAlgorithmModule = studio.getModule(currentProjectDescriptor, algorithmModuleNameParam);
        boolean isNewAlgorithmModule = existingAlgorithmModule == null;
        Module existingModelModule = studio.getModule(currentProjectDescriptor, modelModuleNameParam);
        boolean isNewDataModule = existingModelModule == null;

        validatePaths(algorithmModulePathParam, modelModulePathParam, isNewAlgorithmModule, isNewDataModule);

        boolean infoChanged;

        final OpenAPI existingOpenAPI = currentProjectDescriptor.getOpenapi();
        OpenAPI openAPI = new OpenAPI();
        openAPI.setPath(openAPIPathParam);
        openAPI.setMode(OpenAPI.Mode.GENERATION);

        if (existingOpenAPI == null || existingOpenAPI.getPath() == null || existingOpenAPI.getMode() == null) {
            infoChanged = true;
        } else {
            boolean pathWasChanged = !existingOpenAPI.getPath().equals(openAPIPathParam);
            boolean modeWasChanged = !existingOpenAPI.getMode().equals(OpenAPI.Mode.GENERATION);
            infoChanged = pathWasChanged || modeWasChanged;
        }

        RulesProject currentProject = studio.getCurrentProject();

        AProjectArtefact openAPIFile = getOpenAPIFile(openAPIPathParam, currentProject);

        if (!isNewAlgorithmModule) {
            removeExistingFile(existingAlgorithmModule,
                currentProject,
                "It's impossible to delete existing generated Rules file.");
            openAPI.setAlgorithmModuleName(algorithmModuleNameParam);
        } else {
            Module rulesModule = new Module();
            rulesModule.setRulesRootPath(new PathEntry(algorithmModulePathParam));
            rulesModule.setName(algorithmModuleNameParam);
            modules.add(rulesModule);
            openAPI.setAlgorithmModuleName(algorithmModuleNameParam);
            infoChanged = true;
        }

        if (!isNewDataModule) {
            removeExistingFile(existingModelModule,
                currentProject,
                "It's impossible to delete existing generated Data Types file.");
            openAPI.setModelModuleName(modelModuleNameParam);
        } else {
            Module modelsModule = new Module();
            modelsModule.setRulesRootPath(new PathEntry(modelModulePathParam));
            modelsModule.setName(modelModuleNameParam);
            modules.add(modelsModule);
            openAPI.setModelModuleName(modelModuleNameParam);
            infoChanged = true;
        }

        String workspacePath = studio.getWorkspacePath();
        String internalOpenAPIPath = openAPIFile.getArtefactPath().getStringValue();
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();

        ProjectModel projectModel = getProjectModel(FileNameFormatter.normalizePath(workspacePath),
            internalOpenAPIPath,
            converter);

        if (currentProject.hasArtefact(RULES_DEPLOY_XML)) {
            editRulesDeploy(currentProject, projectModel);
        } else {
            configureRulesDeploy(currentProject, projectModel);
        }

        try (InputStream dataTypes = generateDataTypesFile(projectModel.getDatatypeModels())) {
            currentProject.addResource(modelModulePathParam, dataTypes);
        } catch (IOException | ProjectException e) {
            log.error(e.getMessage(), e);
            throw new Message("Failed to add data types file.");
        }

        try (InputStream spreadsheets = generateAlgorithmsFile(projectModel.getSpreadsheetResultModels(),
            projectModel.getDataModels(),
            getEnvironmentModel(modelModuleNameParam))) {
            currentProject.addResource(algorithmModulePathParam, spreadsheets);
        } catch (IOException | ProjectException e) {
            log.error(e.getMessage(), e);
            throw new Message("Failed to add rules file.");
        }

        refreshProject(currentProject.getRepository().getId(), currentProject.getName());

        if (infoChanged) {
            ProjectDescriptor newProjectDescriptor = cloneProjectDescriptor(currentProjectDescriptor);
            clean(newProjectDescriptor);
            newProjectDescriptor.setOpenapi(openAPI);
            save(newProjectDescriptor);
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
            log.error("Error on reading OpenAPI file.", e);
            throw new Message("There is no openAPI file wasn't found with path " + openAPIPath + ".");
        }
        return openAPIFile;
    }

    private void removeExistingFile(Module existingModule, RulesProject currentProject, String errorMessage) {
        AProjectArtefact file = null;
        try {
            if (existingModule.getRulesRootPath() != null) {
                file = currentProject
                    .getArtefactByPath(new ArtefactPathImpl(getArtefactPath(existingModule.getRulesRootPath().getPath(),
                        studio.getCurrentProject().getFolderPath())));
            }
        } catch (ProjectException e) {
            log.warn("Existing file wasn't found in module {}", existingModule.getName(), e);
        }
        removePreviouslyGeneratedFile(file, errorMessage);
    }

    private void checkPath(String path) {
        File existingArtefact = new File(studio.getCurrentProjectDescriptor().getProjectFolder(), path);
        if (existingArtefact.exists()) {
            throw new Message("Artefact with the path " + path + " already exists.");
        }
    }

    private void editRulesDeploy(RulesProject currentProject, ProjectModel projectModel) {
        try {
            configureSerializer();
            modifyRulesDeploy(currentProject, projectModel);
        } catch (ProjectException | IOException e) {
            log.error("Can't modify rules deploy file to the project.");
            throw new Message("Failed to modify rules deploy xml file.");
        }
    }

    private void configureRulesDeploy(RulesProject currentProject, ProjectModel projectModel) {
        try {
            configureSerializer();
        } catch (IOException e) {
            log.error("There was an error with configuring the serializer.");
            throw new Message("Failed to create rules deploy xml file.");
        }
        try (ByteArrayInputStream rulesDeployInputStream = generateRulesDeployFile(projectModel)) {
            currentProject.addResource(RULES_DEPLOY_XML, rulesDeployInputStream);
        } catch (ProjectException | IOException e) {
            log.error("Can't add rules deploy file to the project.");
            throw new Message("Failed to add rules deploy xml file.");
        }
    }

    private ProjectModel getProjectModel(String workspacePath,
            String internalOpenAPIPath,
            OpenAPIModelConverter converter) {
        ProjectModel projectModel;
        try {
            projectModel = converter.extractProjectModel(workspacePath + "/" + internalOpenAPIPath);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new Message("OpenAPI file is corrupted.");
        }
        return projectModel;
    }

    private ByteArrayInputStream generateRulesDeployFile(ProjectModel projectModel) {
        RulesDeploy rd = new RulesDeploy();
        rd.setProvideRuntimeContext(projectModel.isRuntimeContextProvided());
        rd.setPublishers(new RulesDeploy.PublisherType[] { RulesDeploy.PublisherType.RESTFUL });
        return new ByteArrayInputStream(rulesDeploySerializer.serialize(rd).getBytes(StandardCharsets.UTF_8));
    }

    private void modifyRulesDeploy(RulesProject currentProject, ProjectModel projectModel) throws ProjectException,
                                                                                           IOException {
        AProjectResource artifact;
        artifact = (AProjectResource) currentProject.getArtefact(RULES_DEPLOY_XML);
        try (InputStream rulesDeployContent = artifact.getContent()) {
            RulesDeploy rulesDeploy = rulesDeploySerializerFactory.getSerializer(SupportedVersion.getLastVersion())
                .deserialize(rulesDeployContent);
            rulesDeploy.setProvideRuntimeContext(projectModel.isRuntimeContextProvided());
            artifact.setContent(new ByteArrayInputStream(
                rulesDeploySerializer.serialize(rulesDeploy).getBytes(StandardCharsets.UTF_8)));
        }
    }

    private void removePreviouslyGeneratedFile(AProjectArtefact file, String errorMessage) {
        if (file != null) {
            try {
                file.delete();
            } catch (ProjectException e) {
                throw new Message(errorMessage);
            }
        }
    }

    private String getArtefactPath(String filePath, String basePath) {
        return FileNameFormatter
            .normalizePath(filePath.substring(filePath.lastIndexOf(basePath) + basePath.length() + 1));
    }

    private EnvironmentModel getEnvironmentModel(String moduleName) {
        EnvironmentModel environmentModel = new EnvironmentModel();
        environmentModel.setDependencies(Collections.singletonList(moduleName));
        return environmentModel;
    }

    private InputStream generateDataTypesFile(List<DatatypeModel> datatypeModels) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ExcelFileBuilder.generateDataTypes(datatypeModels, bos);
            byte[] dtBytes = bos.toByteArray();
            return new ByteArrayInputStream(dtBytes);
        }
    }

    private InputStream generateAlgorithmsFile(List<SpreadsheetModel> spreadsheetModels,
            List<DataModel> dataModels,
            EnvironmentModel environmentModel) throws IOException {
        try (ByteArrayOutputStream sos = new ByteArrayOutputStream()) {
            ExcelFileBuilder.generateAlgorithmsModule(spreadsheetModels, dataModels, sos, environmentModel);
            byte[] sprBytes = sos.toByteArray();
            return new ByteArrayInputStream(sprBytes);
        }
    }

    private void tryLockProject() {
        RulesProject currentProject = studio.getCurrentProject();
        if (!currentProject.tryLock()) {
            throw new Message("Project is locked by other user");
        }
    }

    private void save(ProjectDescriptor projectDescriptor) {
        UserWorkspaceProject project = studio.getCurrentProject();
        InputStream rulesDeployContent = null;
        try {
            // validator.validate(descriptor);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            configureSerializer();
            projectDescriptorManager.writeDescriptor(projectDescriptor, byteArrayOutputStream);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

            if (project.hasArtefact(ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME)) {
                AProjectResource artifact = (AProjectResource) project
                    .getArtefact(ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME);
                artifact.setContent(inputStream);
            } else {
                // new
                // ProjectDescriptorManager().writeDescriptor(projectDescriptor,
                // new FileOutputStream(projectDescriptor.getProjectFolder()));
                project.addResource(ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME, inputStream);
                // repositoryTreeState.refreshSelectedNode();
            }

            if (project.hasArtefact(RULES_DEPLOY_XML)) {
                AProjectResource artifact = (AProjectResource) project.getArtefact(RULES_DEPLOY_XML);
                rulesDeployContent = artifact.getContent();
                RulesDeploy rulesDeploy = rulesDeploySerializerFactory.getSerializer(SupportedVersion.getLastVersion())
                    .deserialize(rulesDeployContent);
                artifact.setContent(new ByteArrayInputStream(
                    rulesDeploySerializer.serialize(rulesDeploy).getBytes(StandardCharsets.UTF_8)));
            }

            refreshProject(project.getRepository().getId(), project.getName());
        } catch (ValidationException e) {
            throw new Message(e.getMessage());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new Message("Error while saving the project");
        } finally {
            IOUtils.closeQuietly(rulesDeployContent);
        }
        WebStudioUtils.getWebStudio().resetProjects();
        // postProcess(descriptor);
    }

    private void configureSerializer() throws IOException {
        IProjectDescriptorSerializer serializer;
        SupportedVersion version = supportedVersion;
        if (version == null) {
            version = getSupportedVersion();
        }
        File projectFolder = studio.getCurrentProjectDescriptor().getProjectFolder();
        projectDescriptorSerializerFactory.setSupportedVersion(projectFolder, version);

        serializer = projectDescriptorSerializerFactory.getSerializer(version);
        projectDescriptorManager.setSerializer(serializer);

        rulesDeploySerializer = rulesDeploySerializerFactory.getSerializer(version);
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
        return new SafeCloner().deepClone(projectDescriptor);
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
        return getModuleWithWildcard(module) != null;
    }

    private Module getModuleWithWildcard(Module module) {
        List<Module> modules = getOriginalProjectDescriptor().getModules();

        for (Module otherModule : modules) {
            if (isModuleWithWildcard(otherModule)) {
                List<Module> modulesMatchingPathPattern = getModulesMatchingPathPattern(otherModule);
                for (Module m : modulesMatchingPathPattern) {
                    if (module.getName().equals(m.getName())) {
                        return otherModule;
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

    public SupportedVersion getSupportedVersion() {
        if (supportedVersion != null) {
            return supportedVersion;
        }

        ProjectDescriptor descriptor = studio.getCurrentProjectDescriptor();
        return projectDescriptorSerializerFactory.getSupportedVersion(descriptor.getProjectFolder());
    }

    public void setSupportedVersion(SupportedVersion supportedVersion) {
        this.supportedVersion = supportedVersion;
    }

    public boolean isPropertiesFileNamePatternSupported() {
        return getSupportedVersion().compareTo(SupportedVersion.V5_12) >= 0;
    }

    public boolean isProjectDependenciesSupported() {
        return getSupportedVersion().compareTo(SupportedVersion.V5_12) >= 0;
    }

    public SupportedVersion[] getPossibleVersions() {
        return SupportedVersion.values();
    }

    private ProjectDescriptor getOriginalProjectDescriptor() {
        ProjectDescriptor descriptor = studio.getCurrentProjectDescriptor();
        try {
            File file = new File(descriptor.getProjectFolder(),
                ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME);
            return projectDescriptorManager.readOriginalDescriptor(file);
        } catch (FileNotFoundException ignored) {
            return descriptor;
        } catch (ValidationException e) {
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
