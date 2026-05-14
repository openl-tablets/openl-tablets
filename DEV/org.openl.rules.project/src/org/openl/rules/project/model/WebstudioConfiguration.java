package org.openl.rules.project.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import lombok.Getter;
import lombok.Setter;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = WebstudioConfiguration.WEBSTUDIO_CONFIGURATION)
@Getter
@Setter
public class WebstudioConfiguration {
    static final String WEBSTUDIO_CONFIGURATION = "webstudioConfiguration";
    private boolean compileThisModuleOnly = false;

    /**
     * Marshal a {@link WebstudioConfiguration} only when it diverges from the default state — i.e. when
     * {@link #isCompileThisModuleOnly()} is {@code true}. On unmarshal a missing element materializes a
     * default instance so callers never see {@code null}.
     */
    public static class Adapter extends XmlAdapter<WebstudioConfiguration, WebstudioConfiguration> {
        @Override
        public WebstudioConfiguration unmarshal(WebstudioConfiguration v) {
            return v == null ? new WebstudioConfiguration() : v;
        }

        @Override
        public WebstudioConfiguration marshal(WebstudioConfiguration v) {
            return (v == null || !v.isCompileThisModuleOnly()) ? null : v;
        }
    }
}
