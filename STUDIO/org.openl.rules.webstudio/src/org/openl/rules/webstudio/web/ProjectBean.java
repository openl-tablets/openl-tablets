package org.openl.rules.webstudio.web;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.IProjectDescriptorSerializer;
import org.openl.rules.project.IRulesDeploySerializer;
import org.openl.rules.project.ProjectDescriptorManager;
import org.openl.rules.project.SafeCloner;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.project.model.MethodFilter;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.project.model.validation.ValidationException;
import org.openl.rules.project.resolving.FileNamePatternValidator;
import org.openl.rules.project.resolving.InvalidFileNamePatternException;
import org.openl.rules.project.resolving.InvalidFileNameProcessorException;
import org.openl.rules.project.resolving.NoMatchFileNameException;
import org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy;
import org.openl.rules.project.resolving.PropertiesFileNameProcessor;
import org.openl.rules.project.resolving.PropertiesFileNameProcessorBuilder;
import org.openl.rules.project.xml.ProjectDescriptorSerializerFactory;
import org.openl.rules.project.xml.RulesDeploySerializerFactory;
import org.openl.rules.project.xml.SupportedVersion;
import org.openl.rules.ui.Message;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.ui.util.ListItem;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.rules.webstudio.web.repository.RepositoryTreeState;
import org.openl.rules.webstudio.web.repository.tree.TreeProject;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.CollectionUtils;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.openl.util.StringTool;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.PathMatcher;

@ManagedBean
@RequestScoped
public class ProjectBean {
    private static final String RULES_DEPLOY_XML = "rules-deploy.xml";
    private final ProjectDescriptorManager projectDescriptorManager = new ProjectDescriptorManager();

    @ManagedProperty(value = "#{repositoryTreeState}")
    private RepositoryTreeState repositoryTreeState;

    @ManagedProperty(value = "#{projectDescriptorSerializerFactory}")
    private ProjectDescriptorSerializerFactory projectDescriptorSerializerFactory;
    @ManagedProperty(value = "#{rulesDeploySerializerFactory}")
    private RulesDeploySerializerFactory rulesDeploySerializerFactory;

    private WebStudio studio = WebStudioUtils.getWebStudio();

    private final Logger log = LoggerFactory.getLogger(ProjectBean.class);

    private List<ListItem<ProjectDependencyDescriptor>> dependencies;
    private String sources;

    private UIInput propertiesFileNameProcessorInput;
    private String propertiesFileNameProcessor;

    private String currentModuleName;

    private SupportedVersion supportedVersion;
    private String newFileName;
    private String currentPathPattern;
    private Integer currentModuleIndex;
    private IRulesDeploySerializer rulesDeploySerializer;

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

    // TODO Move messages to ValidationMessages.properties
    public void validatePropertiesFileNameProcessor(FacesContext context, UIComponent toValidate, Object value) {
        String className = (String) value;

        if (!StringUtils.isBlank(className)) {
            FacesUtils.validate(className.matches("([\\w$]+\\.)*[\\w$]+"), "Invalid class name");

            ProjectDescriptor projectDescriptor = cloneProjectDescriptor(studio.getCurrentProjectDescriptor());
            projectDescriptor.setPropertiesFileNameProcessor(className);
            PropertiesFileNameProcessor processor;
            PropertiesFileNameProcessorBuilder propertiesFileNameProcessorBuilder = new PropertiesFileNameProcessorBuilder();
            try {
                processor = propertiesFileNameProcessorBuilder.build(projectDescriptor);
                FacesUtils.validate(processor != null, "Cannot find class " + className);
            } catch (InvalidFileNameProcessorException e) {
                FacesUtils.throwValidationError(e.getMessage());
            } finally {
                propertiesFileNameProcessorBuilder.destroy();
            }
        }
    }

