package org.openl.rules.project.abstraction;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.openl.rules.common.ProjectDescriptor;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.common.impl.ProjectDescriptorImpl;
import org.openl.util.IOUtils;

import javax.xml.bind.*;
import javax.xml.bind.annotation.*;

/**
 * Serializes and deserializes {@link ProjectDescriptor} to/from xml representation
 */
public class ProjectDescriptorSerializer {

    private final Marshaller jaxbMarshaller;
    private final Unmarshaller jaxbUnmarshaller;

    public ProjectDescriptorSerializer() {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Wrapper.class);
            jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            jaxbMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
            jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        } catch (JAXBException e) {
            throw new RuntimeException("Something went wrong when trying to create JAXB serializer", e);
        }
    }

    @SuppressWarnings({ "rawtypes" })
    public InputStream serialize(List<ProjectDescriptor> descriptors) {
        try (StringWriter stringWriter = new StringWriter()) {
            jaxbMarshaller.marshal(new Wrapper(descriptors), stringWriter);
            return IOUtils.toInputStream(stringWriter.toString());
        } catch (IOException | JAXBException e) {
            throw new RuntimeException("Error during Project Descriptor serialization", e);
        }
    }

    @SuppressWarnings({ "rawtypes" })
    public List<ProjectDescriptor> deserialize(InputStream source) {
        try {
            if (source.available() == 0) {
                return null;
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        try {
            return Optional.ofNullable(((Wrapper) jaxbUnmarshaller.unmarshal(source)).getDescriptors()).orElseGet(ArrayList::new);
        } catch (JAXBException e) {
            throw new RuntimeException("Error when trying to deserialize Project Descriptor", e);
        }

    }

    /**
     * For internal purpose, to wrap list of elements to the main tag
     */
    @SuppressWarnings({ "rawtypes" })
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name="descriptors")
    private static final class Wrapper {
        @XmlElement(name = "descriptor")
        public List<ProjectDescriptorXmlDto> descriptors = new ArrayList<>();

        public Wrapper(List<ProjectDescriptor> descriptors) {
            descriptors = descriptors == null ? new ArrayList<>() : descriptors;
            this.descriptors = descriptors.stream()
                    .map(ProjectDescriptorXmlDto::new)
                    .collect(Collectors.toList());
        }

        @SuppressWarnings("unused")
        public Wrapper() {
            // Used by XML deserializer
        }

        public List<ProjectDescriptor> getDescriptors() {
            return descriptors.stream()
                    .map(descriptor -> new ProjectDescriptorImpl(descriptor.repositoryId,
                            descriptor.projectName, descriptor.path, descriptor.branch,
                            new CommonVersionImpl(descriptor.projectVersion)))
                    .collect(Collectors.toList());
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name="descriptor")
    private static class ProjectDescriptorXmlDto {
        private String repositoryId;
        private String path;
        private String projectName;
        private String branch;
        private String projectVersion;

        public ProjectDescriptorXmlDto(ProjectDescriptor projectDescriptor) {
            this.repositoryId = projectDescriptor.getRepositoryId();
            this.path = projectDescriptor.getPath();
            this.projectName = projectDescriptor.getProjectName();
            this.branch = projectDescriptor.getBranch();
            this.projectVersion = projectDescriptor.getProjectVersion().getVersionName();
        }

        // for JAXB serialization
        @SuppressWarnings("unused")
        private ProjectDescriptorXmlDto() {}
    }

}
