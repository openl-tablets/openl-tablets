package org.openl.rules.project;

import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.util.FileUtils;

public class ProjectDescriptorManager {

    public boolean isCoveredByWildcardModule(ProjectDescriptor descriptor, Module otherModule) {
        final String otherModuleRootPath = otherModule.getRulesRootPath();
        for (Module module : descriptor.getModules()) {
            if (module.isModuleWithWildcard() && otherModuleRootPath != null) {
                String relativePath = otherModuleRootPath.replace("\\", "/");
                if (FileUtils.pathMatches(module.getRulesRootPath(), relativePath)) {
                    return true;
                }
            }
        }
        return false;
    }

}
