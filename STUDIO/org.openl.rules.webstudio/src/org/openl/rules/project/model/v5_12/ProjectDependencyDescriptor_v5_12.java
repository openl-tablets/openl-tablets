package org.openl.rules.project.model.v5_12;

import static org.openl.rules.project.xml.XmlProjectDescriptorSerializer.DEPENDENCY_TAG;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.openl.rules.project.xml.BaseProjectDescriptorSerializer;

@XmlRootElement(name = DEPENDENCY_TAG)
@XmlAccessorType(XmlAccessType.FIELD)
public class ProjectDependencyDescriptor_v5_12 {
    @XmlJavaTypeAdapter(BaseProjectDescriptorSerializer.CollapsedStringAdapter2.class)
    private String name;
    private boolean autoIncluded;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isAutoIncluded() {
        return autoIncluded;
    }

    public void setAutoIncluded(boolean autoIncluded) {
        this.autoIncluded = autoIncluded;
    }
}
