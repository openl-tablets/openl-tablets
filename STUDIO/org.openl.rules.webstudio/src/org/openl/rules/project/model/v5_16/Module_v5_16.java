package org.openl.rules.project.model.v5_16;

import org.openl.rules.project.model.MethodFilter;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.v5_13.ModuleType_v5_13;

public class Module_v5_16 {

    private String name;
    private ModuleType_v5_13 type;
    private String classname;
    private PathEntry rulesRootPath;
    private MethodFilter methodFilter;
    private Extension_v5_16 extension;

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

    public ModuleType_v5_13 getType() {
        return type;
    }

    public void setType(ModuleType_v5_13 type) {
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

    public Extension_v5_16 getExtension() {
        return extension;
    }

    public void setExtension(Extension_v5_16 extension) {
        this.extension = extension;
    }

    @Override
    public String toString() {
        return name;
    }

}
