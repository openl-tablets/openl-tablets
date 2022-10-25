package org.openl.rules.project.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.rules.project.IRulesDeploySerializer;
import org.openl.rules.project.model.RulesDeploy;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlAdapter;

public class XmlRulesDeploySerializer implements IRulesDeploySerializer {
    public static final String RULES_DEPLOY_DESCRIPTOR_TAG = "rules-deploy";
    public static final String MODULE_NAME = "module";
    public static final String LAZY_MODULES_FOR_COMPILATION = "lazy-modules-for-compilation";
    public static final String PUBLISHER_TAG = "publisher";
    public static final String PUBLISHERS_TAG = "publishers";
    public static final String NAME_TAG = "name";

    private final Marshaller jaxbMarshaller;
    private final Unmarshaller jaxbUnmarshaller;

    public XmlRulesDeploySerializer() {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(RulesDeploy.class);
            jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            jaxbMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, true); // excludes header
            jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        } catch (JAXBException e) {
            throw new OpenLSerializationException("Error during serializer creation", e);
        }

    }

    @Override
    public String serialize(RulesDeploy source) {
        try (StringWriter stringWriter = new StringWriter()) {
            jaxbMarshaller.marshal(source, stringWriter);
            return stringWriter.toString();
        } catch (IOException | JAXBException e) {
            throw new OpenLSerializationException("Error during Rules Deploy serialization", e);
        }
    }

    @Override
    public RulesDeploy deserialize(InputStream source) {
        try {
            return (RulesDeploy) jaxbUnmarshaller.unmarshal(source);
        } catch (JAXBException  e) {
            throw new OpenLSerializationException("Error during Rules Deploy deserialization", e);
        }
    }

    public RulesDeploy deserialize(String source) {
        try {
            return (RulesDeploy) jaxbUnmarshaller.unmarshal(new ByteArrayInputStream(source.getBytes()));
        } catch (JAXBException e) {
            throw new OpenLSerializationException("Error during Rules Deploy deserialization", e);
        }
    }

    public static class PublisherTypeXmlAdapter extends XmlAdapter<String, RulesDeploy.PublisherType> {
        @Override
        public RulesDeploy.PublisherType unmarshal(String name) {
            return RulesDeploy.PublisherType.valueOf(name.toUpperCase());
        }

        @Override
        public String marshal(RulesDeploy.PublisherType publisherType) {
            return publisherType.toString();
        }
    }

    public static final class MapAdapter extends XmlAdapter<MapAdapter.MapType,Map<String, Object>> {

        public MapAdapter() {}

        @Override
        public MapType marshal(Map<String, Object> arg0) {
            if (arg0 == null) return null;
            List<MapStringEntryType> mapStringEntryTypes = new ArrayList<>();
            MapType mapType = new MapType();
            for(Map.Entry<String, Object> entry : arg0.entrySet()) {
                if (!(entry.getValue() instanceof String)) {
                    throw new OpenLSerializationException("Expected string value in the Rules Deploy configuration");
                }
                MapStringEntryType mapStringEntryType = new MapStringEntryType();
                mapStringEntryType.setString(new String[]{entry.getKey(), (String) entry.getValue()});
                mapStringEntryTypes.add(mapStringEntryType);
            }
            mapType.setEntry(mapStringEntryTypes);
            return mapType;
        }

        @Override
        public Map<String, Object> unmarshal(MapType arg0) {
            HashMap<String, Object> hashMap = new HashMap<>();
            for(MapStringEntryType myEntryType : arg0.entry) {
                hashMap.put(myEntryType.getString()[0], myEntryType.getString()[1]);
            }
            return hashMap;
        }

        @XmlAccessorType(XmlAccessType.FIELD)
        private static class MapType {

            @XmlElement(name = "entry")
            public List<MapStringEntryType> entry = new ArrayList<>();

            public List<MapStringEntryType> getEntry() {
                return entry;
            }

            public void setEntry(List<MapStringEntryType> entry) {
                this.entry = entry;
            }
        }

        @XmlAccessorType(XmlAccessType.FIELD)
        private static class MapStringEntryType {

            public String[] string;

            public String[] getString() {
                return string;
            }

            public void setString(String[] string) {
                this.string = string;
            }
        }
    }
}
