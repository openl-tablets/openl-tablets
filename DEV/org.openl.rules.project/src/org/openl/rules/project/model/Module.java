package org.openl.rules.project.model;

import static org.openl.rules.project.model.WebstudioConfiguration.WEBSTUDIO_CONFIGURATION;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import lombok.Getter;
import lombok.Setter;

import org.openl.util.StringUtils;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "module")
@Getter
@Setter
public class Module {
    @XmlJavaTypeAdapter(CollapsedStringAdapter2.class)
    private String name;
    @XmlElement(name = "rules-root")
    @XmlJavaTypeAdapter(RulesRootPathAdapter.class)
    private String rulesRootPath;
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

    @SuppressWarnings("unused")
    private void beforeMarshal(Marshaller marshaller) {
        name = StringUtils.trimToNull(name);
        rulesRootPath = StringUtils.trimToNull(rulesRootPath);
        if (MethodFilter.isEmpty(methodFilter)) {
            methodFilter = null;
        }
    }

    public Path getRulesPath() {
        return project.getProjectFolder().resolve(rulesRootPath).toAbsolutePath();
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
        return rulesRootPath != null && (rulesRootPath.contains("*") || rulesRootPath.contains("?"));
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    static class RulesRootPathXml {
        @XmlAttribute
        String path;
    }

    static class RulesRootPathAdapter extends XmlAdapter<RulesRootPathXml, String> {
        @Override
        public String unmarshal(RulesRootPathXml v) {
            return v == null ? null : v.path;
        }

        @Override
        public RulesRootPathXml marshal(String v) {
            if (v == null) {
                return null;
            }
            var xml = new RulesRootPathXml();
            xml.path = v;
            return xml;
        }
    }

}
