package org.openl.rules.project.xml;

import java.io.InputStream;
import java.util.ArrayList;

import org.openl.rules.project.IProjectDescriptorSerializer;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDescriptor;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class XmlProjectDescriptorSerializer implements IProjectDescriptorSerializer {

    private static final String PROJECT_DESCRIPTOR_TAG = "project";
    private static final String MODULE_TAG = "module";
    private static final String PATH_TAG = "entry";
    
    private XStream xstream;

    public XmlProjectDescriptorSerializer() {
        xstream = new XStream(new DomDriver());

        xstream.setMode(XStream.NO_REFERENCES);

        xstream.aliasType(PROJECT_DESCRIPTOR_TAG, ProjectDescriptor.class);
        xstream.aliasType(MODULE_TAG, Module.class);
        xstream.aliasType(PATH_TAG, PathEntry.class);
        
        xstream.useAttributeFor(PathEntry.class, "path");
        xstream.aliasField("rules-root", Module.class, "rulesRootPath");
        
        xstream.registerConverter(new ModuleTypeConverter());
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
