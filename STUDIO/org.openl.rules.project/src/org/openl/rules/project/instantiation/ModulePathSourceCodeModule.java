package org.openl.rules.project.instantiation;

import java.nio.file.Path;

import org.openl.source.impl.PathSourceCodeModule;
import org.openl.types.IModuleInfo;

class ModulePathSourceCodeModule extends PathSourceCodeModule implements IModuleInfo {

    private final String moduleName;

    ModulePathSourceCodeModule(Path path, String moduleName) {
        super(path);
        this.moduleName = moduleName;
    }

    @Override
    public String getModuleName() {
        return moduleName;
    }
}
