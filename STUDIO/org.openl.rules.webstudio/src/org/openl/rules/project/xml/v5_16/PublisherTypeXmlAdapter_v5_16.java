package org.openl.rules.project.xml.v5_16;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import org.openl.rules.project.model.v5_16.RulesDeploy_v5_16;

public class PublisherTypeXmlAdapter_v5_16 extends XmlAdapter<String, RulesDeploy_v5_16.PublisherType> {
    @Override
    public RulesDeploy_v5_16.PublisherType unmarshal(String name) {
        return RulesDeploy_v5_16.PublisherType.valueOf(name.toUpperCase());
    }

    @Override
    public String marshal(RulesDeploy_v5_16.PublisherType publisherType) {
        return publisherType.toString();
    }
}
