package org.openl.source.impl;

import java.io.File;

import org.openl.types.IModuleInfo;

public class ModuleFileSourceCodeModule extends FileSourceCodeModule implements IModuleInfo {
    private final String moduleName;

    public ModuleFileSourceCodeModule(File file, String moduleName) {
        super(file, null);
        this.moduleName = moduleName;
    }

    @Override
    public String getModuleName() {
        return moduleName;
    }
}
