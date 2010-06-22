package org.openl.rules.project.model;

import java.util.List;

public class Module {

    private String name;
    private ModuleType type;
    private String classname;
    private String rulesRootPath;
    private List<ClasspathEntry> classpath;
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public ModuleType getType() {
        return type;
    }
    public void setType(ModuleType type) {
        this.type = type;
    }
    public String getClassname() {
        return classname;
    }
    public void setClassname(String classname) {
        this.classname = classname;
    }
    public String getRulesRootPath() {
        return rulesRootPath;
    }
    public void setRulesRootPath(String rulesRootPath) {
        this.rulesRootPath = rulesRootPath;
    }
    public List<ClasspathEntry> getClasspath() {
        return classpath;
    }
    public void setClasspath(List<ClasspathEntry> classpath) {
        this.classpath = classpath;
    }
    
}
