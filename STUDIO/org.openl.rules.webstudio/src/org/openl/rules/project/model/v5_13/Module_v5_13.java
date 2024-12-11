package org.openl.rules.project.model.v5_13;

import static org.openl.rules.project.xml.BaseProjectDescriptorSerializer.METHOD_FILTER_TAG;
import static org.openl.rules.project.xml.BaseProjectDescriptorSerializer.MODULE_TAG;
import static org.openl.rules.project.xml.BaseProjectDescriptorSerializer.RULES_ROOT_TAG;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.openl.rules.project.model.MethodFilter;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.xml.BaseProjectDescriptorSerializer;
import org.openl.rules.project.xml.v5_13.ModuleTypeXmlAdapter_v5_13;

@XmlRootElement(name = MODULE_TAG)
@XmlAccessorType(XmlAccessType.FIELD)
public class Module_v5_13 {
    @XmlJavaTypeAdapter(BaseProjectDescriptorSerializer.CollapsedStringAdapter2.class)
    private String name;
    @XmlJavaTypeAdapter(ModuleTypeXmlAdapter_v5_13.class)
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
