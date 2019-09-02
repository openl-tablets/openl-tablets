package org.openl.rules.project.xml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.openl.rules.project.IProjectDescriptorSerializer;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.Property;
import org.openl.util.CollectionUtils;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.security.NoTypePermission;

/**
 * Project Descriptor serializer/deserializer.
 * <p>
 * Keep in mind that this serializer is for last version of OpenL. Needed version can be obtained: 1) from each project
 * settings (stored inside ".settings" folder) 2) from default.openl.compatibility.version property of WebStudio
 * configuration. Thus if project descriptor serializing is needed consider using ProjectDescriptorSerializerFactory
 * instead.
 */
public class XmlProjectDescriptorSerializer implements IProjectDescriptorSerializer {

    private static final String PROJECT_DESCRIPTOR_TAG = "project";
    private static final String MODULE_TAG = "module";
    private static final String PATH_TAG = "entry";
    private static final String PROPERTY_TAG = "property";
    private static final String METHOD_FILTER_TAG = "method-filter";
    private static final String DEPENDENCY_TAG = "dependency";
    private static final String PROPERTIES_FILE_NAME_PATTERN = "properties-file-name-pattern";
    private static final String CSR_PACKAGE = "csr-package";
    private static final String PROPERTIES_FILE_NAME_PROCESSOR = "properties-file-name-processor";

    private final XStream xstream;

    private final boolean postProcess;

    /**
     * Create Project Descriptor Serializer Note: please consider using ProjectDescriptorSerializerFactory instead
     */
    public XmlProjectDescriptorSerializer() {
        this(true);
    }

    /**
     * Create Project Descriptor Serializer Note: please consider using ProjectDescriptorSerializerFactory instead
     *
     * @param postProcess is post processing of project descriptor is needed
     */
    public XmlProjectDescriptorSerializer(boolean postProcess) {
        xstream = new XStream(new DomDriver());
        xstream.addPermission(NoTypePermission.NONE);
        xstream.allowTypeHierarchy(Module.class);
        xstream.allowTypeHierarchy(ProjectDescriptor.class);
        xstream.allowTypeHierarchy(ProjectDependencyDescriptor.class);
        xstream.allowTypeHierarchy(PathEntry.class);
        xstream.allowTypeHierarchy(Property.class);
        xstream.allowTypeHierarchy(String.class);

        xstream.ignoreUnknownElements();
        xstream.omitField(ProjectDescriptor.class, "id"); // This field was deprecated
        xstream.omitField(ProjectDescriptor.class, "log");
        xstream.omitField(ProjectDescriptor.class, "classLoader");
        xstream.omitField(ProjectDescriptor.class, "projectFolder");
        xstream.omitField(Module.class, "properties"); // properties doesn't supported by rules.xml
        xstream.omitField(Module.class, "wildcardName"); // runtime properties
        xstream.omitField(Module.class, "wildcardRulesRootPath"); // runtime properties
        xstream.omitField(Module.class, "project"); // runtime properties

        xstream.setMode(XStream.NO_REFERENCES);

        xstream.aliasType(PROJECT_DESCRIPTOR_TAG, ProjectDescriptor.class);
        xstream.aliasType(MODULE_TAG, Module.class);
        xstream.aliasType(DEPENDENCY_TAG, ProjectDependencyDescriptor.class);
        xstream.aliasType(PATH_TAG, PathEntry.class);
        xstream.aliasType(PROPERTY_TAG, Property.class);
        xstream.aliasField(PROPERTIES_FILE_NAME_PATTERN, ProjectDescriptor.class, "propertiesFileNamePattern");
        xstream.aliasField(PROPERTIES_FILE_NAME_PROCESSOR, ProjectDescriptor.class, "propertiesFileNameProcessor");
        xstream.aliasField(CSR_PACKAGE, ProjectDescriptor.class, "csrPackage");
        xstream.addDefaultImplementation(HashSet.class, Collection.class);
        xstream.alias("value", String.class);

        xstream.useAttributeFor(PathEntry.class, "path");
        xstream.aliasField("rules-root", Module.class, "rulesRootPath");
        xstream.aliasField(METHOD_FILTER_TAG, Module.class, "methodFilter");
        xstream.registerConverter(new StringValueConverter());

        this.postProcess = postProcess;
    }

    @Override
    public String serialize(ProjectDescriptor source) {
        clean(source);
        return xstream.toXML(source);
    }

    /**
     * @throws com.thoughtworks.xstream.XStreamException if the object cannot be deserialized
     */
    @Override
    public ProjectDescriptor deserialize(InputStream source) {
        ProjectDescriptor descriptor = (ProjectDescriptor) xstream.fromXML(source);
        if (postProcess) {
            postProcess(descriptor);
        }
        return descriptor;
    }

    private void postProcess(ProjectDescriptor descriptor) {
        if (descriptor.getClasspath() == null) {
            descriptor.setClasspath(new ArrayList<PathEntry>());
        }

        if (descriptor.getModules() == null) {
            descriptor.setModules(new ArrayList<Module>());
        }
    }

    private void clean(ProjectDescriptor descriptor) {
        if (CollectionUtils.isEmpty(descriptor.getClasspath())) {
            descriptor.setClasspath(null);
        }

        if (CollectionUtils.isEmpty(descriptor.getModules())) {
            descriptor.setModules(null);
        }
    }

}
