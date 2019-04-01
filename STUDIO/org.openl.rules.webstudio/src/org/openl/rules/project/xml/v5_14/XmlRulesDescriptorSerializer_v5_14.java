package org.openl.rules.project.xml.v5_14;

import org.openl.rules.project.model.v5_14.RulesDeploy_v5_14;
import org.openl.rules.project.model.v5_14.converter.RulesDeployVersionConverter;
import org.openl.rules.project.xml.BaseRulesDeploySerializer;

import com.thoughtworks.xstream.XStream;

public class XmlRulesDescriptorSerializer_v5_14 extends BaseRulesDeploySerializer<RulesDeploy_v5_14> {
    private static final String RULES_DEPLOY_DESCRIPTOR_TAG = "rules-deploy";

    public XmlRulesDescriptorSerializer_v5_14() {
        super(new RulesDeployVersionConverter());
        xstream.ignoreUnknownElements();
        xstream.omitField(RulesDeploy_v5_14.class, "log");

        xstream.setMode(XStream.NO_REFERENCES);

        xstream.aliasType("publisher", RulesDeploy_v5_14.PublisherType.class);
        xstream.aliasType(RULES_DEPLOY_DESCRIPTOR_TAG, RulesDeploy_v5_14.class);
    }
}
