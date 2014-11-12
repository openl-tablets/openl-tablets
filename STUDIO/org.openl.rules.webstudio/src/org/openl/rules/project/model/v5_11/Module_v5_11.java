package org.openl.rules.project.model.v5_11;

import org.openl.rules.project.model.MethodFilter;
import org.openl.rules.project.model.PathEntry;

public class Module_v5_11 {

    private String name;
    private ModuleType_v5_11 type;
    private String classname;
    private PathEntry rulesRootPath;
    private MethodFilter methodFilter;

    public MethodFilter getMethodFilter() {
        return methodFilter;
    }

    public void setMethodFilter(MethodFilter methodFilter) {
        this.methodFilter = methodFilter;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ModuleType_v5_11 getType() {
        return type;
    }

    public void setType(ModuleType_v5_11 type) {
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

    @Override
    public String toString() {
        return name;
    }

}
