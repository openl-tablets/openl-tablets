package org.openl.source.impl;

import java.nio.file.Path;

import org.openl.types.IModuleInfo;

public class ModulePathSourceCodeModule extends PathSourceCodeModule implements IModuleInfo {

    private final String moduleName;

    public ModulePathSourceCodeModule(Path path, String moduleName) {
        super(path);
        this.moduleName = moduleName;
    }

    @Override
    public String getModuleName() {
        return moduleName;
    }
}
