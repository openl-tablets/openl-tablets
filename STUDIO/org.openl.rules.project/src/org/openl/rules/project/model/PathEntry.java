package org.openl.rules.project.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.openl.rules.project.xml.XmlProjectDescriptorSerializer;

@XmlRootElement(name = "rules-root")
@XmlAccessorType(XmlAccessType.FIELD)
public class PathEntry {

    @XmlAttribute
    @XmlJavaTypeAdapter(XmlProjectDescriptorSerializer.CollapsedStringAdapter2.class)
    private String path;

    public PathEntry() {
    }

    public PathEntry(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
