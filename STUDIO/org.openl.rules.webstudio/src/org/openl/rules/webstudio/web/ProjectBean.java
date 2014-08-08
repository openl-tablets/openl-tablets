package org.openl.rules.webstudio.web;


import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.project.ProjectDescriptorManager;
import org.openl.rules.project.SafeCloner;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.project.instantiation.ReloadType;
import org.openl.rules.project.model.*;
import org.openl.rules.project.model.validation.ValidationException;
import org.openl.rules.project.resolving.*;
import org.openl.rules.ui.Message;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.ui.util.ListItem;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.rules.webstudio.web.repository.RepositoryTreeState;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@ManagedBean
@RequestScoped
public class ProjectBean {
    private static final String PROJECT_DESCRIPTOR_FILE = "rules.xml";

    @ManagedProperty(value = "#{repositoryTreeState}")
    private RepositoryTreeState repositoryTreeState;

    private WebStudio studio = WebStudioUtils.getWebStudio();

    private final Logger log = LoggerFactory.getLogger(ProjectBean.class);

    private List<ListItem<ProjectDependencyDescriptor>> dependencies;
    private String sources;

    private UIInput propertiesFileNameProcessorInput;
    private String propertiesFileNameProcessor;

    public String getModulePath(Module module) {
        PathEntry modulePath = module.getRulesRootPath();

        if (modulePath == null)
            return null;

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
        dependencies = new ArrayList<ListItem<ProjectDependencyDescriptor>>();

        ProjectDescriptor currentProject = studio.getCurrentProjectDescriptor();

        List<ProjectDescriptor> projects = studio.getAllProjects();
        for (ProjectDescriptor project : projects) {
            String name = project.getName();
            if (!name.equals(currentProject.getName())) {
                ProjectDependencyDescriptor dependency = new ProjectDependencyDescriptor();
                ProjectDependencyDescriptor projectDependency = studio.getProjectDependency(name);
                dependency.setName(name);
                dependency.setAutoIncluded(projectDependency == null || projectDependency.isAutoIncluded());
                dependencies.add(
                        new ListItem<ProjectDependencyDescriptor>(projectDependency != null, dependency));
            }
        }
        return dependencies;
    }

    public String getSources() {
        ProjectDescriptor currentProject = studio.getCurrentProjectDescriptor();
        List<PathEntry> sourceList = currentProject.getClasspath();
        if (sourceList != null) {
            sources = "";
            for (PathEntry source : sourceList) {
                sources += source.getPath() + StringTool.NEW_LINE;
            }
        }

        return sources;
    }

    public void setSources(String sources) {
        this.sources = sources;
    }

    // TODO Move messages to ValidationMessages.properties
    public void validateProjectName(FacesContext context, UIComponent toValidate, Object value) {
        String name = (String) value;

        FacesUtils.validate(StringUtils.isNotBlank(name), "Can not be empty");

        if (!studio.getCurrentProjectDescriptor().getName().equals(name)) {
            FacesUtils.validate(NameChecker.checkName(name), NameChecker.BAD_PROJECT_NAME_MSG);
            FacesUtils.validate(!studio.isProjectExists(name), "Project with such name already exists");
        }
    }

