package org.openl.rules.project.xml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.openl.rules.project.IProjectDescriptorSerializer;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.OpenAPI;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.util.CollectionUtils;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.security.NoTypePermission;

/**
 * Project Descriptor serializer/deserializer.
 * <p>
 * Keep in mind that this serializer is for last version of OpenL. Needed version can be obtained: 1) from each project
 * settings (stored inside ".settings" folder) 2) from openl.compatibility.version property of WebStudio configuration.
 * Thus if project descriptor serializing is needed consider using ProjectDescriptorSerializerFactory instead.
 */
public class XmlProjectDescriptorSerializer implements IProjectDescriptorSerializer {

    private static final String PROJECT_DESCRIPTOR_TAG = "project";
    private static final String MODULE_TAG = "module";
    private static final String PATH_TAG = "entry";
    private static final String METHOD_FILTER_TAG = "method-filter";
    private static final String DEPENDENCY_TAG = "dependency";
    private static final String PROPERTIES_FILE_NAME_PATTERN = "properties-file-name-pattern";
    private static final String PROPERTIES_FILE_NAME_PROCESSOR = "properties-file-name-processor";

    private final XStream xstream;

    /**
     * Create Project Descriptor Serializer Note: please consider using ProjectDescriptorSerializerFactory instead
     *
     */
    public XmlProjectDescriptorSerializer() {
        xstream = new XStream(new DomDriver());
        xstream.addPermission(NoTypePermission.NONE);
        xstream.allowTypeHierarchy(Module.class);
        xstream.allowTypeHierarchy(ProjectDescriptor.class);
        xstream.allowTypeHierarchy(ProjectDependencyDescriptor.class);
        xstream.allowTypeHierarchy(PathEntry.class);
        xstream.allowTypeHierarchy(String.class);

        xstream.ignoreUnknownElements();
        xstream.omitField(ProjectDescriptor.class, "id"); // This field was deprecated
        xstream.omitField(ProjectDescriptor.class, "log");
        xstream.omitField(ProjectDescriptor.class, "classLoader");
        xstream.omitField(ProjectDescriptor.class, "projectFolder");
        xstream.omitField(Module.class, "properties"); // properties does not supported by rules.xml
        xstream.omitField(Module.class, "wildcardName"); // runtime properties
        xstream.omitField(Module.class, "wildcardRulesRootPath"); // runtime properties
        xstream.omitField(Module.class, "project"); // runtime properties

        xstream.setMode(XStream.NO_REFERENCES);

        xstream.aliasType(PROJECT_DESCRIPTOR_TAG, ProjectDescriptor.class);
        xstream.aliasType(MODULE_TAG, Module.class);
        xstream.aliasType(DEPENDENCY_TAG, ProjectDependencyDescriptor.class);
        xstream.aliasType(PATH_TAG, PathEntry.class);
        xstream.addImplicitArray(ProjectDescriptor.class, "propertiesFileNamePatterns", PROPERTIES_FILE_NAME_PATTERN);
        xstream.aliasField(PROPERTIES_FILE_NAME_PROCESSOR, ProjectDescriptor.class, "propertiesFileNameProcessor");
        xstream.addDefaultImplementation(HashSet.class, Collection.class);
        xstream.alias("value", String.class);

        xstream.useAttributeFor(PathEntry.class, "path");
        xstream.aliasField("rules-root", Module.class, "rulesRootPath");
        xstream.aliasField(METHOD_FILTER_TAG, Module.class, "methodFilter");
        xstream.registerConverter(new StringValueConverter());

        xstream.aliasField("openapi", ProjectDescriptor.class, "openapi");
        xstream.aliasField("model-module-name", OpenAPI.class, "modelModuleName");
        xstream.aliasField("algorithm-module-name", OpenAPI.class, "algorithmModuleName");
    }

    @Override
    public String serialize(ProjectDescriptor source) {
        populateEmptyCollectionsWithNulls(source);
        try {
            return xstream.toXML(source);
        } finally {
            populateNullsWithEmptyCollections(source);
        }
    }

    /**
     * @throws com.thoughtworks.xstream.XStreamException if the object cannot be deserialized
     */
    @Override
    public ProjectDescriptor deserialize(InputStream source) {
        ProjectDescriptor descriptor = (ProjectDescriptor) xstream.fromXML(source);
        populateNullsWithEmptyCollections(descriptor);
        return descriptor;
    }

    private void populateNullsWithEmptyCollections(ProjectDescriptor descriptor) {
        if (descriptor.getClasspath() == null) {
            descriptor.setClasspath(new ArrayList<>());
        }

        if (descriptor.getModules() == null) {
            descriptor.setModules(new ArrayList<>());
        }
    }

    private void populateEmptyCollectionsWithNulls(ProjectDescriptor descriptor) {
        if (CollectionUtils.isEmpty(descriptor.getClasspath())) {
            descriptor.setClasspath(null);
        }

        if (CollectionUtils.isEmpty(descriptor.getModules())) {
            descriptor.setModules(null);
        }
    }

}
