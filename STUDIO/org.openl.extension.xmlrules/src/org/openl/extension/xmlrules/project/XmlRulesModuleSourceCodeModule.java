package org.openl.extension.xmlrules.project;

import java.net.URL;

import org.openl.source.impl.ModuleFileSourceCodeModule;

public class XmlRulesModuleSourceCodeModule extends ModuleFileSourceCodeModule {
    private final XmlRulesModule module;

    public XmlRulesModuleSourceCodeModule(URL url, XmlRulesModule module) {
        super(url, module.getName());
        this.module = module;
    }

    public String getInternalModulePath() {
        return module.getInternalModulePath();
    }

    public XmlRulesModule getModule() {
        return module;
    }
}
