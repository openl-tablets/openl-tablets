package org.openl.rules.project.xml.v5_11;

import org.openl.rules.project.model.v5_11.RulesDeploy_v5_11;
import org.openl.rules.project.model.v5_11.converter.RulesDeployVersionConverter;
import org.openl.rules.project.xml.BaseRulesDeploySerializer;

import com.thoughtworks.xstream.XStream;

public class XmlRulesDescriptorSerializer_v5_11 extends BaseRulesDeploySerializer<RulesDeploy_v5_11> {
    private final static String RULES_DEPLOY_DESCRIPTOR_TAG = "rules-deploy";

    public XmlRulesDescriptorSerializer_v5_11() {
        super(new RulesDeployVersionConverter());
        xstream.ignoreUnknownElements();
        xstream.omitField(RulesDeploy_v5_11.class, "log");

        xstream.setMode(XStream.NO_REFERENCES);

        xstream.aliasType(RULES_DEPLOY_DESCRIPTOR_TAG, RulesDeploy_v5_11.class);
    }
}
