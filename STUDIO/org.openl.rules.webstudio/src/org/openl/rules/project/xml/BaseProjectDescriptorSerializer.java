package org.openl.rules.project.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;

import org.openl.rules.project.IProjectDescriptorSerializer;
import org.openl.rules.project.model.ObjectVersionConverter;
import org.openl.rules.project.model.ProjectDescriptor;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;

public class BaseProjectDescriptorSerializer<T> implements IProjectDescriptorSerializer {
    public static final String PROJECT_DESCRIPTOR_TAG = "project";
    public static final String MODULE_TAG = "module";
    public static final String MODULES_TAG = "modules";
    public static final String PATH_TAG = "entry";
    public static final String RULES_ROOT_TAG = "rules-root";
    public static final String CLASSPATH_TAG = "classpath";
    public static final String METHOD_FILTER_TAG = "method-filter";
    public static final String DEPENDENCY_TAG = "dependency";
    public static final String DEPENDENCIES_TAG = "dependencies";
    public static final String PROPERTIES_FILE_NAME_PATTERN = "properties-file-name-pattern";
    public static final String PROPERTIES_FILE_NAME_PROCESSOR = "properties-file-name-processor";

    private final Marshaller jaxbMarshaller;
    private final Unmarshaller jaxbUnmarshaller;

    private final boolean postProcess;
    private final ObjectVersionConverter<ProjectDescriptor, T> projectDescriptorVersionConverter;

    public BaseProjectDescriptorSerializer(boolean postProcess,
            ObjectVersionConverter<ProjectDescriptor, T> projectDescriptorVersionConverter, Class<T> clazz) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
            jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            jaxbMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, true); // disables header

            jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }

        this.postProcess = postProcess;
        this.projectDescriptorVersionConverter = projectDescriptorVersionConverter;
    }

    @Override
    public String serialize(ProjectDescriptor source) {
        try (StringWriter stringWriter = new StringWriter()) {
            jaxbMarshaller.marshal(projectDescriptorVersionConverter.toOldVersion(source), stringWriter);
            return stringWriter.toString();
        } catch (IOException | JAXBException e) {
            throw new OpenLSerializationException("Error during Project Descriptor serialization", e);
        }
    }

    @Override
    public ProjectDescriptor deserialize(InputStream source) {
        try {
            @SuppressWarnings("unchecked")
            T oldVersion = (T) jaxbUnmarshaller.unmarshal(source);
            ProjectDescriptor descriptor = projectDescriptorVersionConverter.fromOldVersion(oldVersion);
            if (postProcess) {
                postProcess(descriptor);
            }
            return descriptor;
        } catch (JAXBException e) {
            throw new OpenLSerializationException("Error during Project Descriptor deserialization", e);
        }
    }

    private static void postProcess(ProjectDescriptor descriptor) {
        if (descriptor.getClasspath() == null) {
            descriptor.setClasspath(new ArrayList<>());
        }

        if (descriptor.getModules() == null) {
            descriptor.setModules(new ArrayList<>());
        }
    }

    public static class CollapsedStringAdapter2 extends CollapsedStringAdapter {
        @Override
        public String marshal(String s) {
            return super.unmarshal(s);
        }
    }
}
