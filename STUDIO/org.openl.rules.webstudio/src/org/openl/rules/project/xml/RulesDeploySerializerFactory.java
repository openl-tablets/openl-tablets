package org.openl.rules.project.xml;

import java.io.File;
import java.io.IOException;
import javax.xml.bind.JAXBException;

import org.openl.rules.project.IRulesDeploySerializer;
import org.openl.rules.project.model.v5_11.RulesDeploy_v5_11;
import org.openl.rules.project.model.v5_11.converter.RulesDeployVersionConverter_v5_11;
import org.openl.rules.project.model.v5_14.RulesDeploy_v5_14;
import org.openl.rules.project.model.v5_14.converter.RulesDeployVersionConverter_v5_14;
import org.openl.rules.project.model.v5_15.RulesDeploy_v5_15;
import org.openl.rules.project.model.v5_15.converter.RulesDeployVersionConverter_v5_15;
import org.openl.rules.project.model.v5_16.RulesDeploy_v5_16;
import org.openl.rules.project.model.v5_16.converter.RulesDeployVersionConverter_v5_16;
import org.openl.rules.project.model.v5_17.RulesDeploy_v5_17;
import org.openl.rules.project.model.v5_17.converter.RulesDeployVersionConverter_v5_17;
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
            case V5_11:
            case V5_12:
            case V5_13:
                return new BaseRulesDeploySerializer<>(new RulesDeployVersionConverter_v5_11(), RulesDeploy_v5_11.class);
            case V5_14:
                return new BaseRulesDeploySerializer<>(new RulesDeployVersionConverter_v5_14(), RulesDeploy_v5_14.class);
            case V5_15:
                return new BaseRulesDeploySerializer<>(new RulesDeployVersionConverter_v5_15(), RulesDeploy_v5_15.class);
            case V5_16:
                return new BaseRulesDeploySerializer<>(new RulesDeployVersionConverter_v5_16(), RulesDeploy_v5_16.class);
            case V5_17:
            case V5_18:
            case V5_19:
            case V5_20:
            case V5_21:
            case V5_22:
                return new BaseRulesDeploySerializer<>(new RulesDeployVersionConverter_v5_17(), RulesDeploy_v5_17.class);
            case V5_23:
            default: // rules-deploy.xml is not changed in newer versions of OpenL but rules.xml could
                return new BaseRulesDeploySerializer<>(new RulesDeployVersionConverter_v5_23(), RulesDeploy_v5_23.class);
        }
    }
}
