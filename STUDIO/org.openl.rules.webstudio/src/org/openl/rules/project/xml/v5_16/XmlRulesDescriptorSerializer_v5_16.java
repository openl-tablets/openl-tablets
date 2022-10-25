package org.openl.rules.project.xml.v5_16;

import org.openl.rules.project.model.v5_16.RulesDeploy_v5_16;
import org.openl.rules.project.model.v5_16.converter.RulesDeployVersionConverter;
import org.openl.rules.project.xml.BaseRulesDeploySerializer;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.adapters.XmlAdapter;

public class XmlRulesDescriptorSerializer_v5_16 extends BaseRulesDeploySerializer<RulesDeploy_v5_16> {

    public XmlRulesDescriptorSerializer_v5_16() throws JAXBException {
        super(new RulesDeployVersionConverter(), RulesDeploy_v5_16.class);
    }

    public static class PublisherTypeXmlAdapter extends XmlAdapter<String, RulesDeploy_v5_16.PublisherType> {
        @Override
        public RulesDeploy_v5_16.PublisherType unmarshal(String name) {
            return RulesDeploy_v5_16.PublisherType.valueOf(name.toUpperCase());
        }

        @Override
        public String marshal(RulesDeploy_v5_16.PublisherType publisherType) {
            return publisherType.toString();
        }
    }
}
