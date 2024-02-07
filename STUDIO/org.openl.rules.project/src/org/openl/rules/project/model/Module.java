package org.openl.rules.project.model;

import static org.openl.rules.project.xml.XmlProjectDescriptorSerializer.METHOD_FILTER_TAG;
import static org.openl.rules.project.xml.XmlProjectDescriptorSerializer.WEBSTUDIO_CONFIGURATION;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.openl.rules.project.xml.XmlProjectDescriptorSerializer;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "module")
public class Module {
    @XmlJavaTypeAdapter(XmlProjectDescriptorSerializer.CollapsedStringAdapter2.class)
    private String name;
    @XmlElement(name = "rules-root")
    private PathEntry rulesRootPath;
    @XmlTransient
    private ProjectDescriptor project;
    @XmlTransient
    private Map<String, Object> properties;
    @XmlElement(name = WEBSTUDIO_CONFIGURATION)
    private WebstudioConfiguration webstudioConfiguration = new WebstudioConfiguration();
    @XmlTransient
    private String wildcardName;
    @XmlTransient
    private String wildcardRulesRootPath;
    @XmlElement(name = METHOD_FILTER_TAG)
    private MethodFilter methodFilter;

    public MethodFilter getMethodFilter() {
        return methodFilter;
    }

    public void setMethodFilter(MethodFilter methodFilter) {
        this.methodFilter = methodFilter;
    }

    public String getWildcardRulesRootPath() {
        return wildcardRulesRootPath;
    }

    public void setWildcardRulesRootPath(String wildcardRulesRootPath) {
        this.wildcardRulesRootPath = wildcardRulesRootPath;
    }

    public WebstudioConfiguration getWebstudioConfiguration() {
        return webstudioConfiguration;
    }

    public void setWebstudioConfiguration(WebstudioConfiguration webstudioConfiguration) {
        this.webstudioConfiguration = webstudioConfiguration;
    }

    public String getWildcardName() {
        return wildcardName;
    }

    public void setWildcardName(String wildcardName) {
        this.wildcardName = wildcardName;
    }

    public ProjectDescriptor getProject() {
        return project;
    }

    public void setProject(ProjectDescriptor project) {
        this.project = project;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PathEntry getRulesRootPath() {
        return rulesRootPath;
    }

    public Path getRulesPath() {
        return project.getProjectFolder().resolve(rulesRootPath.getPath()).toAbsolutePath();
    }

    public void setRulesRootPath(PathEntry rulesRootPath) {
        this.rulesRootPath = rulesRootPath;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public String getRelativeUri() {
        return Optional.ofNullable(project.getProjectFolder().getParent())
                .orElse(project.getProjectFolder())
                .toUri()
                .relativize(getRulesPath().toUri())
                .toString();
    }

    @Override
    public String toString() {
        return name;
    }

}
