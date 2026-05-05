package org.openl.rules.project.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.openl.rules.project.xml.XmlProjectDescriptorSerializer;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ExposedMethods {
    @XmlElement(name = "include")
    @XmlJavaTypeAdapter(XmlProjectDescriptorSerializer.CollapsedStringAdapter2.class)
    private Set<String> includes;
    @XmlElement(name = "exclude")
    @XmlJavaTypeAdapter(XmlProjectDescriptorSerializer.CollapsedStringAdapter2.class)
    private Set<String> excludes;

    public Collection<String> getExcludes() {
        return excludes;
    }

    public void setExcludes(Set<String> excludes) {
        this.excludes = excludes;
    }

    public Collection<String> getIncludes() {
        return includes;
    }

    public void setIncludes(Set<String> includes) {
        this.includes = includes;
    }

    public void addIncludePattern(String... patterns) {
        if (patterns != null && patterns.length > 0) {
            if (includes == null) {
                includes = new HashSet<>();
            }
            Collections.addAll(includes, patterns);
        }
    }

    public void addExcludePattern(String... patterns) {
        if (patterns != null && patterns.length > 0) {
            if (excludes == null) {
                excludes = new HashSet<>();
            }
            Collections.addAll(excludes, patterns);
        }
    }
}
