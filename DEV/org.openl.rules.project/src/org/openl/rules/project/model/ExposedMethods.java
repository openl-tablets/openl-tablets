package org.openl.rules.project.model;

import java.util.Set;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import lombok.Getter;
import lombok.Setter;

import org.openl.util.StringUtils;

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

    /** Used by parent containers to decide whether the {@code <exposed-methods>} wrapper can be dropped. */
    static boolean isEmpty(ExposedMethods e) {
        return e == null || (isBlank(e.includes) && isBlank(e.excludes));
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
