package org.openl.rules.project.xml.v5_17;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.openl.rules.project.model.v5_17.PublisherType_v5_17;

public class PublisherTypeXmlAdapter_v5_17 extends XmlAdapter<String, PublisherType_v5_17> {
    @Override
    public PublisherType_v5_17 unmarshal(String name) {
        return PublisherType_v5_17.valueOf(name.toUpperCase());
    }

    @Override
    public String marshal(PublisherType_v5_17 publisherType) {
        return publisherType.toString();
    }
}
