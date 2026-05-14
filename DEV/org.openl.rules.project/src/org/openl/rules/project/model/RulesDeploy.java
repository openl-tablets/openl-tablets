package org.openl.rules.project.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "rules-deploy")
@Getter
@Setter
public class RulesDeploy {

    public enum PublisherType {
        WEBSERVICE,
        RESTFUL,
        RMI,
        KAFKA
    }

    /** Custom accessors ({@link #isProvideRuntimeContext()} / {@link #setProvideRuntimeContext(Boolean)}) keep the historical method names. */
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Boolean isProvideRuntimeContext;
    private String serviceName;
    @XmlElementWrapper(name = "publishers")
    @XmlElement(name = "publisher")
    @XmlJavaTypeAdapter(PublisherTypeXmlAdapter.class)
    private PublisherType[] publishers;
    private String interceptingTemplateClassName;
    private String annotationTemplateClassName;
    private String serviceClass;
    private String url;
    private String version;
    private String groups;
    @XmlJavaTypeAdapter(MapAdapter.class)
    private Map<String, Object> configuration;

    public Boolean isProvideRuntimeContext() {
        return isProvideRuntimeContext;
    }

    public void setProvideRuntimeContext(Boolean isProvideRuntimeContext) {
        this.isProvideRuntimeContext = isProvideRuntimeContext;
    }

    public static final String FILE_NAME = "rules-deploy.xml";
    private static final JAXBSerializer SERIALIZER = new JAXBSerializer(RulesDeploy.class);

    public static RulesDeploy read(Path path) throws IOException, JAXBException {
        var file = Files.isDirectory(path) ? path.resolve(FILE_NAME) : path;
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try (var in = Files.newInputStream(file)) {
            return read(in);
        }
    }

    public static RulesDeploy read(InputStream in) throws JAXBException {
        return (RulesDeploy) SERIALIZER.unmarshal(in);
    }

    public InputStream toInputStream() throws JAXBException {
        var outputStream = new ByteArrayOutputStream();
        SERIALIZER.marshal(this, outputStream);
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    public void write(Path folder) throws IOException, JAXBException {
        try (var outputStream = Files.newOutputStream(folder.resolve(FILE_NAME))) {
            SERIALIZER.marshal(this, outputStream);
        }
    }

    public boolean hasChanges(Path folder) throws IOException, JAXBException {
        var original = Files.readAllBytes(folder.resolve(FILE_NAME));
        var outputStream = new ByteArrayOutputStream();
        SERIALIZER.marshal(this, outputStream);
        return !Arrays.equals(original, outputStream.toByteArray());
    }

    /**
     * Renders a {@link PublisherType} enum constant as the bare string the wire format uses.
     */
    public static class PublisherTypeXmlAdapter extends XmlAdapter<String, PublisherType> {
        @Override
        public PublisherType unmarshal(String name) {
            return PublisherType.valueOf(name.toUpperCase(Locale.ROOT));
        }

        @Override
        public String marshal(PublisherType publisherType) {
            return publisherType.toString();
        }
    }

    /**
     * Maps {@link Map}{@code <String, Object>} to/from the JAXB-friendly {@link MapType}/{@link MapStringEntryType}
     * shape. {@code null} and empty maps marshal to {@code null} so the {@code <configuration>} wrapper is
     * omitted from the output.
     */
    public static final class MapAdapter extends XmlAdapter<MapAdapter.MapType, Map<String, Object>> {

        @Override
        public MapType marshal(Map<String, Object> arg0) {
            if (arg0 == null || arg0.isEmpty()) {
                return null;
            }
            List<MapStringEntryType> mapStringEntryTypes = new ArrayList<>();
            MapType mapType = new MapType();
            for (Map.Entry<String, Object> entry : arg0.entrySet()) {
                if (!(entry.getValue() instanceof String)) {
                    throw new IllegalArgumentException("Expected string value in the Rules Deploy configuration");
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
            for (MapStringEntryType myEntryType : arg0.entry) {
                hashMap.put(myEntryType.getString()[0], myEntryType.getString()[1]);
            }
            return hashMap;
        }

        @XmlAccessorType(XmlAccessType.FIELD)
        static class MapType {

            @XmlElement(name = "entry")
            private List<MapStringEntryType> entry = new ArrayList<>();

            List<MapStringEntryType> getEntry() {
                return entry;
            }

            void setEntry(List<MapStringEntryType> entry) {
                this.entry = entry;
            }
        }

        @XmlAccessorType(XmlAccessType.FIELD)
        static class MapStringEntryType {

            private String[] string;

            String[] getString() {
                return string;
            }

            void setString(String[] string) {
                this.string = string;
            }
        }
    }

}
