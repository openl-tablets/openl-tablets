package org.openl.rules.webstudio.web;

import com.sun.swing.internal.plaf.metal.resources.metal;
import org.apache.commons.lang.StringUtils;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDescriptor;
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

@ManagedBean
@RequestScoped
public class ProjectBean {

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

    public String getName() {
        return WebStudioUtils.getWebStudio().getCurrentProjectDescriptor().getName();
    }

    public void setName(String name) {
        int i = 5;
    }

    // TODO Move messages to ValidationMessages.properties
    public void validateProjectName(FacesContext context, UIComponent toValidate, Object value) {
        if (StringUtils.isBlank((String) value)) {
            throw new ValidatorException(
                    new FacesMessage("Can not be empty"));
        }

        if  (!WebStudioUtils.getWebStudio().getCurrentProject().getName().equals((String) value)) {
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
        ProjectDescriptor currentProject = WebStudioUtils.getWebStudio().getCurrentProjectDescriptor();
        int i =9;
    }

}
