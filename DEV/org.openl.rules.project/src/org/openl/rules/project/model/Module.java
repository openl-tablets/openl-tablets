package org.openl.rules.project.model;

import static org.openl.rules.project.model.WebstudioConfiguration.WEBSTUDIO_CONFIGURATION;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import lombok.Getter;
import lombok.Setter;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "module")
@Getter
@Setter
public class Module {
    @XmlJavaTypeAdapter(CollapsedStringAdapter2.class)
    private String name;
    @XmlElement(name = "rules-root")
    private PathEntry rulesRootPath;
    @XmlTransient
    private ProjectDescriptor project;
    @XmlTransient
    private Map<String, Object> properties;
    @XmlElement(name = WEBSTUDIO_CONFIGURATION)
    @XmlJavaTypeAdapter(WebstudioConfiguration.Adapter.class)
    private WebstudioConfiguration webstudioConfiguration = new WebstudioConfiguration();
    @XmlTransient
    private String wildcardName;
    @XmlTransient
    private String wildcardRulesRootPath;
    @XmlElement(name = "method-filter")
    private MethodFilter methodFilter;

    public Path getRulesPath() {
        return project.getProjectFolder().resolve(rulesRootPath.getPath()).toAbsolutePath();
    }

    public String getRelativeUri() {
        return Optional.ofNullable(project.getProjectFolder().getParent())
                .orElse(project.getProjectFolder())
                .toUri()
                .relativize(getRulesPath().toUri())
                .toString();
    }

    public boolean containsTable(String tableUri) {
        if (tableUri == null || getRulesRootPath() == null) {
            // Eclipse project
            return false;
        }
        return tableUri.startsWith(getRelativeUri());
    }

    @Override
    public String toString() {
        return name;
    }

    public boolean isModuleWithWildcard() {
        if (rulesRootPath != null) {
            String path = rulesRootPath.getPath();
            return path.contains("*") || path.contains("?");
        }
        return false;
    }

}
