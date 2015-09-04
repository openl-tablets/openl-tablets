package org.openl.extension.xmlrules.project;

import org.openl.rules.project.model.Module;

public class XmlRulesModule extends Module {
    private String internalModulePath;

    public String getInternalModulePath() {
        return internalModulePath;
    }

    public void setInternalModulePath(String internalModulePath) {
        this.internalModulePath = internalModulePath;
    }
}
