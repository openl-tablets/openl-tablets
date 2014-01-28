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

public class XmlProjectDescriptorSerializer implements IProjectDescriptorSerializer {

    private static final String PROJECT_DESCRIPTOR_TAG = "project";
    private static final String MODULE_TAG = "module";
    private static final String PATH_TAG = "entry";
    private static final String PROPERTY_TAG = "property";
    private static final String METHOD_FILTER_TAG = "method-filter";
    private static final String DEPENDENCY_TAG = "dependency";
    private static final String DEFAULT_PROPERTIES_FILE_NAME_PATTERN = "default-properties-file-name-pattern";
    private static final String DEFAULT_PROPERTIES_FILE_NAME_PROCESSOR = "default-properties-file-name-processor";

    private XStream xstream;

    public XmlProjectDescriptorSerializer() {
        xstream = new XStream(new DomDriver());
        xstream.omitField(ProjectDescriptor.class, "log");
        xstream.omitField(Module.class, "project");

        xstream.setMode(XStream.NO_REFERENCES);

        xstream.aliasType(PROJECT_DESCRIPTOR_TAG, ProjectDescriptor.class);
        xstream.aliasType(MODULE_TAG, Module.class);
        xstream.aliasType(DEPENDENCY_TAG, ProjectDependencyDescriptor.class);
        xstream.aliasType(PATH_TAG, PathEntry.class);
        xstream.aliasType(PROPERTY_TAG, Property.class);
        xstream.aliasField(DEFAULT_PROPERTIES_FILE_NAME_PATTERN, ProjectDescriptor.class, "defaultPropertiesFileNamePattern");
        xstream.aliasField(DEFAULT_PROPERTIES_FILE_NAME_PROCESSOR, ProjectDescriptor.class, "defaultPropertiesFileNameProcessor");
        xstream.addDefaultImplementation(HashSet.class, Collection.class);
        xstream.alias("value", String.class);

        xstream.useAttributeFor(PathEntry.class, "path");
        xstream.aliasField("rules-root", Module.class, "rulesRootPath");
        xstream.aliasField(METHOD_FILTER_TAG, Module.class, "methodFilter");
        xstream.registerConverter(new ModuleTypeConverter());
        xstream.registerConverter(new StringValueConverter());
    }

    public String serialize(ProjectDescriptor source) {
        return xstream.toXML(source);
    }

    public ProjectDescriptor deserialize(InputStream source) {
        ProjectDescriptor descriptor = (ProjectDescriptor) xstream.fromXML(source);
        postProcess(descriptor);
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

}
