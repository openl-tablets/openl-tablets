package org.openl.rules.webstudio.web;


import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.project.SafeCloner;
import org.openl.rules.project.ProjectDescriptorManager;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.project.instantiation.ReloadType;
import org.openl.rules.project.model.*;
import org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy;
import org.openl.rules.ui.Message;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.ui.util.ListItem;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringTool;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

@ManagedBean
@RequestScoped
public class ProjectBean {

    private WebStudio studio = WebStudioUtils.getWebStudio();

    private final Log log = LogFactory.getLog(ProjectBean.class);

    private List<ListItem<ProjectDependencyDescriptor>> dependencies;
    private String sources;

    public String getModulePath(Module module) {
        PathEntry modulePath = module.getRulesRootPath();

        if (modulePath == null)
            return null;

        String moduleFullPath = modulePath.getPath();
        String projectFullPath = module.getProject().getProjectFolder().getAbsolutePath();

        if (moduleFullPath.contains(projectFullPath)) {
            return moduleFullPath.replace(projectFullPath, "").substring(1);
        }
        return moduleFullPath;
    }

    public List<ListItem<ProjectDependencyDescriptor>> getDependencies() {
        dependencies = new ArrayList<ListItem<ProjectDependencyDescriptor>>();

        ProjectDescriptor currentProject = studio.getCurrentProjectDescriptor();
        List<ProjectDependencyDescriptor> projectDependencies = currentProject.getDependencies();

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
            sources  = "";
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
        }
    }

    // TODO Move messages to ValidationMessages.properties
    public void validateModuleName(FacesContext context, UIComponent toValidate, Object value) {
        String newName = (String) value;
        String oldName = FacesUtils.getRequestParameter("moduleNameOld");

        FacesUtils.validate(StringUtils.isNotBlank(newName), "Can not be empty");

        if (StringUtils.isBlank(oldName)       // Add new Module
                || !oldName.equals(newName)) { // Edit current Module
            FacesUtils.validate(NameChecker.checkName(newName), NameChecker.BAD_NAME_MSG);

            Module module = studio.getModule(studio.getCurrentProjectDescriptor(), newName);
            FacesUtils.validate(module == null, "Module with such name already exists");
        }
    }

    // TODO Move messages to ValidationMessages.properties
    public void validateModulePath(FacesContext context, UIComponent toValidate, Object value) {
        String path = (String) value;
        FacesUtils.validate(StringUtils.isNotBlank(path), "Can not be empty");
    }

    public void editName() {
        ProjectDescriptor projectDescriptor = studio.getCurrentProjectDescriptor();
        ProjectDescriptor newProjectDescriptor = cloneProjectDescriptor(projectDescriptor);

        clean(newProjectDescriptor);
        save(newProjectDescriptor);
    }

    public void editModule() {
        ProjectDescriptor projectDescriptor = studio.getCurrentProjectDescriptor();
        ProjectDescriptor newProjectDescriptor = cloneProjectDescriptor(projectDescriptor);

        String oldName = FacesUtils.getRequestParameter("moduleNameOld");
        String name = FacesUtils.getRequestParameter("moduleName");
        String path = FacesUtils.getRequestParameter("modulePath");
        String type = FacesUtils.getRequestParameter("moduleType");
        String clazz = FacesUtils.getRequestParameter("moduleClass");
        String includes = FacesUtils.getRequestParameter("moduleIncludes");
        String excludes = FacesUtils.getRequestParameter("moduleExcludes");

        Module module;

        // Add new Module
        if (StringUtils.isBlank(oldName)) {
            module = new Module();
            module.setProject(newProjectDescriptor);
            newProjectDescriptor.getModules().add(module);
        // Edit current Module
        } else {
            module = studio.getModule(newProjectDescriptor, oldName);
        }

        if (module != null) {
            module.setName(name);

            PathEntry pathEntry = module.getRulesRootPath();
            if (pathEntry == null) {
                pathEntry = new PathEntry();
                module.setRulesRootPath(pathEntry);
            }
            pathEntry.setPath(path);

            module.setType(ModuleType.valueOf(type));
            if (ModuleType.valueOf(type) != ModuleType.API) {
                module.setClassname(clazz);
            }

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
        ProjectDescriptor projectDescriptor = studio.getCurrentProjectDescriptor();
        ProjectDescriptor newProjectDescriptor = cloneProjectDescriptor(projectDescriptor);

        String toRemove = FacesUtils.getRequestParameter("moduleToRemove");

        List<Module> modules = newProjectDescriptor.getModules();
        for (Module module : modules) {
            if (module.getName().equals(toRemove)) {
                modules.remove(module);
                break;
            }
        }

        clean(newProjectDescriptor);
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

}
