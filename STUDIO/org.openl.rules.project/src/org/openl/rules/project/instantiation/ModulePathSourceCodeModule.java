package org.openl.rules.project.instantiation;

import java.nio.file.Path;
import java.util.Optional;

import org.openl.rules.project.model.Module;
import org.openl.source.impl.PathSourceCodeModule;
import org.openl.types.IModuleInfo;

class ModulePathSourceCodeModule extends PathSourceCodeModule implements IModuleInfo {

    private final String moduleName;
    private final String relativeUri;

    ModulePathSourceCodeModule(Module module) {
        super(module.getRulesPath());
        this.moduleName = module.getName();
        Path projFolder = module.getProject().getProjectFolder();
        this.relativeUri = Optional.ofNullable(projFolder.getParent())
            .orElse(projFolder)
            .toUri()
            .relativize(module.getRulesPath().toUri())
            .toString();
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
