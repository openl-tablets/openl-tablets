package org.openl.rules.project.xml;

import java.io.File;
import java.io.IOException;

import org.openl.rules.project.IRulesDeploySerializer;
import org.openl.rules.project.xml.v5_11.XmlRulesDescriptorSerializer_v5_11;
import org.openl.rules.project.xml.v5_14.XmlRulesDescriptorSerializer_v5_14;
import org.openl.rules.project.xml.v5_15.XmlRulesDescriptorSerializer_v5_15;
import org.openl.rules.project.xml.v5_16.XmlRulesDescriptorSerializer_v5_16;
import org.openl.rules.project.xml.v5_17.XmlRulesDescriptorSerializer_v5_17;
import org.openl.rules.project.xml.v5_23.XmlRulesDescriptorSerializer_v5_23;
import org.springframework.core.env.PropertyResolver;

public class RulesDeploySerializerFactory {
    private final SupportedVersionSerializer supportedVersionSerializer;

    public RulesDeploySerializerFactory(PropertyResolver propertyResolver) {
        String defaultCompatibility = propertyResolver.getProperty("default.openl.compatibility.version");
        this.supportedVersionSerializer = new SupportedVersionSerializer(defaultCompatibility);
    }

    public SupportedVersion getSupportedVersion(File projectFolder) {
        return supportedVersionSerializer.getSupportedVersion(projectFolder);
    }

    public void setSupportedVersion(File projectFolder, SupportedVersion version) throws IOException {
        supportedVersionSerializer.setSupportedVersion(projectFolder, version);
    }

    public IRulesDeploySerializer getSerializer(SupportedVersion version) {
        switch (version) {
            case V5_11:
            case V5_12:
            case V5_13:
                return new XmlRulesDescriptorSerializer_v5_11();
            case V5_14:
                return new XmlRulesDescriptorSerializer_v5_14();
            case V5_15:
                return new XmlRulesDescriptorSerializer_v5_15();
            case V5_16:
                return new XmlRulesDescriptorSerializer_v5_16();
            case V5_17:
            case V5_18:
            case V5_19:
            case V5_20:
            case V5_21:
            case V5_22:
                return new XmlRulesDescriptorSerializer_v5_17();
            case V5_23:
            default: // rules-deploy.xml is not changed in newer versions of OpenL but rules.xml could
                return new XmlRulesDescriptorSerializer_v5_23();
        }
    }
}
