package org.openl.rules.project.model.v5_11.converter;

import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ObjectVersionConverter;
import org.openl.rules.project.model.v5_11.Module_v5_11;

public class ModuleVersionConverter implements ObjectVersionConverter<Module, Module_v5_11> {
    private final ModuleTypeVersionConverter moduleTypeVersionConverter = new ModuleTypeVersionConverter();

    @Override
    public Module fromOldVersion(Module_v5_11 oldVersion) {
        Module module = new Module();

        module.setName(oldVersion.getName());
        module.setType(moduleTypeVersionConverter.fromOldVersion(oldVersion.getType()));
        module.setClassname(oldVersion.getClassname());
        module.setRulesRootPath(oldVersion.getRulesRootPath());
        module.setMethodFilter(oldVersion.getMethodFilter());

        return module;
    }

    @Override
    public Module_v5_11 toOldVersion(Module currentVersion) {
        Module_v5_11 module = new Module_v5_11();

        module.setName(currentVersion.getName());
        module.setType(moduleTypeVersionConverter.toOldVersion(currentVersion.getType()));
        module.setClassname(currentVersion.getClassname());
        module.setRulesRootPath(currentVersion.getRulesRootPath());
        module.setMethodFilter(currentVersion.getMethodFilter());

        return module;
    }
}
