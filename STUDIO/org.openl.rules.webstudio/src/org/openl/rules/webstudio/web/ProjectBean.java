package org.openl.rules.webstudio.web;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.project.model.Module;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

@ManagedBean
@RequestScoped
public class ProjectBean {

    public void init() throws Exception {
        String projectName = FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_NAME);

        WebStudio studio = WebStudioUtils.getWebStudio();
        studio.selectProject(projectName);
    }

    public String getModulePath(Module module) {
        String moduleFullPath = module.getRulesRootPath().getPath();
        String projectFullPath = module.getProject().getProjectFolder().getAbsolutePath();

        return moduleFullPath.replace(projectFullPath, "");
    }

}
