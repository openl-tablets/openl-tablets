package org.openl.rules.project.xml.v5_14;

import org.openl.rules.project.model.v5_14.PublisherType_v5_14;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class PublisherTypeXmlAdapter_v5_14 extends XmlAdapter<String, PublisherType_v5_14> {
    @Override
    public PublisherType_v5_14 unmarshal(String name) {
        return PublisherType_v5_14.valueOf(name.toUpperCase());
    }

    @Override
    public String marshal(PublisherType_v5_14 publisherType) {
        return publisherType.toString();
    }
}
