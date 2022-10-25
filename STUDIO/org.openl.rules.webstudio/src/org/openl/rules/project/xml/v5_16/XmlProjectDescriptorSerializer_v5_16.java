package org.openl.rules.project.xml.v5_16;

import org.openl.rules.project.model.v5_16.ProjectDescriptor_v5_16;
import org.openl.rules.project.model.v5_16.converter.ProjectDescriptorVersionConverter;
import org.openl.rules.project.xml.BaseProjectDescriptorSerializer;

import javax.xml.bind.JAXBException;

public class XmlProjectDescriptorSerializer_v5_16 extends BaseProjectDescriptorSerializer<ProjectDescriptor_v5_16> {

    public XmlProjectDescriptorSerializer_v5_16() throws JAXBException {
        this(true);
    }

    public XmlProjectDescriptorSerializer_v5_16(boolean postProcess) throws JAXBException {
        super(postProcess, new ProjectDescriptorVersionConverter(), ProjectDescriptor_v5_16.class);
    }
}
