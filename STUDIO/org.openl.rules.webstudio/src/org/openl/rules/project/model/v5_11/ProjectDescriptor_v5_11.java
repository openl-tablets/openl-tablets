package org.openl.rules.project.model.v5_11;

import static org.openl.rules.project.xml.BaseProjectDescriptorSerializer.CLASSPATH_TAG;
import static org.openl.rules.project.xml.BaseProjectDescriptorSerializer.MODULES_TAG;
import static org.openl.rules.project.xml.BaseProjectDescriptorSerializer.MODULE_TAG;
import static org.openl.rules.project.xml.BaseProjectDescriptorSerializer.PATH_TAG;
import static org.openl.rules.project.xml.BaseProjectDescriptorSerializer.PROJECT_DESCRIPTOR_TAG;

import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.xml.BaseProjectDescriptorSerializer;


@XmlRootElement(name = PROJECT_DESCRIPTOR_TAG)
@XmlAccessorType(XmlAccessType.FIELD)
public class ProjectDescriptor_v5_11 {
    @XmlJavaTypeAdapter(BaseProjectDescriptorSerializer.CollapsedStringAdapter2.class)
    private String id;
    @XmlJavaTypeAdapter(BaseProjectDescriptorSerializer.CollapsedStringAdapter2.class)
    private String name;
    @XmlJavaTypeAdapter(BaseProjectDescriptorSerializer.CollapsedStringAdapter2.class)
    private String comment;
    @XmlElement(name = MODULE_TAG)
    @XmlElementWrapper(name = MODULES_TAG)
    private List<Module_v5_11> modules;
    @XmlElementWrapper(name = CLASSPATH_TAG)
    @XmlElement(name = PATH_TAG)
    private List<PathEntry> classpath;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<Module_v5_11> getModules() {
        return modules;
    }

    public void setModules(List<Module_v5_11> modules) {
        this.modules = modules;
    }

    public List<PathEntry> getClasspath() {
        return classpath;
    }

    public void setClasspath(List<PathEntry> classpath) {
        this.classpath = classpath;
    }
}
