package org.openl.rules.webstudio.web;

import com.rits.cloning.Cloner;
import com.sdicons.json.model.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.common.ProjectDependency;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.IProjectDescriptorSerializer;
import org.openl.rules.project.ProjectDescriptorManager;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.project.instantiation.ReloadType;
import org.openl.rules.project.model.*;
import org.openl.rules.project.model.validation.ValidationException;
import org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy;
import org.openl.rules.project.xml.XmlProjectDescriptorSerializer;
import org.openl.rules.ui.Message;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.ui.util.ListItem;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@ManagedBean
@RequestScoped
public class ProjectBean {

    private final Log log = LogFactory.getLog(ProjectBean.class);

    private List<ListItem<ProjectDependencyDescriptor>> dependencies;

    public void init() throws Exception {
        String projectName = FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_NAME);
        WebStudio studio = WebStudioUtils.getWebStudio();
        studio.selectProject(projectName);
    }

    public String getModulePath(Module module) {
        PathEntry modulePath = module.getRulesRootPath();

        if (modulePath == null)
            return null;

        String moduleFullPath = modulePath.getPath();
        String projectFullPath = module.getProject().getProjectFolder().getAbsolutePath();

        return moduleFullPath.replace(projectFullPath, "").substring(1);
    }

    public List<ListItem<ProjectDependencyDescriptor>> getDependencies() {
        dependencies = new ArrayList<ListItem<ProjectDependencyDescriptor>>();
        WebStudio studio = WebStudioUtils.getWebStudio();

        ProjectDescriptor currentProject = studio.getCurrentProjectDescriptor();
        List<ProjectDependencyDescriptor> projectDependencies = currentProject.getDependencies();

        List<ProjectDescriptor> projects = studio.getAllProjects();
        for (ProjectDescriptor project : projects) {
            String name = project.getName();
            if (!name.equals(currentProject.getName())) {
                ProjectDependencyDescriptor dependency = new ProjectDependencyDescriptor();
                ProjectDependencyDescriptor projectDependency = studio.getProjectDependency(name);
                dependency.setName(name);
                dependency.setAutoIncluded(projectDependency != null ? projectDependency.isAutoIncluded() : true);
                dependencies.add(
                        new ListItem<ProjectDependencyDescriptor>(projectDependency != null, dependency));
            }
        }
        return dependencies;
    }

    // TODO Move messages to ValidationMessages.properties
    public void validateProjectName(FacesContext context, UIComponent toValidate, Object value) {
        if (StringUtils.isBlank((String) value)) {
            throw new ValidatorException(
                    new FacesMessage("Can not be empty"));
        }

        if  (!WebStudioUtils.getWebStudio().getCurrentProjectDescriptor().getName().equals((String) value)) {
            if (!NameChecker.checkName((String) value)) {
                throw new ValidatorException(
                        new FacesMessage(NameChecker.BAD_PROJECT_NAME_MSG));
            }

            if (WebStudioUtils.getWebStudio().isProjectExists((String) value)) {
                throw new ValidatorException(
                        new FacesMessage("Project with such name already exists"));
            }
        }
    }

    public void editName() {
        WebStudio studio = WebStudioUtils.getWebStudio();

        ProjectDescriptor projectDescriptor = studio.getCurrentProjectDescriptor();
        ProjectDescriptor newProjectDescriptor = new Cloner().deepClone(projectDescriptor);

        clean(newProjectDescriptor);

        save(newProjectDescriptor);
    }

    public void editDependencies() {
        WebStudio studio = WebStudioUtils.getWebStudio();

        ProjectDescriptor projectDescriptor = studio.getCurrentProjectDescriptor();
        ProjectDescriptor newProjectDescriptor = new Cloner().deepClone(projectDescriptor);

        clean(newProjectDescriptor);

        List<ProjectDependencyDescriptor> resultDependencies = new ArrayList<ProjectDependencyDescriptor>();

        for (ListItem<ProjectDependencyDescriptor> dependency : dependencies) {
            if (dependency.isSelected()) {
                resultDependencies.add((ProjectDependencyDescriptor) dependency.getItem());
            }
        }

        newProjectDescriptor.setDependencies(!resultDependencies.isEmpty() ? resultDependencies : null);

        save(newProjectDescriptor);
    }

    private void save(ProjectDescriptor projectDescriptor) {
        WebStudio studio = WebStudioUtils.getWebStudio();
        UserWorkspaceProject project = studio.getCurrentProject();

        final IProjectDescriptorSerializer serializer = new XmlProjectDescriptorSerializer();
        try {
            //validator.validate(descriptor);

            ByteArrayInputStream inputStream = new ByteArrayInputStream(serializer.serialize(projectDescriptor).getBytes());

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
            log.error(e);
            throw new Message("Error while updating project");
        }
        //postProcess(descriptor);
    }

    private void clean(ProjectDescriptor descriptor) {
        if (CollectionUtils.isEmpty(descriptor.getClasspath())) {
            descriptor.setClasspath(null);
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
                }
            }
        }

        descriptor.setProjectFolder(null);
    }

}
