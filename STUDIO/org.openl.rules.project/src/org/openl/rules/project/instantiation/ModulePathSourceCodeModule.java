package org.openl.rules.project.instantiation;

import java.nio.file.Path;

import org.openl.source.impl.PathSourceCodeModule;
import org.openl.types.IModuleInfo;

class ModulePathSourceCodeModule extends PathSourceCodeModule implements IModuleInfo {

    private final String moduleName;
    private final String relativePath;

    ModulePathSourceCodeModule(Path path, String moduleName, String relativePath) {
        super(path);
        this.moduleName = moduleName;
        this.relativePath = relativePath;
    }

    @Override
    public String getModuleName() {
        return moduleName;
    }

    @Override
    public String getUri() {
        return relativePath;
    }

    @Override
    public String getFileUri() {
        return makeUri();
    }
}
