package org.openl.rules.project.model.v5_16;

import java.util.List;

public class Extension_v5_16 {
    private String name;
    private String extensionPackage;
    private List<String> dependencies;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * A package where a custom OpenLBuilder class is located. Can be null (in this case package by convention will be
     * "org.openl.extension." + lowerCase(name))
     *
     * @return extension package
     */
    public String getExtensionPackage() {
        return extensionPackage;
    }

    public void setExtensionPackage(String extensionPackage) {
        this.extensionPackage = extensionPackage;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }
}
