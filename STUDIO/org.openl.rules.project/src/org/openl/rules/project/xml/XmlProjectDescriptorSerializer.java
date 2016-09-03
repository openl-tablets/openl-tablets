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

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.openl.util.CollectionUtils;

public class XmlProjectDescriptorSerializer implements IProjectDescriptorSerializer {

    private static final String PROJECT_DESCRIPTOR_TAG = "project";
    private static final String MODULE_TAG = "module";
    private static final String PATH_TAG = "entry";
    private static final String PROPERTY_TAG = "property";
    private static final String METHOD_FILTER_TAG = "method-filter";
    private static final String DEPENDENCY_TAG = "dependency";
    private static final String PROPERTIES_FILE_NAME_PATTERN = "properties-file-name-pattern";
    private static final String PROPERTIES_FILE_NAME_PROCESSOR = "properties-file-name-processor";

    private final XStream xstream;

    private final boolean postProcess;

    public XmlProjectDescriptorSerializer() {
        this(true);
    }

    public XmlProjectDescriptorSerializer(boolean postProcess) {
        xstream = new XStream(new DomDriver());
        xstream.ignoreUnknownElements();
        xstream.omitField(ProjectDescriptor.class, "id"); // This field was deprecated
        xstream.omitField(ProjectDescriptor.class, "log");
        xstream.omitField(ProjectDescriptor.class, "classLoader");
        xstream.omitField(ProjectDescriptor.class, "projectFolder");
        xstream.omitField(Module.class, "properties"); //properties doesn't supported by rules.xml
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
        xstream.addDefaultImplementation(HashSet.class, Collection.class);
        xstream.alias("value", String.class);

        xstream.useAttributeFor(PathEntry.class, "path");
        xstream.aliasField("rules-root", Module.class, "rulesRootPath");
        xstream.aliasField(METHOD_FILTER_TAG, Module.class, "methodFilter");
        xstream.registerConverter(new StringValueConverter());

        this.postProcess = postProcess;
    }

    public String serialize(ProjectDescriptor source) {
        clean(source);
        return xstream.toXML(source);
    }

    /**
     * @throws com.thoughtworks.xstream.XStreamException if the object cannot be deserialized
     */
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
