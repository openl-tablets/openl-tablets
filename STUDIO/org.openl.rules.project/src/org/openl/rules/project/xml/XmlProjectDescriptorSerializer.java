package org.openl.rules.project.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;

import org.openl.rules.project.IProjectDescriptorSerializer;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.WebstudioConfiguration;
import org.openl.util.CollectionUtils;

/**
 * Project Descriptor serializer/deserializer.
 * <p>
 * Keep in mind that this serializer is for last version of OpenL. Needed version can be obtained: 1) from each project
 * settings (stored inside ".settings" folder) 2) from openl.compatibility.version property of WebStudio configuration.
 * Thus if project descriptor serializing is needed consider using ProjectDescriptorSerializerFactory instead.
 */
public class XmlProjectDescriptorSerializer implements IProjectDescriptorSerializer {

    public static final String PROJECT_DESCRIPTOR_TAG = "project";
    public static final String WEBSTUDIO_CONFIGURATION = "webstudioConfiguration";
    public static final String METHOD_FILTER_TAG = "method-filter";
    public static final String DEPENDENCY_TAG = "dependency";
    public static final String PROPERTIES_FILE_NAME_PATTERN = "properties-file-name-pattern";
    public static final String PROPERTIES_FILE_NAME_PROCESSOR = "properties-file-name-processor";
    public static final String STRING_VALUE = "value";

    private final JAXBSerializer jaxbSerializer;

    /**
     * Create Project Descriptor Serializer Note: please consider using ProjectDescriptorSerializerFactory instead
     *
     */
    public XmlProjectDescriptorSerializer() {
        jaxbSerializer = new JAXBSerializer(ProjectDescriptor.class);
    }

    /**
     * Collapses String both for marshall and unmarshall.
     */
    public static class CollapsedStringAdapter2 extends CollapsedStringAdapter {
        @Override
        public String marshal(String s) {
            return super.unmarshal(s);
        }
    }

    @Override
    public String serialize(ProjectDescriptor source) throws IOException, JAXBException {
        populateWithNulls(source);
        try (StringWriter stringWriter = new StringWriter()) {
            jaxbSerializer.marshal(source, stringWriter);
            return stringWriter.toString();
        } finally {
            populateNullsWithDefaultValues(source);
        }
    }

    @Override
    public ProjectDescriptor deserialize(InputStream source) throws JAXBException {
        ProjectDescriptor descriptor = (ProjectDescriptor) jaxbSerializer.unmarshal(source);
        populateNullsWithDefaultValues(descriptor);
        return descriptor;
    }

    private void populateNullsWithDefaultValues(ProjectDescriptor descriptor) {
        if (descriptor.getClasspath() == null) {
            descriptor.setClasspath(new ArrayList<>());
        }

        if (descriptor.getModules() == null) {
            descriptor.setModules(new ArrayList<>());
        }

        for (Module module : descriptor.getModules()) {
            if (module.getWebstudioConfiguration() == null) {
                module.setWebstudioConfiguration(new WebstudioConfiguration());
            }
        }
    }

    private void populateWithNulls(ProjectDescriptor descriptor) {
        if (CollectionUtils.isEmpty(descriptor.getClasspath())) {
            descriptor.setClasspath(null);
        }

        if (CollectionUtils.isEmpty(descriptor.getModules())) {
            descriptor.setModules(null);
        }

        for (Module module : descriptor.getModules()) {
            if (module.getWebstudioConfiguration() != null && Boolean.FALSE
                .equals(module.getWebstudioConfiguration().isCompileThisModuleOnly())) {
                module.setWebstudioConfiguration(null);
            }
        }
    }

}
