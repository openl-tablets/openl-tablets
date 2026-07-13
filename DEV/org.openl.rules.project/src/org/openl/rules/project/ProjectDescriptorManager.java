package org.openl.rules.project;

import java.util.List;

import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.util.FileUtils;

public class ProjectDescriptorManager {

    /**
     * Tells whether a module path is already matched by a wildcard module of the project.
     *
     * <p>When rules.xml declares no modules, the project relies on the implicit default modules
     * ({@link ProjectDescriptor#defaultModules()}), so those are checked instead of the empty declared list. A file
     * under {@code rules/} or {@code tests/} is therefore reported as covered even when rules.xml has no modules.
     */
    public boolean isCoveredByWildcardModule(ProjectDescriptor descriptor, Module otherModule) {
        final String otherModuleRootPath = otherModule.getRulesRootPath();
        for (Module module : effectiveModules(descriptor)) {
            if (module.isModuleWithWildcard() && otherModuleRootPath != null) {
                String relativePath = otherModuleRootPath.replace("\\", "/");
                if (FileUtils.pathMatches(module.getRulesRootPath(), relativePath)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Registers a new module in the descriptor while preserving the modules auto-discovered when rules.xml declares
     * none.
     *
     * <p>Adds nothing when the file is already matched by a wildcard module, so it stays auto-discovered and the
     * declared module list is left empty.
     *
     * <p>Otherwise appends the module. When rules.xml declares no modules, the implicit default modules are
     * materialized first so adding one explicit module does not hide them.
     */
    public void registerModule(ProjectDescriptor descriptor, Module module) {
        if (isCoveredByWildcardModule(descriptor, module)) {
            return;
        }
        if (descriptor.getModules().isEmpty()) {
            descriptor.getModules().addAll(ProjectDescriptor.defaultModules());
        }
        descriptor.getModules().add(module);
    }

    private static List<Module> effectiveModules(ProjectDescriptor descriptor) {
        var modules = descriptor.getModules();
        return modules.isEmpty() ? ProjectDescriptor.defaultModules() : modules;
    }

}
