package org.openl.rules.project.model;


public class Module {

    private String name;
    private ModuleType type;
    private String classname;
    private PathEntry rulesRootPath;
    private ProjectDescriptor project;

    public ProjectDescriptor getProject() {
        return project;
    }

    public void setProject(ProjectDescriptor project) {
        this.project = project;
    }

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

    public PathEntry getRulesRootPath() {
        return rulesRootPath;
    }

    public void setRulesRootPath(PathEntry rulesRootPath) {
        this.rulesRootPath = rulesRootPath;
    }

}