    // TODO Move messages to ValidationMessages.properties
    public void validatePropertiesFileNamePattern(FacesContext context, UIComponent toValidate, Object value) {
        String pattern = (String) value;

        if (StringUtils.isNotBlank(pattern)) {
            PropertiesFileNameProcessor processor;
            PropertiesFileNameProcessorBuilder propertiesFileNameProcessorBuilder = new PropertiesFileNameProcessorBuilder();
            try {
                ProjectDescriptor projectDescriptor = cloneProjectDescriptor(studio.getCurrentProjectDescriptor());
                projectDescriptor.setPropertiesFileNameProcessor((String) propertiesFileNameProcessorInput.getValue());
                projectDescriptor.setPropertiesFileNamePattern(pattern);
                processor = propertiesFileNameProcessorBuilder.build(projectDescriptor);
                if (processor instanceof FileNamePatternValidator) {
                    ((FileNamePatternValidator) processor).validate(pattern);
                }
            } catch (InvalidFileNamePatternException e) {
                FacesUtils.throwValidationError(e.getMessage());
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
        String oldName = FacesUtils.getRequestParameter("moduleNameOld");
        String modulePath = FacesUtils.getRequestParameter("modulePath");

        Module toCheck = new Module();
        toCheck.setRulesRootPath(new PathEntry(modulePath));
        boolean withWildcard = isModuleWithWildcard(toCheck);
        if (!withWildcard) {
            FacesUtils.validate(StringUtils.isNotBlank(newName), "Cannot be empty");
        }

        if (StringUtils.isBlank(oldName) // Add new Module
                || !oldName.equals(newName)) { // Edit current Module
            if (!withWildcard || StringUtils.isNotBlank(newName)) {
                FacesUtils.validate(NameChecker.checkName(newName), NameChecker.BAD_NAME_MSG);

                Module module = studio.getModule(studio.getCurrentProjectDescriptor(), newName);
                FacesUtils.validate(module == null, "Module with such name already exists");
            }
        }
    }

    public void validateModuleNameForCopy(FacesContext context, UIComponent toValidate, Object value) {
        String newName = (String) value;
        String oldName = FacesUtils.getRequestParameter("copyModuleForm:moduleNameOld");
        String modulePath = FacesUtils.getRequestParameter("copyModuleForm:modulePath");

        Module toCheck = new Module();
        toCheck.setRulesRootPath(new PathEntry(modulePath));
        boolean withWildcard = isModuleWithWildcard(toCheck);
        if (!withWildcard) {
            FacesUtils.validate(StringUtils.isNotBlank(newName), "Cannot be empty");
        }

        if (StringUtils.isBlank(oldName) // Add new Module
                || !oldName.equals(newName)) { // Edit current Module
            if (!withWildcard || StringUtils.isNotBlank(newName)) {
                FacesUtils.validate(NameChecker.checkName(newName), NameChecker.BAD_NAME_MSG);

                Module module = studio.getModule(studio.getCurrentProjectDescriptor(), newName);
                FacesUtils.validate(module == null, "Module with such name already exists");
            }
        }
    }

    // TODO Move messages to ValidationMessages.properties
    public void validateModulePath(FacesContext context, UIComponent toValidate, Object value) {
        String path = (String) value;
        FacesUtils.validate(StringUtils.isNotBlank(path), "Cannot be empty");

        if (!(path.contains("*") || path.contains("?"))) {
            File moduleFile = new File(studio.getCurrentProjectDescriptor().getProjectFolder(), path);
            FacesUtils.validate(moduleFile.exists(), "File with such path does not exist");
        }
    }

    public void validateModulePathForCopy(FacesContext context, UIComponent toValidate, Object value) {
        String path = FacesUtils.getRequestParameter("copyModuleForm:modulePath");
        FacesUtils.validate(StringUtils.isNotBlank(path), "Cannot be empty");

        FacesUtils.validate(!(path.contains("*") || path.contains("?")), "Path cannot contain wildcard symbols");
        File moduleFile = new File(studio.getCurrentProjectDescriptor().getProjectFolder(), path);
        FacesUtils.validate(!moduleFile.exists(), "File with such name already exists");
    }

    public void editName() {
        tryLockProject();

        ProjectDescriptor projectDescriptor = studio.getCurrentProjectDescriptor();
        ProjectDescriptor newProjectDescriptor = cloneProjectDescriptor(projectDescriptor);

        RulesProject currentProject = studio.getCurrentProject();
        if (studio.isRenamed(currentProject)) {
            // Restore physical project name
            newProjectDescriptor.setName(currentProject.getName());
        }

        clean(newProjectDescriptor);
        save(newProjectDescriptor);
    }

    public void editModule() {
        tryLockProject();

        ProjectDescriptor projectDescriptor = getOriginalProjectDescriptor();
        ProjectDescriptor newProjectDescriptor = cloneProjectDescriptor(projectDescriptor);

        String index = FacesUtils.getRequestParameter("moduleIndex");
        String oldName = FacesUtils.getRequestParameter("moduleNameOld");
        String name = FacesUtils.getRequestParameter("moduleName");
        String path = FacesUtils.getRequestParameter("modulePath");
        String includes = FacesUtils.getRequestParameter("moduleIncludes");
        String excludes = FacesUtils.getRequestParameter("moduleExcludes");

        Module module;

        if (StringUtils.isBlank(oldName) && StringUtils.isBlank(index)) {
            // Add new Module
            module = new Module();
            module.setProject(newProjectDescriptor);
            newProjectDescriptor.getModules().add(module);
        } else {
            // Edit current Module
            if (!StringUtils.isBlank(oldName)) {
                module = studio.getModule(newProjectDescriptor, oldName);
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

            clean(newProjectDescriptor);
            save(newProjectDescriptor);
        }
    }

    public void copyModule() {
        tryLockProject();

        ProjectDescriptor projectDescriptor = getOriginalProjectDescriptor();
        ProjectDescriptor newProjectDescriptor = cloneProjectDescriptor(projectDescriptor);

        String name = FacesUtils.getRequestParameter("copyModuleForm:moduleName");
        String oldPath = FacesUtils.getRequestParameter("copyModuleForm:modulePathOld");
        String path = FacesUtils.getRequestParameter("copyModuleForm:modulePath");

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
            refreshProject(studio.getCurrentProject().getName());
        }
    }

    private void refreshProject(String name) {
        studio.resolveProject(studio.getCurrentProjectDescriptor());
        TreeProject projectNode = repositoryTreeState.getProjectNodeByPhysicalName(name);
        if (projectNode != null) {
            // For example, repository wasn't refreshed yet
            projectNode.refresh();
        }
    }

    public void removeModule() {
        tryLockProject();

        ProjectDescriptor projectDescriptor = getOriginalProjectDescriptor();
        ProjectDescriptor newProjectDescriptor = cloneProjectDescriptor(projectDescriptor);

        String toRemove = FacesUtils.getRequestParameter("moduleToRemove");
        String leaveExcelFile = FacesUtils.getRequestParameter("leaveExcelFile");

        List<Module> modules = newProjectDescriptor.getModules();
        Module removed = modules.remove(Integer.parseInt(toRemove));

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
                save(newProjectDescriptor);
            } else {
                refreshProject(studio.getCurrentProject().getName());
            }
        } else {
            clean(newProjectDescriptor);
            save(newProjectDescriptor);
        }

    }

