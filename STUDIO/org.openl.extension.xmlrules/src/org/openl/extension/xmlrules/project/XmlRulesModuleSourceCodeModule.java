package org.openl.extension.xmlrules.project;

import java.io.File;

import org.openl.source.impl.ModuleFileSourceCodeModule;

public class XmlRulesModuleSourceCodeModule extends ModuleFileSourceCodeModule {
    private final XmlRulesModule module;

    public XmlRulesModuleSourceCodeModule(File file, XmlRulesModule module) {
        super(file, module.getName());
        this.module = module;
    }

    public String getInternalModulePath() {
        return module.getInternalModulePath();
    }

    public XmlRulesModule getModule() {
        return module;
    }
}
