package org.openl.rules.project.instantiation;

import lombok.Getter;

import org.openl.rules.project.model.Module;
import org.openl.source.impl.PathSourceCodeModule;
import org.openl.types.IModuleInfo;

// A source is equal by its file path; the module name and relative URI come from the module that owns the path.
@SuppressWarnings("java:S2160")
class ModulePathSourceCodeModule extends PathSourceCodeModule implements IModuleInfo {

    @Getter
    private final String moduleName;
    private final String relativeUri;

    ModulePathSourceCodeModule(Module module) {
        super(module.getRulesPath());
        this.moduleName = module.getName();
        this.relativeUri = module.getRelativeUri();
    }

    @Override
    public synchronized String getUri() {
        return relativeUri;
    }

    @Override
    public String getFileUri() {
        return super.getUri();
    }
}
