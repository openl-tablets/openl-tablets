package org.openl.rules.ruleservice.resolver;

import java.io.File;

/**
 * Holds information required to instantiate an OpenL rules as wrapper class or interface or through API.
 */
public class RulesModuleInfo {
    private File moduleFolder;
    private File xlsFile;
    private String className;
    private String name;
    private RulesServiceType serviceType;

    /**
     * Constructor. Some parameters may be <code>null</code>.
     * 
     * @param moduleFolder file where module files are stored
     * @param xlsFile excel file to use
     * @param className name of the class to expose
     * @param name name of the rules module
     * @param serviceType
     */
    public RulesModuleInfo(File moduleFolder, File xlsFile, String className, String name, RulesServiceType serviceType) {
        this.moduleFolder = moduleFolder;
        this.xlsFile = xlsFile;
        this.className = className;
        this.name = name;
        this.serviceType = serviceType;
    }

    public String getClassName() {
        return className;
    }

    public String getName() {
        return name;
    }

    public File getXlsFile() {
        return xlsFile;
    }

    public RulesServiceType getServiceType() {
        return serviceType;
    }

    public File getModuleFolder() {
        return moduleFolder;
    }
}