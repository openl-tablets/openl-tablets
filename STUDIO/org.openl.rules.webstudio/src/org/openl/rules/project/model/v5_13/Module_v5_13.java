package org.openl.rules.project.model.v5_13;

import org.openl.rules.project.model.MethodFilter;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.xml.BaseProjectDescriptorSerializer;
import org.openl.rules.project.xml.v5_13.XmlProjectDescriptorSerializer_v5_13;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import static org.openl.rules.project.xml.BaseProjectDescriptorSerializer.METHOD_FILTER_TAG;
import static org.openl.rules.project.xml.BaseProjectDescriptorSerializer.MODULE_TAG;
import static org.openl.rules.project.xml.BaseProjectDescriptorSerializer.RULES_ROOT_TAG;

@XmlRootElement(name = MODULE_TAG)
@XmlAccessorType(XmlAccessType.FIELD)
public class Module_v5_13 {
    @XmlJavaTypeAdapter(BaseProjectDescriptorSerializer.CollapsedStringAdapter2.class)
    private String name;
    @XmlJavaTypeAdapter(XmlProjectDescriptorSerializer_v5_13.ModuleType_v5_13XmlAdapter.class)
    private ModuleType_v5_13 type;
    @XmlJavaTypeAdapter(BaseProjectDescriptorSerializer.CollapsedStringAdapter2.class)
    private String classname;
    @XmlElement(name = RULES_ROOT_TAG)
    private PathEntry rulesRootPath;
    @XmlElement(name = METHOD_FILTER_TAG)
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

    @Override
    public String toString() {
        return name;
    }

}
