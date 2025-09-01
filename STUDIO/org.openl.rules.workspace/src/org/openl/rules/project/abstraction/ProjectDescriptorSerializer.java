package org.openl.rules.project.abstraction;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import org.openl.rules.common.ProjectDescriptor;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.common.impl.ProjectDescriptorImpl;
import org.openl.rules.project.xml.JAXBSerializer;

/**
 * Serializes and deserializes {@link ProjectDescriptor} to/from xml representation
 */
public class ProjectDescriptorSerializer {

    private final JAXBSerializer jaxbSerializer;

    public ProjectDescriptorSerializer() {
        jaxbSerializer = new JAXBSerializer(Wrapper.class);
    }

    public InputStream serialize(List<ProjectDescriptor> descriptors) throws IOException, JAXBException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            jaxbSerializer.marshal(new Wrapper(descriptors), outputStream);
            return new ByteArrayInputStream(outputStream.toByteArray());
        }
    }

    public List<ProjectDescriptor> deserialize(InputStream source) throws JAXBException {
        try {
            if (source.available() == 0) {
                return null;
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        return Optional.ofNullable(((Wrapper) jaxbSerializer.unmarshal(source)).getDescriptors())
                .orElseGet(ArrayList::new);
    }

    /**
     * For internal purpose, to wrap list of elements to the main tag
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "descriptors")
    private static final class Wrapper {
        @XmlElement(name = "descriptor")
        public List<ProjectDescriptorXmlDto> descriptors = new ArrayList<>();

        public Wrapper(List<ProjectDescriptor> descriptors) {
            descriptors = descriptors == null ? new ArrayList<>() : descriptors;
            this.descriptors = descriptors.stream().map(ProjectDescriptorXmlDto::new).collect(Collectors.toList());
        }

        @SuppressWarnings("unused")
        public Wrapper() {
            // Used by XML deserializer
        }

        public List<ProjectDescriptor> getDescriptors() {
            return descriptors.stream()
                    .map(descriptor -> new ProjectDescriptorImpl(descriptor.repositoryId,
                            descriptor.projectName,
                            descriptor.path,
                            descriptor.branch,
                            new CommonVersionImpl(descriptor.projectVersion)))
                    .collect(Collectors.toList());
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "descriptor")
    private static class ProjectDescriptorXmlDto {
        private String repositoryId;
        private String path;
        private String projectName;
        private String branch;
        private String projectVersion;

        public ProjectDescriptorXmlDto(ProjectDescriptor projectDescriptor) {
            this.repositoryId = projectDescriptor.repositoryId();
            this.path = projectDescriptor.path();
            this.projectName = projectDescriptor.projectName();
            this.branch = projectDescriptor.branch();
            this.projectVersion = projectDescriptor.projectVersion().getVersionName();
        }

        // for JAXB serialization
        @SuppressWarnings("unused")
        private ProjectDescriptorXmlDto() {
        }
    }

}
