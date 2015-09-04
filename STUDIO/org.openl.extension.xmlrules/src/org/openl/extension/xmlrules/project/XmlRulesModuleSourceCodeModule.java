package org.openl.extension.xmlrules.project;

import java.io.File;

import org.openl.source.impl.ModuleFileSourceCodeModule;

public class XmlRulesModuleSourceCodeModule extends ModuleFileSourceCodeModule {
    private final String internalModulePath;

    public XmlRulesModuleSourceCodeModule(File file, String moduleName, String internalModulePath) {
        super(file, moduleName);
        this.internalModulePath = internalModulePath;
    }

    public String getInternalModulePath() {
        return internalModulePath;
    }
}
