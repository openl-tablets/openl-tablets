package org.openl.rules.project.xml.v5_14;

import org.openl.rules.project.model.v5_14.PublisherType_v5_14;
import org.openl.rules.project.model.v5_14.RulesDeploy_v5_14;
import org.openl.rules.project.model.v5_14.converter.RulesDeployVersionConverter;
import org.openl.rules.project.xml.BaseRulesDeploySerializer;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class XmlRulesDescriptorSerializer_v5_14 extends BaseRulesDeploySerializer<RulesDeploy_v5_14> {

    public XmlRulesDescriptorSerializer_v5_14() {
        super(new RulesDeployVersionConverter(), RulesDeploy_v5_14.class);
    }

    public static class PublisherType_v5_14XmlAdapter extends XmlAdapter<String, PublisherType_v5_14> {
        @Override
        public PublisherType_v5_14 unmarshal(String name) {
            return PublisherType_v5_14.valueOf(name.toUpperCase());
        }

        @Override
        public String marshal(PublisherType_v5_14 publisherType) {
            return publisherType.toString();
        }
    }
}
