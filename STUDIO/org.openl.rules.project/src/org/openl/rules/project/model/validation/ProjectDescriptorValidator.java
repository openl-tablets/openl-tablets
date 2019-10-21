package org.openl.rules.project.model.validation;

import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.util.StringUtils;

public class ProjectDescriptorValidator {

    public void validate(ProjectDescriptor descriptor) throws ValidationException {
        if (descriptor == null) {
            throw new ValidationException("Project descriptor is null.");
        }

        if (descriptor.getName() == null || descriptor.getName().trim().isEmpty()) {
            throw new ValidationException("Project name aren't defined.");
        }

        if (descriptor.getModules() == null || descriptor.getModules().isEmpty()) {
            throw new ValidationException("Project modules aren't defined.");
        }

        for (Module module : descriptor.getModules()) {
            validateModule(module);
        }
    }

    private void validateModule(Module module) throws ValidationException {

        if (module == null) {
            throw new ValidationException("Project module is not defined.");
        }

        if (StringUtils.isEmpty(module.getName()) && !isModuleWithWildcard(module)) {
            throw new ValidationException("Module name is not defined.");
        }

        if (module.getRulesRootPath() == null || StringUtils.isEmpty(module.getRulesRootPath().getPath())) {
            throw new ValidationException("Module rules root aren't defined.");
        }
    }

    private boolean isModuleWithWildcard(Module module) {
        if (module.getRulesRootPath() != null) {
            String path = module.getRulesRootPath().getPath();
            return path.contains("*") || path.contains("?");
        }
        return false;
    }

}
