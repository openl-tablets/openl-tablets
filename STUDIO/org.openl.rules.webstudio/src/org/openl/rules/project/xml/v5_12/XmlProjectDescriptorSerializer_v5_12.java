package org.openl.rules.project.xml.v5_12;

import java.util.Collection;
import java.util.HashSet;

import com.thoughtworks.xstream.XStream;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.Property;
import org.openl.rules.project.model.v5_11.Module_v5_11;
import org.openl.rules.project.model.v5_12.ProjectDependencyDescriptor_v5_12;
import org.openl.rules.project.model.v5_12.ProjectDescriptor_v5_12;
import org.openl.rules.project.model.v5_12.converter.ProjectDescriptorVersionConverter;
import org.openl.rules.project.xml.BaseProjectDescriptorSerializer;
import org.openl.rules.project.xml.StringValueConverter;
import org.openl.rules.project.xml.v5_11.ModuleTypeConverter_v5_11;

public class XmlProjectDescriptorSerializer_v5_12 extends BaseProjectDescriptorSerializer<ProjectDescriptor_v5_12> {

    private static final String PROJECT_DESCRIPTOR_TAG = "project";
    private static final String MODULE_TAG = "module";
    private static final String PATH_TAG = "entry";
    private static final String PROPERTY_TAG = "property";
    private static final String METHOD_FILTER_TAG = "method-filter";
    private static final String DEPENDENCY_TAG = "dependency";
    private static final String PROPERTIES_FILE_NAME_PATTERN = "properties-file-name-pattern";
    private static final String PROPERTIES_FILE_NAME_PROCESSOR = "properties-file-name-processor";

    public XmlProjectDescriptorSerializer_v5_12() {
        this(true);
    }

    public XmlProjectDescriptorSerializer_v5_12(boolean postProcess) {
        super(postProcess, new ProjectDescriptorVersionConverter());

        xstream.omitField(ProjectDescriptor_v5_12.class, "id"); // This field was deprecated
        xstream.omitField(ProjectDescriptor_v5_12.class, "log");
        xstream.omitField(ProjectDescriptor_v5_12.class, "classLoader");
        xstream.omitField(ProjectDescriptor_v5_12.class, "projectFolder");
        xstream.omitField(Module_v5_11.class, "properties"); // properties doesn't supported by rules.xml
        xstream.omitField(Module_v5_11.class, "wildcardName"); // runtime properties
        xstream.omitField(Module_v5_11.class, "wildcardRulesRootPath"); // runtime properties
        xstream.omitField(Module_v5_11.class, "project"); // runtime properties

        xstream.setMode(XStream.NO_REFERENCES);

        xstream.aliasType(PROJECT_DESCRIPTOR_TAG, ProjectDescriptor_v5_12.class);
        xstream.aliasType(MODULE_TAG, Module_v5_11.class);
        xstream.aliasType(DEPENDENCY_TAG, ProjectDependencyDescriptor_v5_12.class);
        xstream.aliasType(PATH_TAG, PathEntry.class);
        xstream.aliasType(PROPERTY_TAG, Property.class);
        xstream.aliasField(PROPERTIES_FILE_NAME_PATTERN, ProjectDescriptor_v5_12.class, "propertiesFileNamePattern");
        xstream
            .aliasField(PROPERTIES_FILE_NAME_PROCESSOR, ProjectDescriptor_v5_12.class, "propertiesFileNameProcessor");
        xstream.addDefaultImplementation(HashSet.class, Collection.class);
        xstream.alias("value", String.class);

        xstream.useAttributeFor(PathEntry.class, "path");
        xstream.aliasField("rules-root", Module_v5_11.class, "rulesRootPath");
        xstream.aliasField(METHOD_FILTER_TAG, Module_v5_11.class, "methodFilter");
        xstream.registerConverter(new ModuleTypeConverter_v5_11());
        xstream.registerConverter(new StringValueConverter());
    }

}
