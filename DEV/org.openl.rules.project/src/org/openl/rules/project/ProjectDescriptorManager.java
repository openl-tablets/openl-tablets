package org.openl.rules.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
        final var otherModuleRootPath = otherModule.getRulesRootPath();
        for (Module module : effectiveModules(descriptor)) {
            if (module.isModuleWithWildcard() && otherModuleRootPath != null) {
                var relativePath = otherModuleRootPath.replace("\\", "/");
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
     * <p>Adds nothing when the descriptor already leads to that module: it declares one for the same path, or a
     * wildcard matches the file and names the module the way it is asked for. A wildcard names a module after its
     * workbook, so a module asked for under any other name is declared, or that name would be lost; a wildcard
     * covered by a wider one contributes nothing of its own and is left out.
     *
     * <p>Otherwise appends the module. When rules.xml declares no modules, the implicit default modules are
     * materialized first so adding one explicit module does not hide them.
     */
    public void registerModule(ProjectDescriptor descriptor, Module module) {
        if (isAlreadyRegistered(descriptor, module)) {
            return;
        }
        declareModule(descriptor, module);
    }

    /**
     * Whether the descriptor already leads to that module, so registering it would write nothing.
     *
     * <p>A caller that writes rules.xml itself asks this first: an untouched descriptor is not worth saving.
     */
    public boolean isAlreadyRegistered(ProjectDescriptor descriptor, Module module) {
        return isDeclared(descriptor, module) || isNamedByWildcard(descriptor, module);
    }

    /** Whether the descriptor already declares a module for that path, whatever it calls it. */
    private static boolean isDeclared(ProjectDescriptor descriptor, Module module) {
        return descriptor.getModules()
                .stream()
                .anyMatch(declared -> Objects.equals(declared.getRulesRootPath(), module.getRulesRootPath()));
    }

    /** Whether a wildcard already contributes that module, under the name it is asked for. */
    private boolean isNamedByWildcard(ProjectDescriptor descriptor, Module module) {
        if (!isCoveredByWildcardModule(descriptor, module)) {
            return false;
        }
        return module.isModuleWithWildcard()
                || FileUtils.getBaseName(module.getRulesRootPath()).equals(module.getName());
    }

    /**
     * Declares the module explicitly, whether or not a wildcard already matches its file.
     *
     * <p>When rules.xml declares no modules, the implicit default modules are materialized first so adding one
     * explicit module does not hide them.
     */
    public void declareModule(ProjectDescriptor descriptor, Module module) {
        // Built as its own list: a descriptor may hold one that cannot be added to - the implicit defaults are
        // immutable, and so is any list a caller assembled with List.of.
        var modules = new ArrayList<>(descriptor.getModules());
        if (modules.isEmpty()) {
            modules.addAll(ProjectDescriptor.defaultModules());
        }
        modules.add(module);
        descriptor.setModules(modules);
    }

    private static List<Module> effectiveModules(ProjectDescriptor descriptor) {
        var modules = descriptor.getModules();
        return modules.isEmpty() ? ProjectDescriptor.defaultModules() : modules;
    }

}
