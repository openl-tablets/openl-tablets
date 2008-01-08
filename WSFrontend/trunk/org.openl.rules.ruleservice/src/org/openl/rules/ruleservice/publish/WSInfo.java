package org.openl.rules.ruleservice.publish;

import java.io.File;

class WSInfo {
    private File project;
    private File xlsFile;
    private String className;
    private String name;
    private boolean usingEngineFactory;

    WSInfo(File project, File xlsFile, String className, String name, boolean usingEngineFactory) {
        this.project = project;
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
