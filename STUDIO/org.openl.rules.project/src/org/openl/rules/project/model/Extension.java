package org.openl.rules.project.model;

public class Extension {
    private String name;
    private String extensionPackage;

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
}
