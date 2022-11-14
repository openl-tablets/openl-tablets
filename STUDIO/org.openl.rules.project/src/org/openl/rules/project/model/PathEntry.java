package org.openl.rules.project.model;

import org.openl.rules.project.xml.XmlProjectDescriptorSerializer;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

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
