package org.openl.rules.project.xml.v5_23;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.openl.rules.project.model.v5_23.PublisherType_v5_23;

public class PublisherTypeXmlAdapter_v5_23 extends XmlAdapter<String, PublisherType_v5_23> {
    @Override
    public PublisherType_v5_23 unmarshal(String name) {
        return PublisherType_v5_23.valueOf(name.toUpperCase());
    }

    @Override
    public String marshal(PublisherType_v5_23 publisherType) {
        return publisherType.toString();
    }
}
