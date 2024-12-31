package org.openl.rules.project.xml;

import java.io.File;
import java.io.IOException;
import javax.xml.bind.JAXBException;

import org.openl.rules.project.IRulesDeploySerializer;
import org.openl.rules.project.model.v5_23.RulesDeploy_v5_23;
import org.openl.rules.project.model.v5_23.converter.RulesDeployVersionConverter_v5_23;

public class RulesDeploySerializerFactory {
    private final SupportedVersionSerializer supportedVersionSerializer;

    public RulesDeploySerializerFactory(String defaultVersion) {
        this.supportedVersionSerializer = new SupportedVersionSerializer(defaultVersion);
    }

    public SupportedVersion getSupportedVersion(File projectFolder) {
        return supportedVersionSerializer.getSupportedVersion(projectFolder);
    }

    public void setSupportedVersion(File projectFolder, SupportedVersion version) throws IOException {
        supportedVersionSerializer.setSupportedVersion(projectFolder, version);
    }

    public IRulesDeploySerializer getSerializer(SupportedVersion version) throws JAXBException {
        switch (version) {
            case V5_23:
            case V5_24:
            case V5_25:
            case V5_26:
            case V5_27:
            default: // rules-deploy.xml is not changed in newer versions of OpenL but rules.xml could
                return new BaseRulesDeploySerializer<>(new RulesDeployVersionConverter_v5_23(), RulesDeploy_v5_23.class);
        }
    }
}