    // TODO Move messages to ValidationMessages.properties
    public void validatePropertiesFileNameProcessor(FacesContext context, UIComponent toValidate, Object value) {
        String className = (String) value;

        if (!StringUtils.isBlank(className)) {
            FacesUtils.validate(className.matches("([\\w$]+\\.)*[\\w$]+"), "Invalid class name");

            ProjectDescriptor projectDescriptor = cloneProjectDescriptor(studio.getCurrentProjectDescriptor());
            projectDescriptor.setPropertiesFileNameProcessor(className);
            PropertiesFileNameProcessor processor = null;
            PropertiesFileNameProcessorBuilder propertiesFileNameProcessorBuilder = new PropertiesFileNameProcessorBuilder();
            try {
                processor = propertiesFileNameProcessorBuilder.build(projectDescriptor);
                FacesUtils.validate(processor != null, "Can't find class " + className);
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

        if (!StringUtils.isBlank(pattern)) {
            PropertiesFileNameProcessor processor = null;
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
            FacesUtils.validate(StringUtils.isNotBlank(newName), "Can not be empty");
        }

        if (StringUtils.isBlank(oldName)       // Add new Module
                || !oldName.equals(newName)) { // Edit current Module
            if (!withWildcard || !StringUtils.isBlank(newName)) {
                FacesUtils.validate(NameChecker.checkName(newName), NameChecker.BAD_NAME_MSG);

                Module module = studio.getModule(studio.getCurrentProjectDescriptor(), newName);
                FacesUtils.validate(module == null, "Module with such name already exists");
            }
        }
    }

    // TODO Move messages to ValidationMessages.properties
    public void validateModulePath(FacesContext context, UIComponent toValidate, Object value) {
        String path = (String) value;
        FacesUtils.validate(StringUtils.isNotBlank(path), "Can not be empty");

        if (!(path.contains("*") || path.contains("?"))) {
            File moduleFile = new File(studio.getCurrentProjectDescriptor().getProjectFolder(), path);
            FacesUtils.validate(moduleFile.exists(), "File with such path doesn't exist");
        }
    }

    public void editName() {
        ProjectDescriptor projectDescriptor = studio.getCurrentProjectDescriptor();
        ProjectDescriptor newProjectDescriptor = cloneProjectDescriptor(projectDescriptor);

        clean(newProjectDescriptor);
        save(newProjectDescriptor);
    }

    public void editModule() {
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

    public void removeModule() {
        ProjectDescriptor projectDescriptor = getOriginalProjectDescriptor();
        ProjectDescriptor newProjectDescriptor = cloneProjectDescriptor(projectDescriptor);

        String toRemove = FacesUtils.getRequestParameter("moduleToRemove");

        List<Module> modules = newProjectDescriptor.getModules();
        modules.remove(Integer.parseInt(toRemove));

        clean(newProjectDescriptor);
        save(newProjectDescriptor);
    }

    public void removeDependency(String name) {
        ProjectDescriptor projectDescriptor = studio.getCurrentProjectDescriptor();
        ProjectDescriptor newProjectDescriptor = cloneProjectDescriptor(projectDescriptor);

        clean(newProjectDescriptor);

        List<ProjectDependencyDescriptor> resultDependencies = newProjectDescriptor.getDependencies();

        for (ProjectDependencyDescriptor dependency : new ArrayList<ProjectDependencyDescriptor>(resultDependencies)) {
            if (dependency.getName().equals(name)) {
                resultDependencies.remove(dependency);
            }
        }

        newProjectDescriptor.setDependencies(!resultDependencies.isEmpty() ? resultDependencies : null);

        save(newProjectDescriptor);
    }

    public void editDependencies() {
        ProjectDescriptor projectDescriptor = studio.getCurrentProjectDescriptor();
        ProjectDescriptor newProjectDescriptor = cloneProjectDescriptor(projectDescriptor);

        clean(newProjectDescriptor);

        List<ProjectDependencyDescriptor> resultDependencies = new ArrayList<ProjectDependencyDescriptor>();

        for (ListItem<ProjectDependencyDescriptor> dependency : dependencies) {
            if (dependency.isSelected()) {
                resultDependencies.add(dependency.getItem());
            }
        }

        newProjectDescriptor.setDependencies(!resultDependencies.isEmpty() ? resultDependencies : null);

        save(newProjectDescriptor);
    }

    public void editSources() {
        List<PathEntry> sourceList = new ArrayList<PathEntry>();
        String[] sourceArray = sources.split(StringTool.NEW_LINE);

        if (ArrayUtils.isNotEmpty(sourceArray)) {
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


    private static final ProjectDescriptorManager projectDescriptorManager = new ProjectDescriptorManager();

    private void save(ProjectDescriptor projectDescriptor) {
        UserWorkspaceProject project = studio.getCurrentProject();
        try {
            //validator.validate(descriptor);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            projectDescriptorManager.writeDescriptor(projectDescriptor, byteArrayOutputStream);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

            if (project.hasArtefact(ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME)) {
                AProjectResource artefact = (AProjectResource) project.getArtefact(
                        ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME);
                artefact.setContent(inputStream);
            } else {
                //new ProjectDescriptorManager().writeDescriptor(projectDescriptor,
                //        new FileOutputStream(projectDescriptor.getProjectFolder()));
                project.addResource(ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME, inputStream);
                //repositoryTreeState.refreshSelectedNode();
            }
            studio.reset(ReloadType.FORCED);
            repositoryTreeState.getProjectNodeByPhysicalName(project.getName()).refresh();
        } catch (ValidationException e) {
            throw new Message(e.getMessage());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new Message("Error while saving the project");
        }
        //postProcess(descriptor);
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
                if (CollectionUtils.isEmpty(methodFilter.getIncludes())
                        && CollectionUtils.isEmpty(methodFilter.getExcludes())) {
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

    public String getPropertiesFileNamePatternDescription() {
        ProjectDescriptor projectDescriptor = cloneProjectDescriptor(studio.getCurrentProjectDescriptor());
        projectDescriptor.setPropertiesFileNameProcessor(propertiesFileNameProcessor);
        PropertiesFileNameProcessor processor = null;
        PropertiesFileNameProcessorBuilder propertiesFileNameProcessorBuilder = new PropertiesFileNameProcessorBuilder();
        try {
            processor = propertiesFileNameProcessorBuilder.build(projectDescriptor);

            Class<? extends PropertiesFileNameProcessor> processorClass = processor.getClass();
            String fileName = "/" + processorClass.getName().replace(".", "/") + ".info";
            InputStream inputStream = null;
            try {
                inputStream = processorClass.getResourceAsStream(fileName);
                if (inputStream == null) {
                    throw new FileNotFoundException("File " + fileName + " not found");
                }
                return IOUtils.toString(inputStream, "UTF-8");
            } catch (FileNotFoundException e) {
                return "Description file " + fileName + " is absent";
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                return "Can't load the file " + fileName;
            } finally {
                IOUtils.closeQuietly(inputStream);
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
        PropertiesFileNameProcessor processor = null;
        PropertiesFileNameProcessorBuilder propertiesFileNameProcessorBuilder = new PropertiesFileNameProcessorBuilder();
        try {
            processor = propertiesFileNameProcessorBuilder.build(projectDescriptor);
            if (!(processor instanceof FileNamePatternValidator)) {
                return "Validation isn't supported";
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

    public List<Module> getModulesMatchingPathPattern(Module module) {
        if (module == null || !projectDescriptorManager.isModuleWithWildcard(module)) {
            return Collections.emptyList();
        }

        return projectDescriptorManager.getAllModulesMatchingPathPattern(studio.getCurrentProjectDescriptor(), module, module.getRulesRootPath().getPath());
    }

    public boolean isEmptyMethodFilter(Module module) {
        MethodFilter methodFilter = module.getMethodFilter();
        if (methodFilter == null) {
            return true;
        }

        if (methodFilter.getIncludes() != null) {
            ArrayList<String> includes = new ArrayList<String>(methodFilter.getIncludes());
            includes.removeAll(Arrays.asList("", null));
            if (!includes.isEmpty()) {
                return false;
            }
        }
        if (methodFilter.getExcludes() != null) {
            ArrayList<String> excludes = new ArrayList<String>(methodFilter.getExcludes());
            excludes.removeAll(Arrays.asList("", null));
            if (!excludes.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private ProjectDescriptor getOriginalProjectDescriptor() {
        ProjectDescriptor descriptor = studio.getCurrentProjectDescriptor();
        try {
            File file = new File(descriptor.getProjectFolder(), PROJECT_DESCRIPTOR_FILE);
            return projectDescriptorManager.readOriginalDescriptor(file);
        } catch (FileNotFoundException ignored) {
            return descriptor;
        } catch (ValidationException e) {
            log.error(e.getMessage(), e);
            return descriptor;
        }
    }
}