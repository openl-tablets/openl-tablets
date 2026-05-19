package org.openl.rules.project.model;

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
import java.util.Objects;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
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
import lombok.extern.slf4j.Slf4j;

import org.openl.util.StringUtils;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "rules-deploy")
@Getter
@Setter
@Slf4j
public class RulesDeploy {

    public enum PublisherType {
        RESTFUL,
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

    public static RulesDeploy read(Path path) {
        var file = Files.isDirectory(path) ? path.resolve(FILE_NAME) : path;
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try (var in = Files.newInputStream(file)) {
            return read(in);
        } catch (IOException e) {
            log.warn("Failed to read '{}'.", file, e);
            return null;
        }
    }

    public static RulesDeploy read(InputStream in) {
        try {
            return (RulesDeploy) SERIALIZER.unmarshal(in);
        } catch (JAXBException e) {
            log.warn("Failed to parse '{}'.", FILE_NAME, e);
            return null;
        }
    }

    public byte[] toBytes() {
        var outputStream = new ByteArrayOutputStream();
        try {
            SERIALIZER.marshal(this, outputStream);
        } catch (JAXBException e) {
            throw new IllegalStateException(e);
        }
        return outputStream.toByteArray();
    }

    /**
     * JAXB callback: drops not supported elements from legacy {@code rules-deploy.xml} files so deprecated values
     * do not leak into the model. The XML still parses without error to preserve backward compatibility.
     */
    @SuppressWarnings("unused")
    private void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
        if (this.publishers != null) {
            // Clean up null elements
            publishers = Arrays.stream(this.publishers)
                    .filter(Objects::nonNull)
                    .toArray(PublisherType[]::new);
        }
    }

    /** Nullify blank Strings and empty containers so JAXB omits the corresponding elements entirely. */
    @SuppressWarnings("unused")
    private void beforeMarshal(Marshaller marshaller) {
        serviceName = StringUtils.trimToNull(serviceName);
        interceptingTemplateClassName = StringUtils.trimToNull(interceptingTemplateClassName);
        annotationTemplateClassName = StringUtils.trimToNull(annotationTemplateClassName);
        serviceClass = StringUtils.trimToNull(serviceClass);
        url = StringUtils.trimToNull(url);
        version = StringUtils.trimToNull(version);
        groups = StringUtils.trimToNull(groups);
        if (configuration != null && configuration.isEmpty()) {
            configuration = null;
        }
    }

    /**
     * Renders a {@link PublisherType} enum constant as the bare string the wire format uses.
     */
    public static class PublisherTypeXmlAdapter extends XmlAdapter<String, PublisherType> {
        @Override
        public PublisherType unmarshal(String name) {
            try {
                return PublisherType.valueOf(name.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return null;
            }
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
