package org.openl.rules.project.model.v5_16.converter;

import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ObjectVersionConverter;
import org.openl.rules.project.model.v5_16.Module_v5_16;

public class ModuleVersionConverter implements ObjectVersionConverter<Module, Module_v5_16> {

    @Override
    public Module fromOldVersion(Module_v5_16 oldVersion) {
        Module module = new Module();

        module.setName(oldVersion.getName());
        module.setRulesRootPath(oldVersion.getRulesRootPath());
        module.setMethodFilter(oldVersion.getMethodFilter());

        return module;
    }

    @Override
    public Module_v5_16 toOldVersion(Module currentVersion) {
        Module_v5_16 module = new Module_v5_16();

        module.setName(currentVersion.getName());
        module.setRulesRootPath(currentVersion.getRulesRootPath());
        module.setMethodFilter(currentVersion.getMethodFilter());

        return module;
    }
}
