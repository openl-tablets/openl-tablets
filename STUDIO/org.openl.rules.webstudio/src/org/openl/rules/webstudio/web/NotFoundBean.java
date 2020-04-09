package org.openl.rules.webstudio.web;

import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.context.annotation.RequestScope;

@Controller
@RequestScope
public class NotFoundBean {
    public String getProject() {
        return WebStudioUtils.getRequestParameter("project");
    }

    public String getModule() {
        return WebStudioUtils.getRequestParameter("module");
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
