package org.openl.rules.project.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import lombok.Getter;
import lombok.Setter;

@XmlRootElement(name = "rules-root")
@XmlAccessorType(XmlAccessType.FIELD)
@Getter
@Setter
public class PathEntry {

    @XmlAttribute
    @XmlJavaTypeAdapter(CollapsedStringAdapter2.class)
    private String path;

    public PathEntry() {
    }

    public PathEntry(String path) {
        this.path = path;
    }
}
