package org.openl.rules.project.xml.v5_17;

import org.openl.rules.project.model.v5_17.PublisherType_v5_17;
import org.openl.rules.project.model.v5_17.RulesDeploy_v5_17;
import org.openl.rules.project.model.v5_17.converter.RulesDeployVersionConverter;
import org.openl.rules.project.xml.BaseRulesDeploySerializer;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class XmlRulesDescriptorSerializer_v5_17 extends BaseRulesDeploySerializer<RulesDeploy_v5_17> {

    public XmlRulesDescriptorSerializer_v5_17() {
        super(new RulesDeployVersionConverter(), RulesDeploy_v5_17.class);
    }

    public static class PublisherType_v5_17XmlAdapter extends XmlAdapter<String, PublisherType_v5_17> {
        @Override
        public PublisherType_v5_17 unmarshal(String name) {
            return PublisherType_v5_17.valueOf(name.toUpperCase());
        }

        @Override
        public String marshal(PublisherType_v5_17 publisherType) {
            return publisherType.toString();
        }
    }
}
