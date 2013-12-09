package org.openl.rules.webstudio.web;

import com.rits.cloning.Cloner;
import com.sdicons.json.model.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.IProjectDescriptorSerializer;
import org.openl.rules.project.ProjectDescriptorManager;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.project.instantiation.ReloadType;
import org.openl.rules.project.model.MethodFilter;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.validation.ValidationException;
import org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy;
import org.openl.rules.project.xml.XmlProjectDescriptorSerializer;
import org.openl.rules.ui.Message;
import org.openl.rules.ui.WebStudio;
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
import java.util.List;

@ManagedBean
@RequestScoped
public class ProjectBean {

    private final Log log = LogFactory.getLog(ProjectBean.class);

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
        final IProjectDescriptorSerializer serializer = new XmlProjectDescriptorSerializer();

        WebStudio studio = WebStudioUtils.getWebStudio();

        UserWorkspaceProject project = studio.getCurrentProject();
        ProjectDescriptor projectDescriptor = studio.getCurrentProjectDescriptor();

        try {
            ProjectDescriptor newProjectDescriptor = new Cloner().deepClone(projectDescriptor);

            clean(newProjectDescriptor);
            //validator.validate(descriptor);

            ByteArrayInputStream inputStream = new ByteArrayInputStream(serializer.serialize(newProjectDescriptor).getBytes());

            if (project.hasArtefact(ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME)) {
                AProjectResource artefact = (AProjectResource) project.getArtefact(
                        ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME);
                artefact.setContent(inputStream);
            } else {
                //new ProjectDescriptorManager().writeDescriptor(projectDescriptor,
                //        new FileOutputStream(projectDescriptor.getProjectFolder()));
                project.addResource(ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME, inputStream);
                //repositoryTreeState.refreshSelectedNode();
                //studio.reset(ReloadType.FORCED);
            }
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
