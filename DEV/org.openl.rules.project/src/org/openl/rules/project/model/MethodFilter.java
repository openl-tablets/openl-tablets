package org.openl.rules.project.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import lombok.Getter;
import lombok.Setter;

import org.openl.util.StringUtils;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@Getter
@Setter
public class MethodFilter {
    @XmlElementWrapper(name = "includes")
    @XmlElement(name = "value")
    @XmlJavaTypeAdapter(CollapsedStringAdapter2.class)
    private Set<String> includes;
    @XmlElementWrapper(name = "excludes")
    @XmlElement(name = "value")
    @XmlJavaTypeAdapter(CollapsedStringAdapter2.class)
    private Set<String> excludes;

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

    /** Drop wrapper elements (and the surrounding {@code <method-filter/>} if both sets become empty). */
    static boolean isEmpty(MethodFilter mf) {
        return mf == null || (isBlank(mf.includes) && isBlank(mf.excludes));
    }

    private static boolean isBlank(Set<String> values) {
        return values == null || values.stream().allMatch(StringUtils::isBlank);
    }

    @SuppressWarnings("unused")
    private void beforeMarshal(Marshaller marshaller) {
        if (isBlank(includes)) {
            includes = null;
        }
        if (isBlank(excludes)) {
            excludes = null;
        }
    }
}
