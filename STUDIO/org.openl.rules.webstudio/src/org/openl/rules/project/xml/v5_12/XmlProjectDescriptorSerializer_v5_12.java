package org.openl.rules.project.xml.v5_12;

import org.openl.rules.project.model.v5_12.ProjectDescriptor_v5_12;
import org.openl.rules.project.model.v5_12.converter.ProjectDescriptorVersionConverter;
import org.openl.rules.project.xml.BaseProjectDescriptorSerializer;

import javax.xml.bind.JAXBException;

public class XmlProjectDescriptorSerializer_v5_12 extends BaseProjectDescriptorSerializer<ProjectDescriptor_v5_12> {

    public XmlProjectDescriptorSerializer_v5_12() throws JAXBException {
        this(true);
    }

    public XmlProjectDescriptorSerializer_v5_12(boolean postProcess) throws JAXBException {
        super(postProcess, new ProjectDescriptorVersionConverter(), ProjectDescriptor_v5_12.class);
    }
}
