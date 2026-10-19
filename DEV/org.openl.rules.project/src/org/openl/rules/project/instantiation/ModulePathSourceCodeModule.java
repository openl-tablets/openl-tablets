package org.openl.rules.project.instantiation;

import java.nio.file.Path;

import lombok.Getter;

import org.openl.rules.project.model.Module;
import org.openl.source.IProjectSourceCodeModule;
import org.openl.source.impl.PathSourceCodeModule;
import org.openl.types.IModuleInfo;

// A source is equal by its file path; the module name and relative URI come from the module that owns the path.
@SuppressWarnings("java:S2160")
class ModulePathSourceCodeModule extends PathSourceCodeModule implements IModuleInfo, IProjectSourceCodeModule {

    @Getter
    private final String moduleName;
    private final String relativeUri;
    @Getter
    private final Path projectFolder;

    ModulePathSourceCodeModule(Module module) {
        super(module.getRulesPath());
        this.moduleName = module.getName();
        this.relativeUri = module.getRelativeUri();
        this.projectFolder = module.getProject().getProjectFolder();
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
