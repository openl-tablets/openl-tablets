package org.openl.rules.project.model.validation;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ModuleType;
import org.openl.rules.project.model.ProjectDescriptor;

public class ProjectDescriptorValidator {

    public void validate(ProjectDescriptor descriptor) throws ValidationException {
        if (descriptor == null) {
            throw new ValidationException("Project descriptor is null");
        }

        if (StringUtils.isEmpty(descriptor.getId())) {
            throw new ValidationException("Project ID is not defined");
        }

        if (descriptor.getModules() == null || descriptor.getModules().size() == 0) {
            throw new ValidationException("Project modules is not defined");
        }

        for (Module module : descriptor.getModules()) {
            validateModule(module);
        }
    }

    private void validateModule(Module module) throws ValidationException {

        if (module == null) {
            throw new ValidationException("Project module is not defined");
        }

        if (StringUtils.isEmpty(module.getName())) {
            throw new ValidationException("Module name is not defined");
        }

        if (module.getRulesRootPath() == null || StringUtils.isEmpty(module.getRulesRootPath().getPath())) {
            throw new ValidationException("Module rules root is not defined");
        }

        if (module.getType() == null) {
            throw new ValidationException("Module istantiation type is not defined");
        }

        if (ModuleType.STATIC.equals(module.getType()) || ModuleType.DYNAMIC.equals(module.getType())) {
            if (StringUtils.isEmpty(module.getClassname())) {
                throw new ValidationException("Module java class is not defined");
            }
        }

    }
}
