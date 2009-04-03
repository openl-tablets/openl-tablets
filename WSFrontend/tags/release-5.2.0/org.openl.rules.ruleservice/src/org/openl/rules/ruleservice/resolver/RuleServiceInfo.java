package org.openl.rules.ruleservice.resolver;

import java.io.File;

/**
 * Holds information required to expose a an OpenL wrapper class or interface as a web service. 
 */
public class RuleServiceInfo {
    private File project;
    private File projectBin;
    private File xlsFile;
    private String className;
    private String name;
    private boolean usingEngineFactory;

    /**
     * Constructor. Some parameters may be <code>null</code>.
     *
     * @param project the directory where an OpenL project was downloaded to
     * @param xlsFile excel file to use
     * @param className name of the class to expose
     * @param name name of the webservice
     * @param usingEngineFactory if use <code>EngineFactory</code> to create engine instance
     */
    public RuleServiceInfo(File project, File projectBin, File xlsFile, String className, String name, boolean usingEngineFactory) {
        this.project = project;
        this.projectBin = projectBin;
        this.xlsFile = xlsFile;
        this.className = className;
        this.name = name;
        this.usingEngineFactory = usingEngineFactory;
    }
    
    public String getName() {
        return name;
    }

    public File getProject() {
        return project;
    }
    
    public File getProjectBin() {
        return projectBin;
    }

    public String getClassName() {
        return className;
    }

    public boolean isUsingEngineFactory() {
        return usingEngineFactory;
    }

    public File getXlsFile() {
        return xlsFile;
    }
}
