package org.openl.rules.project.xml.v5_11;

import org.openl.rules.project.model.v5_11.ModuleType_v5_11;
import org.openl.rules.project.model.v5_11.ProjectDescriptor_v5_11;
import org.openl.rules.project.model.v5_11.converter.ProjectDescriptorVersionConverter;
import org.openl.rules.project.xml.BaseProjectDescriptorSerializer;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.adapters.XmlAdapter;

public class XmlProjectDescriptorSerializer_v5_11 extends BaseProjectDescriptorSerializer<ProjectDescriptor_v5_11> {

    public XmlProjectDescriptorSerializer_v5_11() throws JAXBException {
        this(true);
    }

    public XmlProjectDescriptorSerializer_v5_11(boolean postProcess) throws JAXBException {
        super(postProcess, new ProjectDescriptorVersionConverter(), ProjectDescriptor_v5_11.class);
    }

    public static class ModuleType_v5_11XmlAdapter extends XmlAdapter<String, ModuleType_v5_11> {
        @Override
        public ModuleType_v5_11 unmarshal(String name) {
            return ModuleType_v5_11.valueOf(name.toUpperCase());
        }

        @Override
        public String marshal(ModuleType_v5_11 moduleType_v5_11) {
            return moduleType_v5_11.toString().toLowerCase();
        }
    }
}
