package org.openl.rules.project.xml.v5_11;

import java.util.Collection;
import java.util.HashSet;

import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.Property;
import org.openl.rules.project.model.v5_11.Module_v5_11;
import org.openl.rules.project.model.v5_11.ProjectDescriptor_v5_11;
import org.openl.rules.project.model.v5_11.converter.ProjectDescriptorVersionConverter;
import org.openl.rules.project.xml.BaseProjectDescriptorSerializer;
import org.openl.rules.project.xml.StringValueConverter;

import com.thoughtworks.xstream.XStream;

public class XmlProjectDescriptorSerializer_v5_11 extends BaseProjectDescriptorSerializer<ProjectDescriptor_v5_11> {

    private static final String PROJECT_DESCRIPTOR_TAG = "project";
    private static final String MODULE_TAG = "module";
    private static final String PATH_TAG = "entry";
    private static final String PROPERTY_TAG = "property";
    private static final String METHOD_FILTER_TAG = "method-filter";

    public XmlProjectDescriptorSerializer_v5_11() {
        this(true);
    }

    public XmlProjectDescriptorSerializer_v5_11(boolean postProcess) {
        super(postProcess, new ProjectDescriptorVersionConverter());

        xstream.omitField(ProjectDescriptor_v5_11.class, "log");
        xstream.omitField(ProjectDescriptor_v5_11.class, "classLoader");
        xstream.omitField(Module_v5_11.class, "project"); // runtime properties

        xstream.setMode(XStream.NO_REFERENCES);

        xstream.aliasType(PROJECT_DESCRIPTOR_TAG, ProjectDescriptor_v5_11.class);
        xstream.aliasType(MODULE_TAG, Module_v5_11.class);
        xstream.aliasType(PATH_TAG, PathEntry.class);
        xstream.aliasType(PROPERTY_TAG, Property.class);
        xstream.addDefaultImplementation(HashSet.class, Collection.class);
        xstream.alias("value", String.class);

        xstream.useAttributeFor(PathEntry.class, "path");
        xstream.aliasField("rules-root", Module_v5_11.class, "rulesRootPath");
        xstream.aliasField(METHOD_FILTER_TAG, Module_v5_11.class, "methodFilter");
        xstream.registerConverter(new ModuleTypeConverter_v5_11());
        xstream.registerConverter(new StringValueConverter());
    }

}
