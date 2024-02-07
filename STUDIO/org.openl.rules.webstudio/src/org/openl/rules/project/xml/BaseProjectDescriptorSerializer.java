package org.openl.rules.project.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;

import org.openl.rules.project.IProjectDescriptorSerializer;
import org.openl.rules.project.model.ObjectVersionConverter;
import org.openl.rules.project.model.ProjectDescriptor;

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

    private final JAXBSerializer jaxbSerializer;

    private final ObjectVersionConverter<ProjectDescriptor, T> projectDescriptorVersionConverter;

    public BaseProjectDescriptorSerializer(ObjectVersionConverter<ProjectDescriptor, T> projectDescriptorVersionConverter, Class<T> clazz) {
        jaxbSerializer = new JAXBSerializer(clazz);
        this.projectDescriptorVersionConverter = projectDescriptorVersionConverter;
    }

    @Override
    public String serialize(ProjectDescriptor source) throws IOException, JAXBException {
        try (StringWriter stringWriter = new StringWriter()) {
            jaxbSerializer.marshal(projectDescriptorVersionConverter.toOldVersion(source), stringWriter);
            return stringWriter.toString();
        }
    }

    @Override
    public ProjectDescriptor deserialize(InputStream source) throws JAXBException {
        @SuppressWarnings("unchecked")
        T oldVersion = (T) jaxbSerializer.unmarshal(source);
        ProjectDescriptor descriptor = projectDescriptorVersionConverter.fromOldVersion(oldVersion);
        if (descriptor.getClasspath() == null) {
            descriptor.setClasspath(new ArrayList<>());
        }

        if (descriptor.getModules() == null) {
            descriptor.setModules(new ArrayList<>());
        }
        return descriptor;
    }

    public static class CollapsedStringAdapter2 extends CollapsedStringAdapter {
        @Override
        public String marshal(String s) {
            return super.unmarshal(s);
        }
    }
}
