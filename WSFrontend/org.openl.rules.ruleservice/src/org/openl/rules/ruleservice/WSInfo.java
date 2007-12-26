package org.openl.rules.ruleservice;

import java.io.File;

class WSInfo {
    private File project;
    private String className;

    WSInfo(File project, String className) {
        this.project = project;
        this.className = className;
    }

    public File getProject() {
        return project;
    }

    public String getClassName() {
        return className;
    }
}
