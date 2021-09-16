package org.openl.rules.project.instantiation;

import org.openl.rules.project.model.Module;
import org.openl.source.impl.PathSourceCodeModule;
import org.openl.types.IModuleInfo;

class ModulePathSourceCodeModule extends PathSourceCodeModule implements IModuleInfo {

    private final String moduleName;
    private final String relativeUri;

    ModulePathSourceCodeModule(Module module) {
        super(module.getRulesPath());
        this.moduleName = module.getName();
        this.relativeUri = module.getRelativeUri();
    }

    @Override
    public String getModuleName() {
        return moduleName;
    }

    @Override
    public String getUri() {
        return relativeUri;
    }

    @Override
    public String getFileUri() {
        return super.getUri();
    }
}