    public void removeDependency(String name) {
        tryLockProject();

        ProjectDescriptor projectDescriptor = studio.getCurrentProjectDescriptor();
        ProjectDescriptor newProjectDescriptor = cloneProjectDescriptor(projectDescriptor);

        clean(newProjectDescriptor);

        List<ProjectDependencyDescriptor> resultDependencies = newProjectDescriptor.getDependencies();

        for (ProjectDependencyDescriptor dependency : new ArrayList<>(resultDependencies)) {
            if (dependency.getName().equals(name)) {
                resultDependencies.remove(dependency);
            }
        }

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

    private void tryLockProject() {
        RulesProject currentProject = studio.getCurrentProject();
        try {
            if (!currentProject.tryLock()) {
                throw new Message("Project is locked by other user");
            }
        } catch (ProjectException e) {
            log.error(e.getMessage(), e);
            throw new Message("Error while project locking");
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
                AProjectResource artefact = (AProjectResource) project
                    .getArtefact(ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME);
                artefact.setContent(inputStream);
            } else {
                // new
                // ProjectDescriptorManager().writeDescriptor(projectDescriptor,
                // new FileOutputStream(projectDescriptor.getProjectFolder()));
                project.addResource(ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME, inputStream);
                // repositoryTreeState.refreshSelectedNode();
            }

            if (project.hasArtefact(RULES_DEPLOY_XML)) {
                AProjectResource artefact = (AProjectResource) project.getArtefact(RULES_DEPLOY_XML);
                rulesDeployContent = artefact.getContent();
                RulesDeploy rulesDeploy = rulesDeploySerializerFactory.getSerializer(SupportedVersion.getLastVersion())
                    .deserialize(rulesDeployContent);
                artefact.setContent(
                    new ByteArrayInputStream(rulesDeploySerializer.serialize(rulesDeploy).getBytes("UTF-8")));
            }

            refreshProject(project.getName());
        } catch (ValidationException e) {
            throw new Message(e.getMessage());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new Message("Error while saving the project");
        } finally {
            IOUtils.closeQuietly(rulesDeployContent);
        }
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

        if (StringUtils.isBlank(descriptor.getPropertiesFileNamePattern())) {
            descriptor.setPropertiesFileNamePattern(null);
        }

        if (StringUtils.isBlank(descriptor.getPropertiesFileNameProcessor())) {
            descriptor.setPropertiesFileNameProcessor(null);
        }

        if (CollectionUtils.isEmpty(descriptor.getDependencies())) {
            descriptor.setDependencies(null);
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

    public void setRepositoryTreeState(RepositoryTreeState repositoryTreeState) {
        this.repositoryTreeState = repositoryTreeState;
    }

    public void setProjectDescriptorSerializerFactory(
            ProjectDescriptorSerializerFactory projectDescriptorSerializerFactory) {
        this.projectDescriptorSerializerFactory = projectDescriptorSerializerFactory;
    }

    public void setRulesDeploySerializerFactory(RulesDeploySerializerFactory rulesDeploySerializerFactory) {
        this.rulesDeploySerializerFactory = rulesDeploySerializerFactory;
    }

    public UIInput getPropertiesFileNameProcessorInput() {
        return propertiesFileNameProcessorInput;
    }

    public void setPropertiesFileNameProcessorInput(UIInput propertiesFileNameProcessorInput) {
        this.propertiesFileNameProcessorInput = propertiesFileNameProcessorInput;
    }

    public String getPropertiesFileNameProcessor() {
        return propertiesFileNameProcessor;
    }

    public void setPropertiesFileNameProcessor(String propertiesFileNameProcessor) {
        this.propertiesFileNameProcessor = propertiesFileNameProcessor;
    }

    public String getCurrentModuleName() {
        return currentModuleName;
    }

    public void setCurrentModuleName(String currentModuleName) {
        this.currentModuleName = currentModuleName;
    }

    public String getPropertiesFileNamePatternDescription() {
        ProjectDescriptor projectDescriptor = cloneProjectDescriptor(studio.getCurrentProjectDescriptor());
        projectDescriptor.setPropertiesFileNameProcessor(propertiesFileNameProcessor);
        PropertiesFileNameProcessor processor;
        PropertiesFileNameProcessorBuilder propertiesFileNameProcessorBuilder = new PropertiesFileNameProcessorBuilder();
        try {
            processor = propertiesFileNameProcessorBuilder.build(projectDescriptor);

            Class<? extends PropertiesFileNameProcessor> processorClass = processor.getClass();
            String fileName = "/" + processorClass.getName().replace(".", "/") + ".info";

            try {
                InputStream inputStream = processorClass.getResourceAsStream(fileName);
                if (inputStream == null) {
                    throw new FileNotFoundException(String.format("File '%s' is not found.", fileName));
                }
                return IOUtils.toStringAndClose(inputStream);
            } catch (FileNotFoundException e) {
                return "Description file " + fileName + " is absent";
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                return "Cannot load the file " + fileName;
            }
        } catch (InvalidFileNameProcessorException e) {
            return "Incorrect file name processor class '" + propertiesFileNameProcessor + "'";
        } finally {
            propertiesFileNameProcessorBuilder.destroy();
        }
    }

    public String getPropertiesPatternWarnings() {
        ProjectDescriptor projectDescriptor = cloneProjectDescriptor(studio.getCurrentProjectDescriptor());
        projectDescriptor.setPropertiesFileNameProcessor(propertiesFileNameProcessor);
        PropertiesFileNameProcessor processor;
        PropertiesFileNameProcessorBuilder propertiesFileNameProcessorBuilder = new PropertiesFileNameProcessorBuilder();
        try {
            processor = propertiesFileNameProcessorBuilder.build(projectDescriptor);
            if (!(processor instanceof FileNamePatternValidator)) {
                return "Validation is not supported";
            }
        } catch (InvalidFileNameProcessorException ignored) {
        } finally {
            propertiesFileNameProcessorBuilder.destroy();
        }

        return "";
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

        Module module = new Module();
        int indexOfSlash = newFileName.lastIndexOf("/");
        module.setName(indexOfSlash < 0 ? newFileName : newFileName.substring(indexOfSlash + 1));
        module.setRulesRootPath(new PathEntry(newFileName));

        Boolean fileNameMatched = null;
        try {
            String pattern = projectDescriptor.getPropertiesFileNamePattern();
            if (pattern != null) {
                builder.build(projectDescriptor).process(module, pattern);
                fileNameMatched = true;
            }
        } catch (InvalidFileNameProcessorException ignored) {
            // Cannot check for name correctness
        } catch (InvalidFileNamePatternException e) {
            // Invalid pattern, cannot check for name correctness
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
            if (!excludes.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    public String getPropertiesFileNamePattern() {
        return studio.getCurrentProjectDescriptor().getPropertiesFileNamePattern();
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
}