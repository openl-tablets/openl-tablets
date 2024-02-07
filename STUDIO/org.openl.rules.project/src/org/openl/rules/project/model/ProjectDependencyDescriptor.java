package org.openl.rules.project.model;

import static org.openl.rules.project.xml.XmlProjectDescriptorSerializer.DEPENDENCY_TAG;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = DEPENDENCY_TAG)
@XmlAccessorType(XmlAccessType.FIELD)
public class ProjectDependencyDescriptor {
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
