package org.openl.rules.project;

import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.util.FileUtils;

public class ProjectDescriptorManager {

    public boolean isCoveredByWildcardModule(ProjectDescriptor descriptor, Module otherModule) {
        final PathEntry otherModuleRootPath = otherModule.getRulesRootPath();
        for (Module module : descriptor.getModules()) {
            if (module.isModuleWithWildcard() && otherModuleRootPath != null) {
                String relativePath = otherModuleRootPath.getPath().replace("\\", "/");
                if (FileUtils.pathMatches(module.getRulesRootPath().getPath(), relativePath)) {
                    return true;
                }
            }
        }
        return false;
    }

}
