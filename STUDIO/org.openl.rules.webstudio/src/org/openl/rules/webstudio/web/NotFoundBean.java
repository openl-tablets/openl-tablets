package org.openl.rules.webstudio.web;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

@ManagedBean
@RequestScoped
public class NotFoundBean {
    public String getProject() {
        return FacesUtils.getRequestParameter("project");
    }

    public String getModule() {
        return FacesUtils.getRequestParameter("module");
    }

    public Type getType() {
        String project = getProject();
        if (project != null) {
            ProjectDescriptor projectDescriptor = WebStudioUtils.getWebStudio().getProjectByName(project);
            if (projectDescriptor == null) {
                return Type.PROJECT;
            }

            String module = getModule();
            if (module != null && WebStudioUtils.getWebStudio().getModule(projectDescriptor, module) == null) {
                return Type.MODULE;
            }
        }

        return Type.OTHER;
    }

    public enum Type {
        PROJECT,
        MODULE,
        OTHER
    }
}
