package org.openl.rules.project.model;

import java.util.Set;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import lombok.Getter;
import lombok.Setter;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@Getter
@Setter
public class ExposedMethods {
    @XmlElement(name = "include")
    @XmlJavaTypeAdapter(CollapsedStringAdapter2.class)
    private Set<String> includes;
    @XmlElement(name = "exclude")
    @XmlJavaTypeAdapter(CollapsedStringAdapter2.class)
    private Set<String> excludes;
}
